package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 佩戴检测与玩家持久状态（七花收集进度、快照、各类冷却）。
 */
public final class FRBState {
    public static final String TAG = "FloralRiteBloom";
    public static final String SLOT_CHARM = "flower_charm";
    public static final String SLOT_ETERNAL = "flower_eternal";

    // ---- 佩戴检测 ----

    /**
     * 佩戴缓存。判定方式对齐诡厄巫法启示录的 {@code CuriosFinder.findCurio}——
     * 那边每次使用能力都直接查 Curios，不缓存；这里保留缓存只是为了减少同一 tick 内的重复查询
     * （{@link #active} 每 tick 会被调用十几次），但必须满足两个条件：
     * <ul>
     *   <li>缓存键是「客户端/服务端 + 玩家 UUID + 具体饰品」：否则戴着森语藤时查焚生藤也会命中；
     *       单人游戏里客户端和服务端共用一个 JVM，不加 side 的话客户端同步窗口内的 false
     *       会污染服务端判定；</li>
     *   <li>缓存最多存活 {@value #WEAR_CACHE_TTL} tick：登录 / 切维度时 Curios 还没同步完，
     *       这一瞬间查到的 false 不能被长期记住（旧实现会几乎永久记住，导致切维度后
     *       "不被判定成携带状态"）。</li>
     * </ul>
     */
    private static final Map<WearKey, WearEntry> WEAR_CACHE = new ConcurrentHashMap<>();
    /** 佩戴缓存存活时间：20 tick = 1 秒。 */
    private static final int WEAR_CACHE_TTL = 20;

    private record WearKey(boolean clientSide, UUID player, Item item) {}
    private record WearEntry(boolean worn, int tick) {}

    /** 该物品是否佩戴在实体的花仪 / 永恒槽位中（含其它 curios 槽，统一判定）。 */
    public static boolean wearing(LivingEntity entity, Item item) {
        if (entity == null || item == null) return false;
        boolean clientSide = entity.level() != null && entity.level().isClientSide;
        WearKey key = new WearKey(clientSide, entity.getUUID(), item);
        WearEntry cached = WEAR_CACHE.get(key);
        int now = entity.tickCount;
        if (cached != null) {
            int age = now - cached.tick();
            // 客户端换维度后 tickCount 会从头计数，age 可能为负——同样按过期处理
            if (age >= 0 && age < WEAR_CACHE_TTL) return cached.worn();
        }
        boolean result = findEquipped(entity, item).isPresent();
        if (WEAR_CACHE.size() > 16384) WEAR_CACHE.clear();
        WEAR_CACHE.put(key, new WearEntry(result, now));
        return result;
    }

    /** 扫描全部 curios 槽寻找匹配物品（findFirstCurio 比 isEquipped 更可靠）。 */
    private static java.util.Optional<ItemStack> findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(h -> h.findFirstCurio(stack -> stack.is(item)))
                .map(top.theillusivec4.curios.api.SlotResult::stack);
    }

    /**
     * 使缓存失效（装备/卸下/登录/重生时调用）。
     * 切维度<b>不需要</b>调用：饰品仍然戴着，缓存本身有效；即使这一瞬间 Curios 未同步，
     * {@link #WEAR_CACHE_TTL} 后也会自动重查，不会像旧实现那样把 false 长期记住。
     */
    public static void invalidateWearCache(LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        WEAR_CACHE.keySet().removeIf(key -> key.player().equals(id));
    }

    public static void clearWearCache() {
        if (!WEAR_CACHE.isEmpty()) WEAR_CACHE.clear();
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

    public static boolean hasUltimate(Player player) {
        return wearing(player, FRBItems.ETERNAL_SPIRIT.get());
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
