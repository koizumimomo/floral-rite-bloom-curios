package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/** S2C：阶段演出（-1 表示 Boss 苏醒公告）。 */
public record PhaseTitlePacket(int entityId, int phase) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(phase);
    }

    public static PhaseTitlePacket decode(FriendlyByteBuf buf) {
        return new PhaseTitlePacket(buf.readVarInt(), buf.readVarInt());
    }
}
