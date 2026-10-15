package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * r86 羽剑冲锋（天空阶段 FF14 机制）。
 *
 * 流程：
 *  1. 警告期（WARNING_TICKS，默认 60）：贴地直线红色预警显示完整路线，
 *     羽剑悬浮在路线起点蓄势。
 *  2. 冲锋期（CHARGE_TICKS，默认 16）：羽剑沿路线快速飞驰，途中对扫到的玩家
 *     造成一次性物理伤害（每名玩家单次冲锋只打一次，去重）。
 *  3. 冲锋结束：实体 discard。
 *
 * 路线由 {@link SkyAttackGeometry.Lane} 描述：起点 = boss 位置，
 * 终点 = boss 朝目标方向延伸 chargeRange 格。halfWidth = 1.5（3 格宽伤害带）。
 * 客户端据同步数据 DATA_END_X/Z + DATA_HALF_W + DATA_STATE 自行渲染预警/剑模型。
 */
public class FeatherSwordEntity extends Entity implements GeoEntity {

    public static final int WARNING_TICKS = 60;
    public static final int CHARGE_TICKS = 16;
    /** 伤害半宽（格）。3 格宽伤害带。 */
    public static final float HALF_WIDTH = 1.5F;
    /** 基础伤害（受 FRBConfig.dmgFeatherSword() 倍率与全局倍率叠加）。 */
    public static final float BASE_DAMAGE = 8.0F;

