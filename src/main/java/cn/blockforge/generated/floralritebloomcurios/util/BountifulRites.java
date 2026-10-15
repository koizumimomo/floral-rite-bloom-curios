package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 丰饶的「花灵印刻」：物品 NBT 记录已完成的七仪（每种花灵仪式各一次，防止重复强化）。
 * 存储结构：stack tag "FRBRites" = CompoundTag{ <charmId>: 1b, ... }；
 * 每印刻一仪，丰饶造成的最终伤害 +10%。
 */
public final class BountifulRites {

    public static final String TAG = "FRBRites";
    /** 解封标记：新凝结的丰饶处于封印态，需用「世界之花的意志」解封后才能印刻。 */
    public static final String TAG_UNSEALED = "FRBUnsealed";

    /** 是否已解封（未打标记 = 封印态）。 */
    public static boolean isUnsealed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_UNSEALED);
    }

    /** 解封（幂等）。 */
    public static void unseal(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(TAG_UNSEALED, true);
    }

    /** 已铭记的仪式数量（= 锻造等级，最大 7）。 */
    public static int level(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG)) return 0;
        CompoundTag rites = tag.getCompound(TAG);
        int n = 0;
        for (CharmType type : CharmType.ALL) {
            if (rites.getBoolean(type.id)) n++;
        }
        return n;
    }

    public static boolean has(ItemStack stack, CharmType type) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG) && tag.getCompound(TAG).getBoolean(type.id);
    }

    /** 印刻一仪（幂等）。 */
    public static void mark(ItemStack stack, CharmType type) {
        stack.getOrCreateTagElement(TAG).putBoolean(type.id, true);
    }

    /** 最终伤害倍率：1 + 10% × 已印刻数。 */
    public static float damageMultiplier(ItemStack stack) {
        return 1.0F + 0.1F * level(stack);
    }

    private BountifulRites() {}
}
