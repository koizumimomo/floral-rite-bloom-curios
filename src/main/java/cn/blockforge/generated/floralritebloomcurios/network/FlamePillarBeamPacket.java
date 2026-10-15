package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * r96 S2C：光辉炎柱的信标光柱渲染指令。
 *
 * 在 (x, y, z) 处渲染一根向上延伸的橙色光柱（复用原版 beacon_beam.png 贴图，
 * 两个垂直交叉的半透明 quad），高度 height、宽度 radius×2，持续 duration tick。
 * 客户端由 FRBClient 存储、FRBWorldRender 渲染，过期自动清理。
 */
public record FlamePillarBeamPacket(
        double x, double y, double z,
        float radius, float height, int duration) {

    public static void encode(FlamePillarBeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeDouble(pkt.x());
        buf.writeDouble(pkt.y());
        buf.writeDouble(pkt.z());
        buf.writeFloat(pkt.radius());
        buf.writeFloat(pkt.height());
        buf.writeVarInt(pkt.duration());
    }

    public static FlamePillarBeamPacket decode(FriendlyByteBuf buf) {
        return new FlamePillarBeamPacket(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }
}
