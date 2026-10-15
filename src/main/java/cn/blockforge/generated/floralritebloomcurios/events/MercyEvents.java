package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * r87 芙莉艾丝的仁慈：
 *
 * Boss 战中，玩家承受的某次伤害若足以致命，则取消该次伤害、保留 1 点生命，
 * 并将玩家送回自己的重生点（无重生点则世界出生点），附 60 tick（3s）无敌帧
 * 防止同 tick 后续伤害补刀。
 *
 * 不触发的情况：
 *  - 创造 / 旁观模式；
 *  - 双手持有不死图腾（交由原版图腾流程处理，不抢救命）；
 *  - 虚空伤害 / /kill 等 BYPASSES_INVULNERABILITY 绝对伤害；
 *  - 附近 64 格内没有存活的芙莉艾丝（非本 boss 战斗）；
 *  - 非致命伤害。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class MercyEvents {

    /** 判定"处于 boss 战斗中"的搜索半径（与 boss 追踪范围 64 一致）。 */
    private static final double FIGHT_RADIUS = 64.0D;
    /** 救人后的无敌帧（tick），防止同 tick 多段伤害 / 落地摔 / 残留火伤补刀。 */
    private static final int RESCUE_INVULN_TICKS = 60;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        // 仅生存/冒险；创造/旁观本来就不会死
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return;
        // LivingDamageEvent 的 amount 已扣过护甲/抗性/吸收心，>= 当前血量即致命
        if (event.getAmount() < player.getHealth()) return;
        // 不死图腾优先：双手任一持有图腾则放行，由原版消耗图腾保命
        if (hasTotem(player)) return;
        // 虚空、/kill 等绝对伤害不拦截
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ServerLevel level = (ServerLevel) player.level();
        // 必须处于芙莉艾丝战斗中
        java.util.List<FriliasEntity> bosses = level.getEntitiesOfClass(FriliasEntity.class,
                player.getBoundingBox().inflate(FIGHT_RADIUS),
                b -> b.isAlive() && !b.isRemoved());
        if (bosses.isEmpty()) return;

        // ---- 触发仁慈：取消致命伤害 ----
        event.setCanceled(true);
        player.setHealth(1.0F);
        player.invulnerableTime = RESCUE_INVULN_TICKS;
        player.clearFire(); // 火伤会绕过 1HP 仁慈继续致命
        player.removeEffect(net.minecraft.world.effect.MobEffects.WITHER); // 凋零跳伤同样会致命
        player.setDeltaMovement(Vec3.ZERO);

        // 原位置留下一片微粒，再传送
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.8D, player.getZ(), 24,
                0.4D, 0.6D, 0.4D, 0.05D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.2F, 1.5F);

        teleportToRespawn(player);

        ServerLevel dest = (ServerLevel) player.level();
        dest.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.8D, player.getZ(), 24,
                0.4D, 0.6D, 0.4D, 0.05D);
        dest.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.6F);
        player.displayClientMessage(Component.translatable("frb.mercy.saved")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        // r93：boss 对被救回出生点的玩家补一句道别（聊天栏）
        bosses.get(0).frbSayTo(player, "frb.boss.say.mercy");
    }

    private static boolean hasTotem(ServerPlayer player) {
        return player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    /** 送回玩家重生点（床/重生锚），失效则世界出生点；坐标不安全时上下搜索或回退出生点。 */
    private static void teleportToRespawn(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel target;
        BlockPos anchor;
        float yaw;

        BlockPos respawn = player.getRespawnPosition();
        ServerLevel respawnLevel = respawn == null ? null
                : server.getLevel(player.getRespawnDimension());
        if (respawn != null && respawnLevel != null) {
            Vec3 safe = findStandPosition(respawnLevel, Vec3.atBottomCenterOf(respawn));
            if (safe != null) {
                player.teleportTo(respawnLevel, safe.x, safe.y, safe.z,
                        Set.of(RelativeMovement.X_ROT), player.getRespawnAngle(), player.getXRot());
                return;
            }
            target = respawnLevel;
            anchor = respawn;
            yaw = player.getRespawnAngle();
        } else {
            target = server.overworld();
            anchor = target.getSharedSpawnPos();
            yaw = target.getSharedSpawnAngle();
        }
        // 重生点失效/被堵 → 世界出生点
        ServerLevel overworld = server.overworld();
        Vec3 fallback = findStandPosition(target, Vec3.atBottomCenterOf(anchor));
        if (fallback == null) {
            target = overworld;
            anchor = overworld.getSharedSpawnPos();
            yaw = overworld.getSharedSpawnAngle();
            fallback = findStandPosition(overworld, Vec3.atBottomCenterOf(anchor));
        }
        if (fallback == null) {
            fallback = new Vec3(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        }
        player.teleportTo(target, fallback.x, fallback.y, fallback.z,
                Set.of(RelativeMovement.X_ROT), yaw, player.getXRot());
    }

    /** 以给定位置为中心，Y 方向 -3 ~ +3 搜索一个可站立、头部有空间的坐标；找不到返回 null。 */
    @Nullable
    private static Vec3 findStandPosition(ServerLevel level, Vec3 pos) {
        AABB stand = playerBox(pos);
        if (level.noCollision(stand)) return pos;
        for (int dy = 1; dy <= 3; dy++) {
            Vec3 up = pos.add(0.0D, dy, 0.0D);
            if (level.noCollision(playerBox(up))) return up;
            Vec3 down = pos.add(0.0D, -dy, 0.0D);
            if (level.noCollision(playerBox(down))) return down;
        }
        return null;
    }

    private static AABB playerBox(Vec3 pos) {
        return new AABB(pos.x - 0.3D, pos.y, pos.z - 0.3D,
                pos.x + 0.3D, pos.y + 1.8D, pos.z + 0.3D);
    }

    private MercyEvents() {}
}
