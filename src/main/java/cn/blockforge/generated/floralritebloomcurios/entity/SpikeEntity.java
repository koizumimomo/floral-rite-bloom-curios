package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 延迟爆发的地刺（世界之花 Boss 用）：kind 0 = 荆棘（自然阶段，玩家脚下 1.5s 后刺出，
 * 爆发时结算一次伤害，随后 8 tick 回缩）；kind 1 = 冰刺（霜华阶段，命中后碎裂消退）。
 *
 * A5：地刺属于机制实体，继承 MechanicEntity 即可免疫一切伤害/效果。
 */
public class SpikeEntity extends MechanicEntity {

    public static final int KIND_THORN = 0;
    public static final int KIND_ICE = 1;

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);

    private int ownerId = -1;

    public SpikeEntity(EntityType<? extends SpikeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, double x, double y, double z,
                             int kind, int fuse, @Nullable LivingEntity owner) {
        SpikeEntity spike = new SpikeEntity(FRBEntities.SPIKE.get(), level);
        spike.setPos(x, y, z);
        spike.entityData.set(KIND, kind);
        spike.entityData.set(FUSE, fuse);
        if (owner != null) spike.ownerId = owner.getId();
        level.addFreshEntity(spike);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, KIND_ICE);
        this.entityData.define(FUSE, 30);
    }

    public int getKind() {
        return this.entityData.get(KIND);
    }

    public int getFuse() {
        return this.entityData.get(FUSE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        int fuse = this.entityData.get(FUSE) - 1;
        this.entityData.set(FUSE, fuse);
        if (fuse > 0) return;
        if (fuse == 0) burst();
        if (fuse <= -8) this.discard();
    }

    private void burst() {
        ServerLevel level = (ServerLevel) this.level();
        Vec3 c = this.position();
        if (getKind() == KIND_THORN) {
            level.sendParticles(ParticleTypes.MYCELIUM, c.x, c.y + 0.9D, c.z,
                    25, 0.4D, 0.9D, 0.4D, 0.02D);
            level.sendParticles(ParticleTypes.GLOW_SQUID_INK, c.x, c.y + 0.5D, c.z,
                    6, 0.3D, 0.5D, 0.3D, 0.01D);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SHROOMLIGHT_FALL, SoundSource.HOSTILE, 1.2F, 0.7F);
        } else {
            level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.8D, c.z,
                    40, 0.7D, 0.9D, 0.7D, 0.1D);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 0.7F);
        }
        Entity ownerEnt = this.ownerId >= 0 ? level.getEntity(this.ownerId) : null;
        LivingEntity owner = ownerEnt instanceof LivingEntity le ? le : null;
        DamageSource src = getKind() == KIND_THORN
                ? (owner != null ? level.damageSources().thorns(owner) : level.damageSources().cactus())
                : (owner != null ? level.damageSources().indirectMagic(owner, owner) : level.damageSources().freeze());
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(1.0D, 0.9D, 1.0D),
                e -> e != owner && !(e instanceof FriliasEntity))) {
            if (getKind() == KIND_THORN) {
                t.hurt(src, 5.0F);
            } else {
                t.hurt(src, 6.0F);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(KIND, tag.getInt("FRBKind"));
        this.entityData.set(FUSE, tag.getInt("FRBFuse"));
        this.ownerId = tag.getInt("FRBOwner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBKind", getKind());
        tag.putInt("FRBFuse", this.entityData.get(FUSE));
        tag.putInt("FRBOwner", this.ownerId);
    }
}
