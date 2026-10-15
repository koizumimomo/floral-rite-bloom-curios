package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 死亡之花（世界之花 Boss · 枯萎阶段）：环绕 Boss 的三朵护命花，
 * 各 100 点生命、6 点护甲；全部被击败后 Boss 的无敌解除。
 */
public class DeathFlowerEntity extends Monster {

    private UUID bossId;
    private int orbitSlot;
    /** r66：平滑环绕角（弧度）。NaN = 首次定位；花朵被击杀后剩余花按存活数量重新均分时平滑过渡。 */
    private double orbitAngle = Double.NaN;

    public DeathFlowerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoGravity(true);
        this.setCustomName(Component.translatable("entity.frb.death_flower"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public void bind(UUID bossId, int orbitSlot) {
        this.bossId = bossId;
        this.orbitSlot = orbitSlot;
    }

    /** 给 FriliasEntity 用来判复活花的花期队列。 */
    public int getOrbitSlot() {
        return this.orbitSlot;
    }

    /** 枯萎阶段的护命花与 Boss 一样免疫一切效果（A5：现在统一挡全量，不只是黑死病）。 */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    protected void registerGoals() {
        // 无 AI：只负责绕 Boss 旋转，等待被击败。
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) return;
        FriliasEntity boss = this.bossId == null ? null
                : level.getEntity(this.bossId) instanceof FriliasEntity f ? f : null;
        if (boss == null || !boss.isAlive() || boss.getFrbPhase() != FriliasEntity.PHASE_COUNT - 1) {
            this.discard();
            return;
        }
        // r66：均分环绕——按存活花朵数量把圆周均分（3 朵 = 120°，2 朵 = 180°），
        // 修复原来固定 66 tick 槽位偏移（≈75.6°）导致的"上、下、右"不均排布；
        // 击杀一朵后剩余花以每 tick 8% 的收敛速度平滑滑到新均分位置。
        java.util.List<DeathFlowerEntity> alive = new java.util.ArrayList<>();
        for (UUID id : boss.frbFlowerIds()) {
            if (level.getEntity(id) instanceof DeathFlowerEntity f && f.isAlive()) {
                alive.add(f);
            }
        }
        alive.sort(java.util.Comparator.comparingInt(DeathFlowerEntity::getOrbitSlot));
        int idx = Math.max(0, alive.indexOf(this));
        int n = Math.max(1, alive.size());
        double targetAngle = this.tickCount * 0.02D + (Math.PI * 2.0D / n) * idx;
        if (Double.isNaN(this.orbitAngle)) {
            this.orbitAngle = targetAngle;
        } else {
            double diff = Math.toRadians(
                    net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(targetAngle - this.orbitAngle)));
            this.orbitAngle += diff * 0.08D;
        }
        double a = this.orbitAngle;
        double x = boss.getX() + Math.cos(a) * 3.4D;
        double y = boss.getY() + 2.2D + Math.sin((this.tickCount + this.orbitSlot * 40) * 0.05D) * 0.45D;
        double z = boss.getZ() + Math.sin(a) * 3.4D;
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel level
                && this.bossId != null && level.getEntity(this.bossId) instanceof FriliasEntity boss) {
            boss.onDeathFlowerKilled(this.orbitSlot, level.getGameTime());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.bossId != null) tag.putUUID("FRBBoss", this.bossId);
        tag.putInt("FRBOrbitSlot", this.orbitSlot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FRBBoss")) this.bossId = tag.getUUID("FRBBoss");
        this.orbitSlot = tag.getInt("FRBOrbitSlot");
    }
}
