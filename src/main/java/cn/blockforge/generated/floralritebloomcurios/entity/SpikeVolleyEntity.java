package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 荆棘 / 冰刺飞弹（r64）：从 boss 身后两侧发射的非追踪子弹。
 *
 * - kind 0 = 荆棘（自然阶段，命中 5 点物理伤害 + 玩家头顶绿色 marker 反馈）
 * - kind 1 = 冰刺（霜华阶段，命中 6 点魔法伤害 + 3 秒缓慢 III）
 * - LIFE_TICKS 后自动消失
 *
 * A5：飞弹属于机制实体（Projectile 自带 knockback，伤害逻辑在 onHit 中处理即可，
 * 不需要效果免疫——飞弹本身不接受任何 effect）。
 *
 * 视觉走 SpikeEntity 渲染（贴图复用）。
 */
public class SpikeVolleyEntity extends Projectile {

    public static final int KIND_THORN = 0;
    public static final int KIND_ICE = 1;

    public static final int LIFE_TICKS = 80; // 4 秒飞行时间
    private static final float DAMAGE_THORN = 5.0F;
    private static final float DAMAGE_ICE = 6.0F;

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(SpikeVolleyEntity.class, EntityDataSerializers.INT);

    private int life = LIFE_TICKS;
    private LivingEntity owner;

    public SpikeVolleyEntity(EntityType<? extends SpikeVolleyEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /**
     * 服务端生成：从指定位置沿 dir 发射一颗荆棘/冰刺飞弹。
     */
    public static SpikeVolleyEntity shoot(ServerLevel level, Vec3 from, Vec3 dir, int kind,
                                          LivingEntity owner) {
        SpikeVolleyEntity e = new SpikeVolleyEntity(FRBEntities.SPIKE_VOLLEY.get(), level);
        e.setPos(from.x, from.y, from.z);
        e.setOwner(owner);
        e.entityData.set(KIND, kind);
        e.owner = owner;
        e.setDeltaMovement(dir.scale(1.5D)); // 风刃同档速度
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, KIND_ICE);
    }

    public int getKind() {
        return this.entityData.get(KIND);
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
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                if (this.isRemoved()) return;
            }
            if (this.tickCount % 2 == 0) {
                var particle = getKind() == KIND_THORN
                        ? ParticleTypes.MYCELIUM : ParticleTypes.SNOWFLAKE;
                ((ServerLevel) this.level()).sendParticles(particle,
                        this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) return;
        if (result instanceof net.minecraft.world.phys.EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity target
                && target != this.owner && !(target instanceof FriliasEntity)) {
            DamageSource src = this.owner != null
                    ? (getKind() == KIND_THORN
                            ? this.level().damageSources().thorns(this.owner)
                            : this.level().damageSources().indirectMagic(this.owner, this.owner))
                    : (getKind() == KIND_THORN
                            ? this.level().damageSources().cactus()
                            : this.level().damageSources().freeze());
            if (getKind() == KIND_THORN) {
                // r72：荆棘双弹伤害走 FRBConfig 三层倍率（基础×全局×阶段倍率）
                target.hurt(src, FRBConfig.dmg(DAMAGE_THORN) * FRBConfig.dmgThornVolley());
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.MYCELIUM,
                        target.getX(), target.getY() + 0.5D, target.getZ(),
                        12, 0.3D, 0.4D, 0.3D, 0.02D);
            } else {
                // r72：冰刺双弹伤害走 FRBConfig 三层倍率
                target.hurt(src, FRBConfig.dmg(DAMAGE_ICE) * FRBConfig.dmgFrostIceSpike());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 0.8D, target.getZ(),
                        20, 0.4D, 0.5D, 0.4D, 0.04D);
            }
        }
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return target != this.owner && target.isAlive() && target instanceof LivingEntity
                && !(target instanceof FriliasEntity);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** r74：Projectile 默认 isInvulnerableTo=false → hurt() 触发 markHurt() 变红。
     *      飞弹本体不受任何伤害，必须显式覆写三件套（与 PetalVolleyEntity 一致）。 */
    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("FRBLife");
        this.entityData.set(KIND, tag.getInt("FRBKind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.life);
        tag.putInt("FRBKind", getKind());
    }
}