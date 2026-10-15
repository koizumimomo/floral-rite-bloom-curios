package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.item.BountifulItem;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 丰饶的花瓣 / 叶片弹：低伤害自动追踪弹射物。
 * 服务端每 tick 朝目标转向（锁定目标优先，否则最近的敌对生物），
 * 客户端用同步的速度矢量做同样的直线积分保证平滑渲染。
 */
public class PetalVolleyEntity extends Projectile {

    public static final int LIFETIME = 60;
    /** 基础伤害；实际伤害发射时按（基础 + 玩家攻击力）× 花仪铭记倍率计算（见 BountifulItem）。 */
    public static final float BASE_DAMAGE = 4.0F;
    private static final double SPEED = 1.0D;
    private static final double TURN = 0.2D;
    /** 0 = 花瓣，1 = 叶片（客户端选贴图）。 */
    private static final EntityDataAccessor<Integer> DATA_LEAF =
            SynchedEntityData.defineId(PetalVolleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VX =
            SynchedEntityData.defineId(PetalVolleyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VY =
            SynchedEntityData.defineId(PetalVolleyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VZ =
            SynchedEntityData.defineId(PetalVolleyEntity.class, EntityDataSerializers.FLOAT);

    private int life = LIFETIME;
    /** 命中伤害（发射时确定）。 */
    private float damage = BASE_DAMAGE;

    public PetalVolleyEntity(EntityType<? extends PetalVolleyEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static PetalVolleyEntity shoot(ServerLevel level, Player owner, Vec3 from, Vec3 dir, boolean leaf,
                                          float damage) {
        PetalVolleyEntity e = new PetalVolleyEntity(FRBEntities.PETAL_VOLLEY.get(), level);
        e.setOwner(owner);
        e.setPos(from.x, from.y, from.z);
        e.entityData.set(DATA_LEAF, leaf ? 1 : 0);
        e.damage = damage;
        e.setVelocity(dir.scale(SPEED));
        level.addFreshEntity(e);
        return e;
    }

    private void setVelocity(Vec3 v) {
        this.setDeltaMovement(v);
        this.entityData.set(DATA_VX, (float) v.x);
        this.entityData.set(DATA_VY, (float) v.y);
        this.entityData.set(DATA_VZ, (float) v.z);
    }

    public boolean isLeaf() {
        return this.entityData.get(DATA_LEAF) == 1;
    }

    /** r71（bug C）：花瓣弹属于机制飞行物，永远不接受伤害，避免身体变红特效。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LEAF, 0);
        this.entityData.define(DATA_VX, 0.0F);
        this.entityData.define(DATA_VY, 0.0F);
        this.entityData.define(DATA_VZ, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide) {
            if (--this.life <= 0) {
                this.discard();
                return;
            }
            LivingEntity target = findTarget();
            if (target != null) {
                Vec3 want = target.getEyePosition().subtract(this.position());
                if (want.lengthSqr() > 1.0e-4D) {
                    Vec3 cur = motion.normalize();
                    Vec3 steer = cur.scale(1.0D - TURN).add(want.normalize().scale(TURN)).normalize();
                    motion = steer.scale(SPEED);
                    this.setVelocity(motion);
                }
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                if (this.isRemoved()) return;
            }
            if (this.tickCount % 7 == 0) { // 尾迹密度约为原 1/3，配合无重力淡出粒子已足够清晰
                ((ServerLevel) this.level()).sendParticles(trailParticle(),
                        this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        } else {
            // 客户端：用最后一次同步的速度矢量积分，位置由服务端周期性校正
            float vx = this.entityData.get(DATA_VX);
            float vy = this.entityData.get(DATA_VY);
            float vz = this.entityData.get(DATA_VZ);
            motion = Float.isFinite(vx) && Float.isFinite(vy) && Float.isFinite(vz)
                    ? new Vec3(vx, vy, vz) : Vec3.ZERO;
            this.setDeltaMovement(motion);
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    /** 锁定目标优先；否则取弹体 16 格内最近的敌对生物（Monster 含 boss 与其召唤物）。 */
    @Nullable
    private LivingEntity findTarget() {
        if (!(this.level() instanceof ServerLevel level)) return null;
        Entity owner = this.getOwner();
        if (owner != null && owner.getPersistentData().hasUUID(BountifulItem.TAG_LOCK)) {
            Entity locked = level.getEntity(owner.getPersistentData().getUUID(BountifulItem.TAG_LOCK));
            if (locked instanceof LivingEntity living && living.isAlive()
                    && living.distanceToSqr(this) < 64.0D * 64.0D) {
                return living;
            }
        }
        List<Monster> near = level.getEntitiesOfClass(Monster.class,
                this.getBoundingBox().inflate(16.0D), LivingEntity::isAlive);
        Monster best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster m : near) {
            double d = m.distanceToSqr(this);
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    private ParticleOptions trailParticle() {
        return FRBParticles.BOUNTIFUL_TRAIL.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();
        Entity owner = this.getOwner();
        // 花灵伤害：专属 DamageType（挂 minecraft:bypasses_cooldown）——无视原版受击无敌帧，高频弹幕每发全额结算。
        // 1.20.1 的 DamageSources 三参 source 是 private，直接 new DamageSource(holder, 直击体, 归属者)
        Holder<DamageType> floral = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(cn.blockforge.generated.floralritebloomcurios.registry.FRBDamageTypes.FLORAL);
        DamageSource source = new DamageSource(floral, this, owner);
        Entity hit = result.getEntity();
        // 末影龙部件不是 LivingEntity：取父龙直接结算；未命中弱点（hurt 返回 false）时不销毁花瓣，让其继续追踪
        if (hit instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon = part.getParent();
            if (dragon != null && dragon.hurt(source, this.damage)) {
                applyCharmEffects(owner, dragon);
                impact(level);
            }
            return;
        }
        if (hit instanceof LivingEntity living && living.hurt(source, this.damage)) {
            applyCharmEffects(owner, living);
            impact(level);
        } else if (!(hit instanceof LivingEntity)) {
            impact(level);
        }
    }

    /**
     * 花灵联动：佩戴焚生藤 → 命中点燃，佩戴黑灵草 → 凋零 II，佩戴森语藤 → 中毒 II。
     * 效果为固定时长/强度，重复命中只会刷新不会叠加；饰品被封印时（FRBState.active=false）不生效。
     */
    private static void applyCharmEffects(Entity owner, LivingEntity target) {
        if (!(owner instanceof Player player)) return;
        if (FRBState.active(player, CharmType.EMBER_VINE)) {
            target.setSecondsOnFire(4);
        }
        if (FRBState.active(player, CharmType.BLACK_SPIRIT)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }
        if (FRBState.active(player, CharmType.FOREST_WHISPER)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;
        impact((ServerLevel) this.level());
    }

    private void impact(ServerLevel level) {
        level.sendParticles(trailParticle(),
                this.getX(), this.getY(), this.getZ(), 2, 0.15D, 0.15D, 0.15D, 0.02D);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("FRBLife");
        this.damage = tag.contains("FRBDamage") ? tag.getFloat("FRBDamage") : BASE_DAMAGE;
        if (tag.hasUUID("FROwner") && this.level() instanceof ServerLevel level) {
            this.setOwner(level.getEntity(tag.getUUID("FROwner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.life);
        tag.putFloat("FRBDamage", this.damage);
        Entity owner = this.getOwner();
        if (owner != null) {
            tag.putUUID("FROwner", owner.getUUID());
        }
    }
}
