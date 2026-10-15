package cn.blockforge.generated.floralritebloomcurios.charm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * r77：花灵"可切换的光环 / 主动效果"统一接口。
 *
 * <p>用途：玩家手持花灵饰品 shift + 右键能切换某些被动效果（例如黑灵草的"黑死病光环"）。
 * CharmEffect 上加新机制时直接 {@code CharmTools.toggleAura(player, type)} 即可，
 * 不需要单独给每种效果写开关 / lang / Tag 命名——按 CharmType 维度统一管理。</p>
 *
 * <p>存储：每种花灵的开关位存在玩家 persistentData 的 {@code "FRBCharmAuraOff"} int bitmask。
 * 默认开启（bitmask = 0 = 全开），玩家关闭某一种时把对应 bit 置 1。</p>
 */
public final class CharmTools {

    private static final String TAG_BITMASK = "FRBCharmAuraOff";

    private CharmTools() {}

    /** 该花灵的光环是否启用（默认 true）。 */
    public static boolean isAuraEnabled(Player player, CharmType type) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(TAG_BITMASK)) return true;
        int mask = root.getInt(TAG_BITMASK);
        return (mask & type.bit()) == 0;
    }

    /** 切换花灵光环；返回切换后的状态（true = 启用）。 */
    public static boolean toggleAura(Player player, CharmType type) {
        CompoundTag root = player.getPersistentData();
        int mask = root.contains(TAG_BITMASK) ? root.getInt(TAG_BITMASK) : 0;
        boolean enabled = (mask & type.bit()) != 0;
        if (enabled) {
            mask &= ~type.bit();
        } else {
            mask |= type.bit();
        }
        root.putInt(TAG_BITMASK, mask);
        return !enabled;
    }

    /** 给定 CharmType 的 bit 偏移（按 ALL 序号算）。 */
    public static int bit(CharmType type) {
        int i = 0;
        for (CharmType t : CharmType.ALL) {
            if (t == type) return 1 << i;
            i++;
        }
        return 0;
    }
}