package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * r92：通用 AOE 预警法阵（轻量纯展示实体）。
 *
 * 与 {@link FlowerCradleSigilEntity} 彻底解耦——后者是"花之摇篮"机制的
 * 结算载体（带倒计时、触发落花、判定 buff），本实体只是"在地面画一个
 * 预警圈、倒计时结束自动消失"，供荆棘双弹 / 冰刺双弹 等需要
 * 提前告知落点但不产生机制结算的技能使用。
 *
 * 历史上荆棘/冰刺的预警直接复用了花之摇篮的贴图实体（spawnWarn），
 * 导致玩家在花之摇篮机制触发的同时又看到荆棘预警法阵，
 * 视觉上"几乎每次连着两张一模一样的法阵"。r92 拆出独立实体 +
 * 独立贴图（aoe_circle.png）后两种预警不再混淆。
 *
 * 颜色由 RGB 三个通道的同步数据控制（荆棘绿 / 冰刺蓝）。
 */
public class AoeWarnSigilEntity extends MechanicEntity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(AoeWarnSigilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_R =
            SynchedEntityData.defineId(AoeWarnSigilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_G =
            SynchedEntityData.defineId(AoeWarnSigilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_B =
            SynchedEntityData.defineId(AoeWarnSigilEntity.class, EntityDataSerializers.FLOAT);

    /** 预警圈半径（格），与贴图尺寸对应。 */
    public static final double RADIUS = 2.0D;

    public AoeWarnSigilEntity(EntityType<? extends AoeWarnSigilEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    /**
     * 服务端生成一个预警法阵。
     *
     * @param duration 存活 tick（倒计时归零自动消失）
     * @param r/g/b    染色（0~1），贴图本身为白色/浅色圆形 AOE 图标
     */
    public static void spawn(ServerLevel level, BlockPos ground, int duration,
                             float r, float g, float b) {
        AoeWarnSigilEntity e = new AoeWarnSigilEntity(
                FRBEntities.AOE_WARN_SIGIL.get(), level);
        e.setPos(ground.getX() + 0.5D, ground.getY() + 0.05D, ground.getZ() + 0.5D);
        e.entityData.set(DATA_LIFETIME, duration);
        e.entityData.set(DATA_R, r);
        e.entityData.set(DATA_G, g);
        e.entityData.set(DATA_B, b);
        level.addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 60);
        this.entityData.define(DATA_R, 1.0F);
        this.entityData.define(DATA_G, 1.0F);
        this.entityData.define(DATA_B, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        int left = this.entityData.get(DATA_LIFETIME) - 1;
        this.entityData.set(DATA_LIFETIME, left);
        if (left <= 0) {
            this.discard();
        }
    }

    public int getLife() { return this.entityData.get(DATA_LIFETIME); }
    public float getR() { return this.entityData.get(DATA_R); }
    public float getG() { return this.entityData.get(DATA_G); }
    public float getB() { return this.entityData.get(DATA_B); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_LIFETIME, tag.getInt("FRBLife"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.entityData.get(DATA_LIFETIME));
    }
}
