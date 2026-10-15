package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * C2S：客户端左键准心拾取到目标后请求锁定。
 * 服务端校验：主手持丰饶、目标是敌对生物、存活、距离 ≤24 格。
 */
public record LockTargetPacket(int entityId) {

    public static void encode(LockTargetPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static LockTargetPacket decode(FriendlyByteBuf buf) {
        return new LockTargetPacket(buf.readVarInt());
    }
}
