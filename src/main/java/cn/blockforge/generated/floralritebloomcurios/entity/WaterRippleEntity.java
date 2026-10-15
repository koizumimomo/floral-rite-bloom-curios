package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r96 万变水波（云雨阶段持久地面机制）：
 *
 * boss 转阶段后在自身 8 格外生成 4 个半径 3 格的圆形水面，
 * 只要玩家踏入其中，就获得 5 秒【浸泡】（受伤 +25%）并叠加 1 层雨之华。
 *
 * 该实体本身没有生命值、不移动、不被攻击；渲染器贴地画一张水波贴图。
 * 当云雨阶段全屏 AOE 释放后，boss 会调用 {@link #discard()} 旧水波，
 * 再在正点/斜点互换的位置重新生成新水波。
 *
 * 判定半径 3 格（与贴图对应），效果每 20 tick 结算一次（踏入持续刷新浸泡时长）。
 */
public class WaterRippleEntity extends MechanicEntity {

    /** 判定半径（格）。 */
    public static final double RADIUS = 3.0D;

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(WaterRippleEntity.class, EntityDataSerializers.INT);

    public WaterRippleEntity(EntityType<? extends WaterRippleEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    /** 服务端生成一个万变水波。 */
    public static WaterRippleEntity spawn(ServerLevel level, double x, double y, double z, int lifetime) {
        WaterRippleEntity e = new WaterRippleEntity(FRBEntities.WATER_RIPPLE.get(), level);
        e.setPos(x, y + 0.05D, z);
        e.entityData.set(DATA_LIFETIME, lifetime);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 6000);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        int left = this.entityData.get(DATA_LIFETIME) - 1;
        this.entityData.set(DATA_LIFETIME, left);
        if (left <= 0) {
            this.discard();
            return;
        }
        // 每 20 tick 给半径 3 格内玩家附加浸泡 5s + 1 层雨之华
        if (this.tickCount % 20 == 0) {
            ServerLevel sl = (ServerLevel) this.level();
            for (Player p : sl.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(RADIUS, 1.0D, RADIUS))) {
                double dx = p.getX() - this.getX();
                double dz = p.getZ() - this.getZ();
                if (dx * dx + dz * dz <= RADIUS * RADIUS) {
                    p.addEffect(new MobEffectInstance(FRBEffects.SOGGY.get(), 100, 0,
                            false, true, true));
                    var inst = p.getEffect(FRBEffects.RAIN_BLOOM.get());
                    int next = inst == null ? 0 : Math.min(inst.getAmplifier() + 1, 11);
                    p.addEffect(new MobEffectInstance(FRBEffects.RAIN_BLOOM.get(), 120, next,
                            false, true, true));
                }
            }
        }
    }

    public int getLife() { return this.entityData.get(DATA_LIFETIME); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_LIFETIME, tag.getInt("FRBLife"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.entityData.get(DATA_LIFETIME));
    }
}
