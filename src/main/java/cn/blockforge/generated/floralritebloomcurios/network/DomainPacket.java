package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/** S2C：终末花环领域爆发的视觉演出。 */
public record DomainPacket(double x, double y, double z, int color) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(color);
    }

    public static DomainPacket decode(FriendlyByteBuf buf) {
        return new DomainPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
    }
}
