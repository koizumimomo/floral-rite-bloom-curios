package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：玩家头顶标记（FX_MARKER）。
 * 客户端在指定 entityId 的玩家头顶 1.5 格处持续渲染指定贴图帧，
 * duration tick 后自动淡出。color 仅作为色彩提示用，目前未使用（贴图本身带颜色）。
 */
public record MarkerTargetPacket(int entityId, int duration, int color) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(duration);
        buf.writeVarInt(color);
    }

    public static MarkerTargetPacket decode(FriendlyByteBuf buf) {
        return new MarkerTargetPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}