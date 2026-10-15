package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：Boss 万花凋零咏唱中屏幕文本 + 倒计时。
 *
 * lines = 客户端要显示的两行字符串（首行咏唱名，末行倒计时）。
 * remainingTicks = 剩余 tick（客户端转换为整数秒显示）。
 */
public record FinaleTextPacket(int entityId, String line1, String line2, int remainingTicks) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(line1);
        buf.writeUtf(line2);
        buf.writeVarInt(remainingTicks);
    }

    public static FinaleTextPacket decode(FriendlyByteBuf buf) {
        return new FinaleTextPacket(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }
}