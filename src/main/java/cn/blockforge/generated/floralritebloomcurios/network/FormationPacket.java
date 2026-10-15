package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/** S2C：在地面绘制法阵。kind: 0 仪式绿环 / 1 冰刺法阵 / 2 虚空裂缝。 */
public record FormationPacket(double x, double y, double z, int kind) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(kind);
    }

    public static FormationPacket decode(FriendlyByteBuf buf) {
        return new FormationPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
    }
}
