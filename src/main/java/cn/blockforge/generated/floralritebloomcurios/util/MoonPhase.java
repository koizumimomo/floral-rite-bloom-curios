package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.world.level.Level;

/**
 * r74：月相计算——vanilla MC 用 (dayTime / 24000) % 8 表示 0..7 的月相。
 *
 * - 0 = 满月
 * - 4 = 新月
 * - 数字越大月越缺（0 → 7 → 0）
 * - 我们以"月相越接近满月，成功率越高"的语义，线性映射为 5%~50% 概率。
 */
public final class MoonPhase {

    /** 0..7 月相（0=满月，4=新月）。 */
    public static int current(long dayTime) {
        return (int) ((dayTime / 24000L) % 8L);
    }

    /** 0..1 月相补足率（1.0 = 满月，0.0 = 新月）。 */
    public static float fullness(long dayTime) {
        int phase = current(dayTime);
        // 0..4 → 1.0..0.0（满月→新月），4..7 → 0.0..1.0（新月→满月）
        int dist = Math.min(phase, 8 - phase); // 0..4
        return 1.0F - dist / 4.0F;
    }

    /** 当前月相的成功率：5%（新月）~ 50%（满月）。 */
    public static float successChance(long dayTime) {
        return 0.05F + fullness(dayTime) * 0.45F;
    }

    /** 便捷重载。 */
    public static float successChance(Level level) {
        return successChance(level.getDayTime());
    }

    private MoonPhase() {}
}