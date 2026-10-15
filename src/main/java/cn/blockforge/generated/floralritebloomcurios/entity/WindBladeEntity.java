package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 风刃（F2 天空阶段）：非追踪直线弹。
 *
 * - 6 点物理伤害（受护甲减免）+ 3 格击退
 * - LIFE_TICKS 后自动消失
 * - 命中玩家立即结算，不追踪
 *
 * 视觉尾迹直接走粒子（SpriteSet 由 FRBSetup 注册时复用 petal 贴图）。
 */
public class WindBladeEntity extends Projectile {

    public static final int LIFE_TICKS = 60;
    private static final float DAMAGE = 6.0F;
    private int life = LIFE_TICKS;
    private LivingEntity owner;

    public WindBladeEntity(EntityType<? extends WindBladeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static WindBladeEntity shoot(ServerLevel level, Vec3 from, Vec3 dir, LivingEntity owner) {
        WindBladeEntity e = new WindBladeEntity(FRBEntities.WIND_BLADE.get(), level);
        e.setPos(from.x, from.y, from.z);
        e.setOwner(owner);
        e.setDeltaMovement(dir.scale(1.4D));
        e.owner = owner;
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {}

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
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.CLOUD,
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
                && target != this.owner) {
            DamageSource src = this.owner != null
                    ? this.level().damageSources().mobAttack(this.owner)
                    : this.level().damageSources().magic();
            target.hurt(src, DAMAGE);
            // 3 格击退
            Vec3 knock = this.getDeltaMovement().normalize().scale(0.6D);
            target.push(knock.x, 0.2D, knock.z);
            target.hurtMarked = true;
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + 0.8D, target.getZ(),
                    10, 0.3D, 0.5D, 0.3D, 0.0D);
        }
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return target != this.owner && target.isAlive() && target instanceof LivingEntity;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** r74：同 SpikeVolleyEntity——Projectile 默认 isInvulnerableTo=false，
     *      hurt() 会 markHurt() 变红。补齐三件套（与 PetalVolleyEntity 一致）。 */
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.life);
    }
}