    /** r96：GeckoLib 动画实例缓存（静态模型，无控制器）。 */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_END_X =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HALF_W =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);
    /** 冲锋方向 yaw（度），客户端据此摆正剑模型与预警矩形朝向。 */
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);
    /** r90：预警矩形贴地高度（boss 悬空时羽剑从地面冲锋，红线必须画在地上而非 boss 所在高度）。 */
    private static final EntityDataAccessor<Float> DATA_GROUND_Y =
            SynchedEntityData.defineId(FeatherSwordEntity.class, EntityDataSerializers.FLOAT);

    /** 状态：0=警告期，1=冲锋期，2=已完成（待清理）。 */
    public static final int STATE_WARNING = 0;
    public static final int STATE_CHARGING = 1;
    public static final int STATE_DONE = 2;

    private SkyAttackGeometry.Lane lane;
    private LivingEntity owner;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private int stateTimer = WARNING_TICKS;
    private long launchTick = 0L;

    public FeatherSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** 由 boss 在天空阶段调用：从 from（地面高度）朝目标方向冲锋 chargeRange 格。
     *  r90：groundY = 预警红线贴地高度；实体自带发光轮廓便于玩家识别。 */
    public static FeatherSwordEntity launch(ServerLevel level, Vec3 from, Vec3 dir,
                                             LivingEntity owner, float chargeRange, float scale,
                                             double groundY) {
        FeatherSwordEntity e = new FeatherSwordEntity(FRBEntities.FEATHER_SWORD.get(), level);
        Vec3 norm = dir.lengthSqr() > 1.0E-6D ? dir.normalize() : new Vec3(1, 0, 0);
        Vec3 end = from.add(norm.scale(chargeRange));
        e.setPos(from.x, from.y, from.z);
        e.owner = owner;
        e.lane = new SkyAttackGeometry.Lane(from.x, from.z, end.x, end.z, HALF_WIDTH);
        // Minecraft yaw：0=南(+Z)，atan2(z,x) 从 +X 起算，需 -90° 偏移
        float yaw = (float) Math.toDegrees(Math.atan2(norm.z, norm.x)) - 90.0F;
        e.entityData.set(DATA_STATE, STATE_WARNING);
        e.entityData.set(DATA_END_X, (float) end.x);
        e.entityData.set(DATA_END_Z, (float) end.z);
        e.entityData.set(DATA_HALF_W, HALF_WIDTH);
        e.entityData.set(DATA_SCALE, scale);
        e.entityData.set(DATA_YAW, yaw);
        e.entityData.set(DATA_GROUND_Y, (float) groundY);
        // 同时设置 yRot/yBodyRot/yHeadRot/yRotO，确保客户端渲染朝向与冲锋方向一致
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.setYBodyRot(yaw);
        e.setYHeadRot(yaw);
        // r90：羽剑发光（警告+冲锋全程高亮轮廓，结束 discard 自动消失）
        // r92 修复：1.20.1 MCP mapping 无 setGlowing 公开方法，改用 setSharedFlag(6, true)。
        e.setSharedFlag(6, true);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STATE, STATE_WARNING);
        this.entityData.define(DATA_END_X, 0.0F);
        this.entityData.define(DATA_END_Z, 0.0F);
        this.entityData.define(DATA_HALF_W, HALF_WIDTH);
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_GROUND_Y, 0.0F);
    }

    public int getState() { return this.entityData.get(DATA_STATE); }
    public float getEndX() { return this.entityData.get(DATA_END_X); }
    public float getEndZ() { return this.entityData.get(DATA_END_Z); }
    public float getHalfWidth() { return this.entityData.get(DATA_HALF_W); }
    public float getScale() { return this.entityData.get(DATA_SCALE); }
    public float getYawDeg() { return this.entityData.get(DATA_YAW); }
    /** r90：预警红线贴地 Y。 */
    public float getGroundY() { return this.entityData.get(DATA_GROUND_Y); }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // 客户端仅展示尾迹粒子（剑模型由 renderer 渲染）
            if (this.getState() == STATE_CHARGING && this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        0.0D, 0.02D, 0.0D);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        int state = this.getState();
        if (state == STATE_WARNING) {
            // 警告期：原地蓄势，偶尔冒火星
            if (this.stateTimer > 0) this.stateTimer--;
            if (this.stateTimer % 5 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 0.5D, this.getZ(), 2,
                        0.15D, 0.3D, 0.15D, 0.02D);
            }
            if (this.stateTimer <= 0) {
                this.entityData.set(DATA_STATE, STATE_CHARGING);
                this.stateTimer = CHARGE_TICKS;
                this.launchTick = sl.getServer().getTickCount();
                sl.playSound(null, this, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.4F);
            }
            return;
        }
        if (state == STATE_CHARGING) {
            if (this.lane == null) { this.discard(); return; }
            // 冲锋期：沿路线推进，扫掠玩家伤害
            double prev = SkyAttackGeometry.progress(sl.getServer().getTickCount() - 1,
                    this.launchTick, CHARGE_TICKS);
            double curr = SkyAttackGeometry.progress(sl.getServer().getTickCount(),
                    this.launchTick, CHARGE_TICKS);
            // 移动实体到当前进度位置
            double px = this.lane.xAt(curr);
            double pz = this.lane.zAt(curr);
            this.setPos(px, this.getY(), pz);
            this.hasImpulse = true;
            // 扫掠判定
            DamageSource src = this.owner != null
                    ? sl.damageSources().mobAttack(this.owner)
                    : sl.damageSources().magic();
            float dmg = FRBConfig.dmg(BASE_DAMAGE) * FRBConfig.dmgFeatherSword();
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(4.0D))) {
                if (this.hitEntityIds.contains(p.getId())) continue;
                if (this.lane.containsSweep(p.getX(), p.getZ(), prev, curr, 0.8D)) {
                    p.hurt(src, dmg);
                    p.push(this.lane.dirX() * 0.5D, 0.25D, this.lane.dirZ() * 0.5D);
                    p.hurtMarked = true;
                    this.hitEntityIds.add(p.getId());
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            p.getX(), p.getY() + 1.0D, p.getZ(), 3,
                            0.3D, 0.2D, 0.3D, 0.0D);
                }
            }
            if (this.stateTimer > 0) this.stateTimer--;
            if (this.stateTimer <= 0 || curr >= 1.0D) {
                this.entityData.set(DATA_STATE, STATE_DONE);
                // 冲锋尽头爆发粒子
                sl.sendParticles(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.5D, this.getZ(), 12,
                        0.4D, 0.3D, 0.4D, 0.1D);
                this.discard();
            }
            return;
        }
        // STATE_DONE 兜底
        this.discard();
    }

    @Override
    public boolean isPickable() { return false; }
    @Override
    public boolean hurt(DamageSource src, float amt) { return false; }
    @Override
    public boolean isInvulnerable() { return true; }
    @Override
    public boolean isInvulnerableTo(DamageSource src) { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.stateTimer = tag.getInt("FRBStateTimer");
        this.entityData.set(DATA_STATE, tag.getInt("FRBState"));
        this.entityData.set(DATA_END_X, tag.getFloat("FRBEndX"));
        this.entityData.set(DATA_END_Z, tag.getFloat("FRBEndZ"));
        this.entityData.set(DATA_HALF_W, tag.getFloat("FRBHalfW"));
        this.entityData.set(DATA_SCALE, tag.getFloat("FRBScale"));
        this.entityData.set(DATA_YAW, tag.getFloat("FRBYaw"));
        this.entityData.set(DATA_GROUND_Y, tag.getFloat("FRBGroundY"));
        // 重建 lane
        if (tag.contains("FRBStartX")) {
            this.lane = new SkyAttackGeometry.Lane(
                    tag.getDouble("FRBStartX"), tag.getDouble("FRBStartZ"),
                    tag.getDouble("FRBEndX"), tag.getDouble("FRBEndZ"),
                    tag.getFloat("FRBHalfW"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBStateTimer", this.stateTimer);
        tag.putInt("FRBState", this.getState());
        tag.putFloat("FRBEndX", this.getEndX());
        tag.putFloat("FRBEndZ", this.getEndZ());
        tag.putFloat("FRBHalfW", this.getHalfWidth());
        tag.putFloat("FRBScale", this.getScale());
        tag.putFloat("FRBYaw", this.getYawDeg());
        tag.putFloat("FRBGroundY", this.getGroundY());
        if (this.lane != null) {
            tag.putDouble("FRBStartX", this.lane.startX());
            tag.putDouble("FRBStartZ", this.lane.startZ());
        }
    }

    /** 客户端读取：警告期剩余 tick（=WARNING_TICKS - tickCount，仅警告期有效）。 */
    public int getWarningRemaining() {
        if (this.getState() != STATE_WARNING) return 0;
        return Math.max(0, WARNING_TICKS - this.tickCount);
    }

    // ---- GeoEntity 接口（r96：改用 GeckoLib geo 模型渲染）----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 静态模型，无动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object o) {
        return this.tickCount;
    }
}
