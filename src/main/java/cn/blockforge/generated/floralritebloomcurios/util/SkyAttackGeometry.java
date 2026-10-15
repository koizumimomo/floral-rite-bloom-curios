package cn.blockforge.generated.floralritebloomcurios.util;

/**
 * 天空机制的纯几何工具，不自行生成实体、不改变 Boss 排程。
 * 所有横向判定采用玩家脚底中心，不额外膨胀玩家碰撞箱，使预警边界和判定一致。
 * 高度取玩家碰撞箱与指定伤害高度带的交集；伤害必须由服务端另行执行。
 */
public final class SkyAttackGeometry {
    private SkyAttackGeometry() {}

    /** 危险边界包含内圈与外圈；风眼内部安全。 */
    public static boolean inAnnulus(double px, double pz, double cx, double cz,
                                    double innerRadius, double outerRadius) {
        if (!Double.isFinite(innerRadius) || !Double.isFinite(outerRadius)
                || innerRadius < 0 || outerRadius <= innerRadius) {
            throw new IllegalArgumentException("月环半径必须满足 0 <= 内半径 < 外半径");
        }
        double dx = px - cx;
        double dz = pz - cz;
        double distanceSquared = dx * dx + dz * dz;
        return distanceSquared >= innerRadius * innerRadius
                && distanceSquared <= outerRadius * outerRadius;
    }

    /** r90：圆形判定——玩家在圆心 radius 范围内受伤。 */
    public static boolean inCircle(double px, double pz, double cx, double cz, double radius) {
        if (!Double.isFinite(radius) || radius <= 0) {
            throw new IllegalArgumentException("圆形半径必须为正数");
        }
        double dx = px - cx;
        double dz = pz - cz;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static boolean overlapsHeight(double minY, double maxY,
                                         double groundY, double attackHeight) {
        return attackHeight > 0 && maxY >= groundY && minY <= groundY + attackHeight;
    }

    /** 一条不可变的有限长、平头矩形路线；不采用带圆形端帽的胶囊判定。 */
    public record Lane(double startX, double startZ, double endX, double endZ, double halfWidth) {
        public Lane {
            if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                    || !Double.isFinite(endX) || !Double.isFinite(endZ)
                    || !Double.isFinite(halfWidth) || halfWidth <= 0
                    || Math.hypot(endX - startX, endZ - startZ) < 1.0E-6) {
                throw new IllegalArgumentException("冲锋路线必须为有限坐标、非零长度与正半宽");
            }
        }

        public double length() { return Math.hypot(endX - startX, endZ - startZ); }
        public double dirX() { return (endX - startX) / length(); }
        public double dirZ() { return (endZ - startZ) / length(); }
        public double normalX() { return -dirZ(); }
        public double normalZ() { return dirX(); }
        public double xAt(double progress) { return startX + (endX - startX) * clamp01(progress); }
        public double zAt(double progress) { return startZ + (endZ - startZ) * clamp01(progress); }

        /**
         * 检测本 tick 扫过的横向带。previous/current 是路线进度，范围 0..1。
         * frontHalfLength 是模型前后的伤害半长；起终点始终截断到完整预警矩形内。
         * 一次冲锋只打一次的去重集合由调用者维护。
         */
        public boolean containsSweep(double px, double pz, double previous, double current,
                                     double frontHalfLength) {
            if (!Double.isFinite(previous) || !Double.isFinite(current)
                    || !Double.isFinite(frontHalfLength) || frontHalfLength < 0) {
                throw new IllegalArgumentException("冲锋进度与伤害半长必须有效");
            }
            double length = length();
            double dx = (endX - startX) / length;
            double dz = (endZ - startZ) / length;
            double rx = px - startX;
            double rz = pz - startZ;
            double forward = rx * dx + rz * dz;
            double sideways = -rx * dz + rz * dx;
            double lo = Math.max(0, Math.min(clamp01(previous), clamp01(current)) * length - frontHalfLength);
            double hi = Math.min(length, Math.max(clamp01(previous), clamp01(current)) * length + frontHalfLength);
            return forward >= lo && forward <= hi && Math.abs(sideways) <= halfWidth;
        }

        public boolean contains(double px, double pz) {
            return containsSweep(px, pz, 0, 1, 0);
        }
    }

    /** 到达发动时刻为 0，到达结束时刻为 1；所有时刻使用服务端游戏 tick。 */
    public static double progress(long now, long launchTick, int dashTicks) {
        if (dashTicks <= 0) throw new IllegalArgumentException("冲锋持续时间必须大于 0");
        return clamp01((double) (now - launchTick) / dashTicks);
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
