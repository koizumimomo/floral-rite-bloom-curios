package cn.blockforge.generated.floralritebloomcurios.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 自定义渲染类型：继承 RenderType 以访问 protected 的 RenderStateShard。
 * 神环用：实体半透明自发光 + 开启深度写入。原版 entityTranslucentEmissive 不写深度，
 * 导致更远的生物和后绘制的云都会盖在环上；写深度后按真实距离正确遮挡。
 */
public final class FRBRenderTypes extends RenderType {

    private FRBRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState,
                           Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType backRing(ResourceLocation texture) {
        return RenderType.create("frb_back_ring", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        // r69 修复：必须声明 LIGHTMAP state 绑定光照贴图纹理，否则 NEW_ENTITY
                        // shader 的 Sampler2 残留上一帧绑定的其它纹理（如伤害指示红染）
                        .setLightmapState(LIGHTMAP)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        // r90：双面渲染，身后视角也能看到神环
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false));
    }

    /**
     * 通用发光贴图：实体半透明自发光（忽略光照贴图，黑暗中也发亮）+ 深度写入 + 双面。
     * 花之摇篮法阵 / 落花等 Boss 机制特效用。使用时缓存为 static final 以合批。
     */
    public static RenderType emissiveGlow(ResourceLocation texture) {
        return RenderType.create("frb_emissive_glow", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        // r69 修复：声明 LIGHTMAP state；配合调用处传 FULL_BRIGHT 实现"黑暗中也发亮"，
                        // 缺此 state 时 uv2 采样残留纹理导致法阵被染红（伤害指示粒子纹理）
                        .setLightmapState(LIGHTMAP)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false));
    }
}
