package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * S2C：世界空间后处理效果（冲击波 / 虫洞引力透镜等）。
 *
 * name 支持 "Shockwave"（波纹扩散）与 "Wormhole"（虫洞镜头变形）。
 * 客户端收到后加入 FRBShaderEffects.EFFECTS，由 RenderLevelStageEvent.AFTER_LEVEL 每帧应用。
 */
public record ShaderEffectPacket(String dimension, String name,
                                 double x, double y, double z,
                                 int lifeTime, float scale) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeUtf(name);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(lifeTime);
        buf.writeFloat(scale);
    }

    public static ShaderEffectPacket decode(FriendlyByteBuf buf) {
        return new ShaderEffectPacket(
                buf.readUtf(), buf.readUtf(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readFloat());
    }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(dimension));
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }
}
