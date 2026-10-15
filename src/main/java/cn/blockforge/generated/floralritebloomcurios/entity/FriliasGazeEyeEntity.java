package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * r75：霜凝蓄力期间挂在 boss 头顶的"凝视之眼"——用户截图 #6 要求：
 * <ul>
 *   <li>1.5x 放大（在原本 boss 头顶的 gaze 印记基础上加大）；</li>
 *   <li>作为独立实体固定在 boss 头顶 getEyeY() + 1.6，不随玩家移动；</li>
 *   <li>永不旋转（render 层不朝相机方向旋转）；</li>
 *   <li>boss 身上不再喷 gaze 粒子（已移除 FriliasEntity.tickFrostGaze 中的 boss 周围 12 颗 FROST_EYE）。</li>
 * </ul>
 *
 * <p>由 FriliasEntity.startFrostGaze 起 spawn；tickFrostGaze 读条归零时 discard。
 * boss 死亡 / 切阶段也会在 FriliasEntity.remove 中兜底 discard。</p>
 */
public class FriliasGazeEyeEntity extends MechanicEntity {

    private static final EntityDataAccessor<Integer> DATA_BOSS_ID =
            SynchedEntityData.defineId(FriliasGazeEyeEntity.class, EntityDataSerializers.INT);

    public FriliasGazeEyeEntity(EntityType<? extends FriliasGazeEyeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    /**
     * 在 boss 头顶 spawn 一只凝视之眼。若 boss 上已有 gazeEye 则仅刷新跟随位置（避免重复 spawn）。
     */
    public static FriliasGazeEyeEntity spawnOrUpdate(ServerLevel level, FriliasEntity boss) {
        if (boss.gazeEye != null && boss.gazeEye.isAlive()) {
            boss.gazeEye.setPos(boss.getX(), boss.getEyeY() + 1.6D, boss.getZ());
            return boss.gazeEye;
        }
        FriliasGazeEyeEntity e = new FriliasGazeEyeEntity(
                FRBEntities.GAZE_EYE.get(), level);
        e.entityData.set(DATA_BOSS_ID, boss.getId());
        e.setPos(boss.getX(), boss.getEyeY() + 1.6D, boss.getZ());
        level.addFreshEntity(e);
        boss.gazeEye = e;
        return e;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BOSS_ID, -1);
    }

    public int getBossId() {
        return this.entityData.get(DATA_BOSS_ID);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.level() instanceof ServerLevel sl) {
            if (sl.getEntity(this.getBossId()) instanceof FriliasEntity boss) {
                if (!boss.isAlive() || boss.getFrostGazeTicks() <= 0) {
                    // boss 死了 / 霜凝读条已归零 → 让眼睛实体淡出
                    if (boss.gazeEye == this) {
                        boss.gazeEye = null;
                    }
                    this.discard();
                    return;
                }
                // r75：始终跟随 boss 头顶 +1.6 格，不旋转（rotateTo 不调用）。
                this.setPos(boss.getX(), boss.getEyeY() + 1.6D, boss.getZ());
                this.setRot(0.0F, 0.0F);
            } else {
                // boss 实体不存在（被回收）→ 自动 discard
                this.discard();
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_BOSS_ID, tag.getInt("FRBBoss"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBBoss", this.entityData.get(DATA_BOSS_ID));
    }
}