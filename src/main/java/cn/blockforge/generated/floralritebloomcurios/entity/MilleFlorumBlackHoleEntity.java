package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 万花凋零咏唱期间悬浮在 boss 头顶的黑洞实体。
 *
 * - 跟随 boss 头顶 1.5 格
 * - 每 tick 喷 void_mote + void_core 紫色飞溅
 * - 持续时长由 Boss 通过外部 scheduleDelayedTask 控制
 * - 不渲染（黑洞效果由 boss_finale.fsh 后处理 shader 完成），但 spawn 用于服务端逻辑标识 + boss 位置广播
 *
 * 这里 mesh 给一个不可见的简单实体，让客户端有跟踪目标。
 */
public class MilleFlorumBlackHoleEntity extends MechanicEntity {

    private static final EntityDataAccessor<Integer> DATA_BOSS_ID =
            SynchedEntityData.defineId(MilleFlorumBlackHoleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(MilleFlorumBlackHoleEntity.class, EntityDataSerializers.INT);

    /** 持续时长：120 tick（6 秒）。 */
    public static final int LIFETIME = 120;

    public MilleFlorumBlackHoleEntity(EntityType<? extends MilleFlorumBlackHoleEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static MilleFlorumBlackHoleEntity spawn(ServerLevel level, FriliasEntity boss) {
        MilleFlorumBlackHoleEntity e = new MilleFlorumBlackHoleEntity(
                FRBEntities.MILLE_FLORUM_BLACK_HOLE.get(), level);
        e.entityData.set(DATA_BOSS_ID, boss.getId());
        e.entityData.set(DATA_LIFE, LIFETIME);
        e.setPos(boss.getX(), boss.getEyeY() + 1.5D, boss.getZ());
        level.addFreshEntity(e);
        return e;
    }

    public int getBossId() {
        return this.entityData.get(DATA_BOSS_ID);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BOSS_ID, -1);
        this.entityData.define(DATA_LIFE, LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // 跟随 boss 头顶
        if (this.level() instanceof ServerLevel sl) {
            if (sl.getEntity(this.getBossId()) instanceof FriliasEntity boss) {
                this.setPos(boss.getX(), boss.getEyeY() + 1.5D, boss.getZ());
            }
        }
        int life = this.entityData.get(DATA_LIFE) - 1;
        this.entityData.set(DATA_LIFE, life);
        if (life <= 0) {
            this.discard();
            return;
        }
        // r98：改用 time_phase_06_void 粒子（比 VOID_SLASH 更小），营造凋零虚空氛围
        if (this.level() instanceof ServerLevel s) {
            s.sendParticles(FRBParticles.TIME_PHASE_06.get(),
                    this.getX(), this.getY(), this.getZ(),
                    6, 0.5D, 0.5D, 0.5D, 0.05D);
            s.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    12, 0.8D, 0.8D, 0.8D, 0.06D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_BOSS_ID, tag.getInt("FRBBoss"));
        this.entityData.set(DATA_LIFE, tag.getInt("FRBLife"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBBoss", this.entityData.get(DATA_BOSS_ID));
        tag.putInt("FRBLife", this.entityData.get(DATA_LIFE));
    }
}