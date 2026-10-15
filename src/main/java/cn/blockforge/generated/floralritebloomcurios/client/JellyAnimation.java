package cn.blockforge.generated.floralritebloomcurios.client;

/**
 * 果冻回弹数学（移植自 friendswine 模组的 JellyAnimation）。
 * <p>
 * 曲线由 5 个关键帧定义，帧间用 smoothstep（三次 Hermite）插值，
 * 一个周期（约 0.917 秒）内完成两次「压扁 → 还原」的回弹。
 * <p>
 * squash ∈ [0, 1]：0 为原形，1 为最扁。
 * heightScale = 1 - (compress / 100) * squash  （压扁时高度降低）
 * widthScale  = 1 + (width / 100) * squash     （压扁时横向变宽，保持体积近似守恒）
 * <p>
 * r110：时间单位统一为<b>秒</b>。旧版 {@code squash(float ticks)} 收的是 tick 并在内部 /20，
 * 与 friendswine 的秒制 API 混用极易出错（改旋转时就踩过），这里统一成秒。
 */
public final class JellyAnimation {

    /** 一个周期的长度（秒），11/12 ≈ 0.9167。 */
    public static final double LENGTH = 11.0 / 12.0;

    /** 关键帧时间点（秒）。 */
    private static final double[] TIMES = {0.0, 0.25, 11.0 / 24.0, 17.0 / 24.0, LENGTH};
    /** 关键帧 squash 值。 */
    private static final double[] VALUES = {0.0, 1.0, 0.0, 1.0, 0.0};

    /** 压扁强度（百分比），50 → 最大压缩到一半高度。 */
    public static final double COMPRESS = 50.0;
    /** 横向膨胀强度（百分比），35。 */
    public static final double WIDTH = 35.0;

    private JellyAnimation() {}

    /** 将时间映射到 [0, LENGTH) 的周期相位。 */
    public static double phase(double seconds) {
        return Math.max(0.0, seconds) % LENGTH;
    }

    /**
     * 旋转角度（度）。一个果冻周期恰好转一整圈，方向为负。
     * <p>
     * 与 {@link #squash(double)} 共用 {@link #phase(double)}，所以旋转和挤压天然锁相：
     * 每压扁一次正好转过一圈。
     *
     * @param seconds 动画已进行的秒数
     */
    public static float angle(double seconds) {
        return (float) (-360.0 * phase(seconds) / LENGTH);
    }

    /**
     * 计算当前 squash 值（0~1）。
     *
     * @param seconds 动画已进行的<b>秒</b>数（含 partialTick）
     */
    public static float squash(double seconds) {
        double t = phase(seconds);
        // 找到 t 所在的关键帧区间
        int i = 0;
        while (i < TIMES.length - 2 && t > TIMES[i + 1]) i++;
        // 区间内归一化位置
        double segT = (t - TIMES[i]) / (TIMES[i + 1] - TIMES[i]);
        // smoothstep：t²(3 - 2t)
        double smooth = segT * segT * (3.0 - 2.0 * segT);
        double v0 = VALUES[i];
        double v1 = VALUES[i + 1];
        return (float) (v0 + (v1 - v0) * smooth);
    }

    /** 便捷重载：直接由秒数算出高度缩放（用默认 COMPRESS）。 */
    public static float heightScale(double seconds) {
        return heightScale(squash(seconds), COMPRESS);
    }

    /** 便捷重载：直接由秒数算出宽度缩放（用默认 WIDTH）。 */
    public static float widthScale(double seconds) {
        return widthScale(squash(seconds), WIDTH);
    }

    /** 高度缩放：squash=1 时高度 = 1 - compress/100。 */
    public static float heightScale(float squash, double compress) {
        return (float) (1.0 - (compress / 100.0) * squash);
    }

    /** 宽度缩放：squash=1 时宽度 = 1 + width/100。 */
    public static float widthScale(float squash, double width) {
        return (float) (1.0 + (width / 100.0) * squash);
    }
}
