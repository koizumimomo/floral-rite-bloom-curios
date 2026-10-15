package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * 虚空裂隙（世界之花 Boss · 虚空阶段）：Boss 周围 32 格内每 2 秒生成一条，
 * 触碰者受到 10 点虚空伤害并被传送到附近没有裂隙的地方（传送本身即是主要惩罚）。
 * Boss 离场时统一清除。
 *
 * G2：裂隙生成时会召唤 enderslash 紫色飞溅 + 吸引 2 格内玩家 + 地面铺一层 void_erosion 附层。
 *
 * A5：裂隙属于机制实体，继承 MechanicEntity 即可免疫一切伤害/效果。
 */
public class VoidRiftEntity extends MechanicEntity {

    public static final int LIFE_TICKS = 600;
    private int life = LIFE_TICKS;
    private final Map<Integer, Long> hitCooldowns = new HashMap<>();

    public VoidRiftEntity(EntityType<? extends VoidRiftEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, double x, double y, double z) {
        VoidRiftEntity rift = new VoidRiftEntity(FRBEntities.VOID_RIFT.get(), level);
        rift.setPos(x, y, z);
        level.addFreshEntity(rift);
    }

    @Override
    protected void defineSynchedData() {
        // no-op: 继承 MechanicEntity 后此处无需任何同步字段；
        // 但 Forge 要求该方法存在。
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
        if (this.tickCount % 2 == 0) {
            Vec3 c = this.position();
            level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0D, c.z,
                    4, 0.3D, 0.9D, 0.3D, 0.06D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1.4D, c.z,
                    2, 0.2D, 0.6D, 0.2D, 0.0D);
        }
        // G2 吸引：裂隙生成后每 5 tick 把 2 格内玩家向中心拽 0.02 格
        if (this.tickCount > 20 && this.tickCount % 5 == 0) {
            Vec3 c = this.position();
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(2.0D, 1.5D, 2.0D),
                    e -> !(e instanceof FriliasEntity) && !(e instanceof DeathFlowerEntity))) {
                Vec3 pull = c.subtract(t.position()).normalize().scale(0.02D);
                t.push(pull.x, 0.0D, pull.z);
            }
        }
        // G2 虚空侵蚀附层：r64 修复用户反馈"侵蚀地块应该和生之焰一样是附层"——
        // 裂隙悬浮在空中，直接 setBlockAndUpdate 在它 Y 位置会让 canSurvive 失败（下方是空气）；
        // 改为取该 XZ 的地表高度图（与生之焰一致，在方块顶面铺一层）。
        if (this.tickCount == 21) {
            BlockPos baseXZ = new BlockPos(
                    net.minecraft.util.Mth.floor(this.getX()), 0,
                    net.minecraft.util.Mth.floor(this.getZ()));
            BlockPos groundTop = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseXZ);
            // r103：getHeightmapPos 已返回地面上方的空气位置，直接在该处放置；
            // 若该位置是花/草/紫水晶簇则原地替换
            if (level.hasChunk(groundTop.getX() >> 4, groundTop.getZ() >> 4)) {
                var existing = level.getBlockState(groundTop);
                if (!existing.is(cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks.VOID_EROSION.get())
                        && (existing.isAir() || cn.blockforge.generated.floralritebloomcurios.block.LifeFireBlock
                                .isReplaceableFlora(existing))) {
                    level.setBlockAndUpdate(groundTop, cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks.VOID_EROSION.get().defaultBlockState());
                }
            }
        }
        // 裂隙先显形一秒，再启用碰撞伤害。
        if (this.tickCount < 20) return;
        long now = level.getGameTime();
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(0.9D, 0.6D, 0.9D),
                e -> !(e instanceof FriliasEntity) && !(e instanceof DeathFlowerEntity))) {
            Long until = this.hitCooldowns.get(t.getId());
            if (until != null && now < until) continue;
            this.hitCooldowns.put(t.getId(), now + 60L);
            t.hurt(level.damageSources().magic(), 10.0F);
            Vec3 dest = findClearSpot(level, t.position());
            if (dest != null) {
                level.sendParticles(ParticleTypes.PORTAL, t.getX(), t.getY() + 1.0D, t.getZ(),
                        30, 0.4D, 0.9D, 0.4D, 0.1D);
                t.teleportTo(dest.x, dest.y, dest.z);
                level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0D, dest.z,
                        30, 0.4D, 0.9D, 0.4D, 0.1D);
                level.playSound(null, dest.x, dest.y, dest.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.7F);
                if (t instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("frb.boss.void.rift")
                            .withStyle(ChatFormatting.DARK_PURPLE), true);
                }
            }
        }
    }

    /** 附近找一个没有裂隙、可站立两格空间的落点。 */
    @Nullable
    private Vec3 findClearSpot(ServerLevel level, Vec3 from) {
        RandomSource r = level.getRandom();
        for (int i = 0; i < 12; i++) {
            double a = r.nextDouble() * Math.PI * 2.0D;
            double d = 9.0D + r.nextDouble() * 7.0D;
            double x = from.x + Math.cos(a) * d;
            double z = from.z + Math.sin(a) * d;
            int y = (int) Math.floor(from.y);
            int low = Math.max(y - 12, level.getMinBuildHeight() + 1);
            int high = Math.min(y + 6, level.getMaxBuildHeight() - 4);
            for (int yy = high; yy >= low; yy--) {
                BlockPos feet = BlockPos.containing(x, yy, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                    AABB probe = new AABB(feet).inflate(4.0D);
                    if (level.getEntitiesOfClass(VoidRiftEntity.class, probe).isEmpty()
                            && level.getEntitiesOfClass(SpikeEntity.class, probe).isEmpty()) {
                        return new Vec3(x, yy, z);
                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isPickable() {
        return false;
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
