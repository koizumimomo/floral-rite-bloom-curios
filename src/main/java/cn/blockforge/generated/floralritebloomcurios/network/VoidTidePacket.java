package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * r90 S2C：虚空潮汐 / 时间潮汐 / 钢铁月环 / 光辉炎柱 地面贴图预警。
 *
 * kind=0 虚空环形（void_aoe_ring.png）：外半径 radius，内安全区 = radius * 8/18。
 * kind=1 虚空圆形（void_aoe_circle.png）：半径 radius。
 * kind=2 时间环形（time_aoe_ring.png）：外半径 radius，内安全区 = radius * 8/18。
 * kind=3 时间圆形（time_aoe_circle.png）：半径 radius。
 * kind=4 钢铁月环 环形（frost_moon_ring.png）：外半径 radius，内安全区 = radius * 8/18。
 * kind=5 钢铁月环 圆形（frost_moon_circle.png）：半径 radius。
 * kind=6 光辉炎柱 预警圆（flame_pillar_circle.png）：半径 radius（仅预警，结算后由粒子呈现炎柱）。
 * kind=7 万花凋零 魔法阵（mille_florum.png）：半径 radius。
 *
 * 在 (x, y, z) 处绘制贴地 XZ 平面贴图，持续 duration tick。
 */
public record VoidTidePacket(double x, double y, double z, float radius, int duration, int kind) {

    public static final int KIND_VOID_RING = 0;
    public static final int KIND_VOID_CIRCLE = 1;
    public static final int KIND_TIME_RING = 2;
    public static final int KIND_TIME_CIRCLE = 3;
    /** r96：钢铁月环 环形（frost_moon_ring.png）。 */
    public static final int KIND_FROST_MOON_RING = 4;
    /** r96：钢铁月环 圆形（frost_moon_circle.png）。 */
    public static final int KIND_FROST_MOON_CIRCLE = 5;
    /** r96：光辉炎柱 预警圆（flame_pillar_circle.png）。 */
    public static final int KIND_FLAME_PILLAR_CIRCLE = 6;
    /** r107：万花凋零终结技 魔法阵（mille_florum.png）。 */
    public static final int KIND_MILLE_FLORUM = 7;
    // 兼容旧引用
    public static final int KIND_RING = 0;
    public static final int KIND_CIRCLE = 1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(radius);
        buf.writeVarInt(duration);
        buf.writeVarInt(kind);
    }

    public static VoidTidePacket decode(FriendlyByteBuf buf) {
        return new VoidTidePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }
}
