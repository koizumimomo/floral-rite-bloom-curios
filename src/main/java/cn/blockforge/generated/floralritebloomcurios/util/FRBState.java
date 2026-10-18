package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 佩戴检测与玩家持久状态（七花收集进度、快照、各类冷却）。
 */
public final class FRBState {
    public static final String TAG = "FloralRiteBloom";
    public static final String SLOT_CHARM = "flower_charm";
    public static final String SLOT_ETERNAL = "flower_eternal";

    // ---- 佩戴检测 ----

    /**
     * 该物品是否佩戴在实体的花仪 / 永恒槽位中（含其它 curios 槽，统一判定）。
     * <p>
     * 每次直接查询 Curios，与诡厄巫法启示录的
     * {@code CuriosFinder.findCurio} / {@code ATAHelper.hasHalo} 一致，不做任何缓存。
     * 之前为减少重复查询加过缓存，但 Curios 在登录 / 切换维度后的同步时序并不可靠：
     * 只要在同步窗口内写进过一次 false，终极饰品就会持续判定为"未携带"，直到重新装备。
     * 参考实现的晋升之环每次使用能力都现查，这里也改成现查。
     */
    public static boolean wearing(LivingEntity entity, Item item) {
        if (entity == null || item == null) return false;
        return findEquipped(entity, item).isPresent();
    }

