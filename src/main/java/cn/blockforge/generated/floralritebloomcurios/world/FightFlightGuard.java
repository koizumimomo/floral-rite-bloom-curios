package cn.blockforge.generated.floralritebloomcurios.world;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * r75：boss 战禁飞。
 *
 * <p>用户截图 #11 反馈：当玩家在永恒花园挑战 boss 时若试图飞行 / 骑乘神鸟被打飞后，
 * 会强制把空中玩家拽回最近的地面，并显示"汝不可于此亵渎天空！"。</p>
 *
 * <p>判定条件（每 4 tick 扫描一次，避免每 tick AABB 探测）：</p>
 * <ol>
 *   <li>玩家位于永恒花园维度；</li>
 *   <li>玩家不在创造 / 旁观模式；</li>
 *   <li>玩家处于飞行状态（abilities.flying）或脚下 6 格内无实心方块（被弹飞/弹起）；</li>
 *   <li>领域 96 格内至少有一只存活的芙莉艾丝 boss。</li>
 * </ol>
 *
 * <p>处置：把玩家 Y 坐标定位到脚下 5 格内最高实心方块上方 +0.1，给一句 actionbar 提示（每 80 tick 重复一次，避免刷屏）。</p>
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FightFlightGuard {

    /** 玩家最近一次显示禁飞提示的 tick（key=玩家 UUID）。 */
    private static final Map<UUID, Long> LAST_WARN_TICK = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (!player.level().dimension().equals(EternalGarden.DIMENSION_KEY)) return;
        if (player.isCreative() || player.isSpectator()) return;
        // 扫描频率：每 4 tick 才检查一次（不每 tick AABB），降低守护开销。
        if (player.tickCount % 4 != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        // 找最近活 boss（96 格）
        FriliasEntity nearestBoss = null;
        double bestDist = 96.0D * 96.0D;
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof FriliasEntity boss) || !boss.isAlive()) continue;
            double d = entity.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                nearestBoss = boss;
            }
        }
        if (nearestBoss == null) return;

        // 判定是否需要拉回地面：
        //   - 玩家自身在飞行模式（开了创造飞行 / 装鞘翅飞行 / 末影龙翅膀等任何飞行源）
        //   - 或脚下 6 格内无实心方块（被打飞 / 弹起 / 跳起时）
        boolean flying = player.getAbilities().flying;
        boolean airborne = !isOnGroundNearby(player);
        if (!flying && !airborne) return;

        // 把玩家拽到脚下 5 格内最高实心方块上方
        double targetY = findGroundY(level, player);
        if (targetY > Double.NEGATIVE_INFINITY) {
            player.setPos(player.getX(), targetY + 0.1D, player.getZ());
            // 同时按住跳跃的纵向速度清零，避免再弹起
            player.setDeltaMovement(player.getDeltaMovement().x * 0.5D, 0.0D, player.getDeltaMovement().z * 0.5D);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
        }

        // 限频提示：每 80 tick 一次
        long now = level.getServer().getTickCount();
        Long last = LAST_WARN_TICK.get(player.getUUID());
        if (last == null || now - last >= 80) {
            LAST_WARN_TICK.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("frb.fight.no_fly")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** 玩家脚下 6 格内是否有可承重方块。 */
    private static boolean isOnGroundNearby(Player player) {
        var level = player.level();
        BlockPos feet = player.blockPosition();
        for (int dy = 0; dy <= 6; dy++) {
            BlockState state = level.getBlockState(feet.below(dy));
            // 不算叶子 / 空气 / 红石线等不可承重方块
            if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
            if (!state.getCollisionShape(level, feet.below(dy)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 玩家脚下往下最多 5 格内寻找最高实心方块顶面 Y（Double.NEGATIVE_INFINITY 表示没找到）。 */
    private static double findGroundY(ServerLevel level, Player player) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        m.set(player.getX(), player.getY() - 0.1D, player.getZ());
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos p = m.set(player.getX(), player.getY() - dy, player.getZ());
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && !state.is(BlockTags.LEAVES)
                    && !state.getCollisionShape(level, p).isEmpty()) {
                return p.getY() + 1.0D;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    /** 玩家退出战斗后清理记录。 */
    public static void cleanup(UUID id) {
        LAST_WARN_TICK.remove(id);
    }

    private FightFlightGuard() {}
}