package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 积雨云（E3 云雨阶段）：缓慢向下方玩家头顶漂移的云朵。
 *
 * - 持续 600 tick（30 秒）
 * - 下方 8 格内每 20 tick 给玩家附加【潮湿】效果（持续 60s，叠加刷新）
 * - 沿用 PetalVolleyEntity 的漂移逻辑：每 tick 朝最近的玩家方向前进 0.05 格
 *
 * A5：积雨云属于机制实体（MechanicEntity 免疫效果）。
 *
 * r64：积雨云跨阶段应消失——beginTransition(oldPhase=3) 调用 discardAll()。
 * r64：渲染改走用户提供的 rain_cloud.geo.json + rain_cloud.png（GeckoLib GeoEntity）。
 */
public class RainCloudEntity extends MechanicEntity implements GeoEntity {

    private static final int LIFE = 600;
    private int life = LIFE;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RainCloudEntity(EntityType<? extends RainCloudEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, double x, double y, double z) {
        RainCloudEntity e = new RainCloudEntity(FRBEntities.RAIN_CLOUD.get(), level);
        e.setPos(x, y, z);
        level.addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        // 静态模型，不需要动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();
        if (--this.life <= 0) {
            this.discard();
            return;
        }
        // r66 漂移逻辑：缓慢追踪最近玩家（水平 0.05 格/tick），高度恒定贴地面 5 格（修复贴地飞行）
        Player nearest = level.getNearestPlayer(this, 32.0D);
        double vx = 0.0D;
        double vz = 0.0D;
        if (nearest != null) {
            Vec3 dir = nearest.position().subtract(this.position());
            double hlen = Math.max(0.001D, Math.hypot(dir.x, dir.z));
            if (hlen > 1.0D) {
                vx = dir.x / hlen * 0.05D;
                vz = dir.z / hlen * 0.05D;
            }
        }
        // r96：积雨云高度由"地面+5"改为"最近玩家头顶+3"——
        //      原本贴地面 5 格在 boss 战斗中玩家经常看不见雨云，
        //      改为跟随最近玩家头部上方 3 格，确保雨水始终可见且能淋到玩家。
        double hoverY;
        if (nearest != null) {
            hoverY = nearest.getY() + nearest.getEyeHeight() + 3.0D;
        } else {
            int groundY = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    this.getBlockX(), this.getBlockZ());
            hoverY = groundY + 5.0D;
        }
        double vy = net.minecraft.util.Mth.clamp((hoverY - this.getY()) * 0.06D, -0.12D, 0.12D);
        this.setDeltaMovement(vx, vy, vz);
        this.hasImpulse = true;
        this.move(MoverType.SELF, this.getDeltaMovement());

        // 每 20 tick 给下方 8 格内玩家附加【潮湿】效果（持续 60s）
        if (this.tickCount % 20 == 0) {
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), LivingEntity::isAlive)) {
                if (t instanceof Player player) {
                    player.addEffect(new MobEffectInstance(FRBEffects.DAMPNESS.get(), 1200, 0, false, true, true));
                }
            }
        }
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