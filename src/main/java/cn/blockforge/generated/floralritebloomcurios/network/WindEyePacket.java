package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * r86 S2C：风眼月环地面贴图预警。
 *
 * 在 (x, y, z) 处绘制半径 outerRadius 的 wind_eye_4_18.png 贴图（贴地 XZ 平面），
 * 持续 duration tick。内安全区 = outerRadius * 4/18，外危险区 = outerRadius。
 * 用于天空阶段月环机制：玩家在环带（内 R ~ 外 R）内将受伤，需靠近 boss 躲入风眼。
 */
public record WindEyePacket(double x, double y, double z, float outerRadius, int duration) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(outerRadius);
        buf.writeVarInt(duration);
    }

    public static WindEyePacket decode(FriendlyByteBuf buf) {
        return new WindEyePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readVarInt());
    }
}
