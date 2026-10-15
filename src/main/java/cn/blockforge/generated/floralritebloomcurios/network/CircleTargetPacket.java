package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：地面预警圈（FX_CIRCLE）。
 *
 * 在 (x, y, z) 处显示半径 radius 的预警环，持续 duration tick，
 * 颜色与样式按 kind 区分（0=红圈警戒 / 1=冰蓝 / 2=橙色冲击）。
 * 用于 B2 花之摇篮、C1 灼热之种、D2 霜刺等地面的"预警 → 结算"模式。
 */
public record CircleTargetPacket(int entityId, double x, double y, double z,
                                 double radius, int duration, int kind) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(radius);
        buf.writeVarInt(duration);
        buf.writeVarInt(kind);
    }

    public static CircleTargetPacket decode(FriendlyByteBuf buf) {
        return new CircleTargetPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt());
    }
}