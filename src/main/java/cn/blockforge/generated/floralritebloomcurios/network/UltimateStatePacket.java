package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：服务端权威的【万花万草之灵】佩戴状态。
 * <p>
 * 命令式跨维度传送（{@code teleportTo(ServerLevel, ...)}）后，Curios 的客户端同步可能滞后，
 * 客户端会短暂（甚至持续）认为饰品不在槽位里，导致光环渲染、二段跳等客户端判定失效。
 * 服务端每 20 tick 比对一次佩戴状态，变化时下发本包；客户端把该值作为兜底。
 */
public record UltimateStatePacket(boolean worn) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(worn);
    }

    public static UltimateStatePacket decode(FriendlyByteBuf buf) {
        return new UltimateStatePacket(buf.readBoolean());
    }
}