    /** 扫描全部 curios 槽寻找匹配物品（findFirstCurio 比 isEquipped 更可靠）。 */
    private static java.util.Optional<ItemStack> findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(h -> h.findFirstCurio(stack -> stack.is(item)))
                .map(top.theillusivec4.curios.api.SlotResult::stack);
    }

    /**
     * 命令式跨维度传送（{@code teleportTo(ServerLevel, ...)}）和传送门走的不是同一条路径：
     * 传送门路径会在新维度里重发能力与全部药效，命令路径不会。客户端重建 LocalPlayer 后
     * 能力和药效列表是空的，而服务端又因为"自己这边还有"不会重新下发，表现就是
     * 进了永恒花园后夜视、水下呼吸、飞行全部消失（但服务端数据和饰品都还在）。
     * 这里手动补发一次。药效必须在 Respawn 包之后发送，所以调用点要放在 teleportTo 之后。
     */
    public static void resendClientState(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(
                player.getAbilities()));
        for (net.minecraft.world.effect.MobEffectInstance effect : player.getActiveEffects()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(
                    player.getId(), effect));
        }
    }

    /** 槽位内佩戴该饰品时为 true（只统计指定花仪槽，slotId 为 curios 槽名）。 */
    public static boolean wearingIn(LivingEntity entity, Item item, String slotId) {
        return CuriosApi.getCuriosInventory(entity).map(h ->
                h.findCurios(slotId).stream().anyMatch(r -> r.stack().is(item))).orElse(false);
    }

    public static boolean hasAllSeven(Player player) {
        for (CharmType type : CharmType.ALL) {
            if (!wearing(player, FRBItems.charmOf(type))) return false;
        }
        return true;
    }

    /**
     * 客户端由服务端下发的权威状态兜底：命令式跨维度传送后 Curios 的客户端同步可能滞后，
     * 光环渲染 / 二段跳等客户端判定会误以为没戴。服务端每 20 tick 比对并下发
     * {@link cn.blockforge.generated.floralritebloomcurios.network.UltimateStatePacket}。
     */
    private static volatile boolean clientUltimateWorn;

    public static void setClientUltimateWorn(boolean worn) {
        clientUltimateWorn = worn;
    }

    public static boolean hasUltimate(Player player) {
        boolean worn = wearing(player, FRBItems.ETERNAL_SPIRIT.get());
        if (!worn && player.level() != null && player.level().isClientSide) {
            return clientUltimateWorn;
        }
        return worn;
    }

    /**
     * 是否携带「已解封的丰饶」（BountifulItem + BountifulRites.TAG_UNSEALED）。
     * 用于时间阶段触发判定：枯萎结束后先检查玩家是否持有本项或「万花万草之灵」，
     * 任一为真则跳过万花凋零直接进入额外阶段·时间。
     */
    public static boolean holdingUnsealedBountiful(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(FRBItems.BOUNTIFUL.get())
                    && cn.blockforge.generated.floralritebloomcurios.util.BountifulRites.isUnsealed(stack)) {
                return true;
            }
        }
        // 也兼容 curios 槽（丰饶是手持主手物品为主，但保险起见扫一次花仪槽）
        return CuriosApi.getCuriosInventory(player).map(h ->
                h.findCurios(SLOT_CHARM).stream().anyMatch(r ->
                        r.stack().is(FRBItems.BOUNTIFUL.get())
                                && cn.blockforge.generated.floralritebloomcurios.util.BountifulRites
                                .isUnsealed(r.stack()))).orElse(false);
    }

    // ---- Boss 封印（阶段结束后对应花灵被封印，击杀 Boss 后复原） ----

    public static final String TAG_SEALED = "FRBSealed";
    public static final String TAG_GROWTH_ENABLED = "FRBGrowthEnabled";
    public static final String TAG_DOUBLE_JUMP_USED = "FRBDoubleJumpUsed";
    public static final String TAG_AIR_TICKS = "FRBAirTicks";
    /** 终极饰品副手 shift 右键：单独关闭聚合出的寒风铃传送回溯。 */
    public static final String TAG_ULT_RECALL_OFF = "FRBUltRecallOff";

    /** 森语藤催熟默认关闭；玩家可手持饰品蹲下右键切换。 */
    public static boolean growthEnabled(Player player) {
        CompoundTag root = player.getPersistentData();
        return root.contains(TAG_GROWTH_ENABLED) && root.getBoolean(TAG_GROWTH_ENABLED);
    }

    public static boolean toggleGrowth(Player player) {
        boolean enabled = !growthEnabled(player);
        player.getPersistentData().putBoolean(TAG_GROWTH_ENABLED, enabled);
        return enabled;
    }

    /** 读取判定必须是纯读：getOrCreateTag() 会给无 NBT 的堆叠凭空挂上标签，
     *  在服务端每 tick 的佩戴扫描里造成不必要的堆叠改动（同步风暴源头之一）。 */
    private static boolean isSealedStack(Item stackInSlot, net.minecraft.world.item.ItemStack stack) {
        return stack.is(stackInSlot) && stack.hasTag() && stack.getTag().getBoolean(TAG_SEALED);
    }

    /** 封印指定花灵饰品，兼容已放入 Curios 和暂存在玩家背包中的饰品。 */
    public static int sealCharm(Player player, CharmType type) {
        Item charm = FRBItems.charmOf(type);
        int[] n = {0};
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findCurios(SLOT_CHARM).forEach(r -> n[0] += markSeal(r.stack(), charm)));
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            n[0] += markSeal(player.getInventory().getItem(i), charm);
        }
        return n[0];
    }

    private static int markSeal(net.minecraft.world.item.ItemStack stack, Item charm) {
        if (!stack.is(charm) || (stack.hasTag() && stack.getTag().getBoolean(TAG_SEALED))) return 0;
        stack.getOrCreateTag().putBoolean(TAG_SEALED, true);
        return 1;
    }

    /** 解除玩家身上全部花仪封印，包含 Curios 槽和背包中已被标记的饰品。 */
    public static int releaseAllSeals(Player player) {
        int[] n = {0};
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findCurios(SLOT_CHARM).forEach(r -> n[0] += clearSeal(r.stack())));
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            n[0] += clearSeal(player.getInventory().getItem(i));
        }
        if (n[0] > 0) {
            player.getInventory().setChanged();
        }
        return n[0];
    }

    private static int clearSeal(net.minecraft.world.item.ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().getBoolean(TAG_SEALED)) return 0;
        stack.getTag().remove(TAG_SEALED);
        return 1;
    }

    /** 花仪槽里该饰品是否处于被封印状态。 */
    public static boolean sealed(Player player, CharmType type) {
        Item charm = FRBItems.charmOf(type);
        return CuriosApi.getCuriosInventory(player).map(h ->
                h.findCurios(SLOT_CHARM).stream().anyMatch(r -> isSealedStack(charm, r.stack()))).orElse(false);
    }

    /** 终极饰品的寒风铃传送回溯是否开启（默认开启）。 */
    public static boolean ultimateRecallEnabled(Player player) {
        return !player.getPersistentData().getBoolean(TAG_ULT_RECALL_OFF);
    }

    public static boolean toggleUltimateRecall(Player player) {
        boolean enabled = !ultimateRecallEnabled(player);
        player.getPersistentData().putBoolean(TAG_ULT_RECALL_OFF, !enabled);
        return enabled;
    }

    /**
     * 花灵效果是否生效：佩戴且未被封印，或由终极饰品【万花万草之灵】聚合提供。
     * 所有饰品效果判定的唯一入口。（森语藤的催熟开关只作用于催熟本身，见 growthEnabled）
     */
    public static boolean active(Player player, CharmType type) {
        if (hasUltimate(player)) return true;
        return wearing(player, FRBItems.charmOf(type)) && !sealed(player, type);
    }

    /** 寒风铃的传送回溯是否可用：本体佩戴未被封印，或终极饰品提供且回溯未被关闭。 */
    public static boolean recallAllowed(Player player) {
        if (wearing(player, FRBItems.charmOf(CharmType.FROST_CHIME))
                && !sealed(player, CharmType.FROST_CHIME)) {
            return true;
        }
        return hasUltimate(player) && ultimateRecallEnabled(player);
    }

    /** 花仪槽内已佩戴的饰品数量（天泪花耐久回复量 = 1 + 该值）。 */
    public static int flowerCharmCount(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(h -> h.findCurios(SLOT_CHARM).size()).orElse(0);
    }

    // ---- 收集进度 ----

    public static void markCollected(Player player, CharmType type) {
        tag(player).putBoolean("Collected_" + type.id, true);
    }

    public static boolean collected(Player player, CharmType type) {
        return tag(player).getBoolean("Collected_" + type.id);
    }

    public static int collectedCount(Player player) {
        int n = 0;
        for (CharmType type : CharmType.ALL) {
            if (collected(player, type)) n++;
        }
        return n;
    }

    // ---- 通用数值存取 ----

    /** 天泪花每 2 秒的耐久回复量：1 + [群花]槽饰品数（无玩家上下文时按 1）。 */
    public static int repairAmount(@javax.annotation.Nullable Player player) {
        return player == null ? 1 : 1 + flowerCharmCount(player);
    }

    public static float getFloat(Player player, String key, float def) {
        CompoundTag t = tag(player);
        return t.contains(key) ? t.getFloat(key) : def;
    }

    public static void setFloat(Player player, String key, float value) {
        tag(player).putFloat(key, value);
    }

    public static long getLong(Player player, String key) {
        return tag(player).getLong(key);
    }

    public static void setLong(Player player, String key, long value) {
        tag(player).putLong(key, value);
    }

    /** 冷却：若当前游戏时间已过截止时间则返回 true 并续上新的冷却。 */
    public static boolean useCooldown(Player player, String key, long now, long cooldown) {
        long until = getLong(player, key);
        if (now >= until) {
            setLong(player, key, now + cooldown);
            return true;
        }
        return false;
    }

    public static long cooldownLeft(Player player, String key, long now) {
        return Math.max(0L, getLong(player, key) - now) / 20L;
    }

    private static CompoundTag tag(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag t = root.getCompound(TAG);
        root.put(TAG, t);
        return t;
    }

    private FRBState() {}
}
