package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * r90 虚空潮汐 / 时间潮汐 / 钢铁月环 / 光辉炎柱 地面贴图渲染器：
 * kind=0 虚空环形（void_aoe_ring.png）
 * kind=1 虚空圆形（void_aoe_circle.png）
 * kind=2 时间环形（time_aoe_ring.png）
 * kind=3 时间圆形（time_aoe_circle.png）
 * kind=4 钢铁月环 环形（frost_moon_ring.png）
 * kind=5 钢铁月环 圆形（frost_moon_circle.png）
 * kind=6 光辉炎柱 预警圆（flame_pillar_circle.png）
 * kind=7 万花凋零 魔法阵（mille_florum.png）
 * 两种贴图均贴地 XZ 平面，法线 +Y。
 */
public final class VoidTideGroundRenderer {
    public static final ResourceLocation VOID_RING_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/void_aoe_ring.png");
    public static final ResourceLocation VOID_CIRCLE_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/void_aoe_circle.png");
    public static final ResourceLocation TIME_RING_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/time_aoe_ring.png");
    public static final ResourceLocation TIME_CIRCLE_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/time_aoe_circle.png");
    /** r96：钢铁月环 环形贴图。 */
    public static final ResourceLocation FROST_MOON_RING_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/frost_moon_ring.png");
    /** r96：钢铁月环 圆形贴图。 */
    public static final ResourceLocation FROST_MOON_CIRCLE_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/frost_moon_circle.png");
    /** r96：光辉炎柱 预警圆贴图。 */
    public static final ResourceLocation FLAME_PILLAR_CIRCLE_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/flame_pillar_circle.png");
    /** r107：万花凋零终结技 魔法阵贴图。 */
    public static final ResourceLocation MILLE_FLORUM_TEX = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/mille_florum.png");
    public static final RenderType VOID_RING_LAYER = RenderType.entityTranslucent(VOID_RING_TEX);
    public static final RenderType VOID_CIRCLE_LAYER = RenderType.entityTranslucent(VOID_CIRCLE_TEX);
    public static final RenderType TIME_RING_LAYER = RenderType.entityTranslucent(TIME_RING_TEX);
    public static final RenderType TIME_CIRCLE_LAYER = RenderType.entityTranslucent(TIME_CIRCLE_TEX);
    public static final RenderType FROST_MOON_RING_LAYER = RenderType.entityTranslucent(FROST_MOON_RING_TEX);
    public static final RenderType FROST_MOON_CIRCLE_LAYER = RenderType.entityTranslucent(FROST_MOON_CIRCLE_TEX);
    public static final RenderType FLAME_PILLAR_CIRCLE_LAYER = RenderType.entityTranslucent(FLAME_PILLAR_CIRCLE_TEX);
    /** r107：万花凋零魔法阵图层。 */
    public static final RenderType MILLE_FLORUM_LAYER = RenderType.entityTranslucent(MILLE_FLORUM_TEX);

    /** 环形贴图内外半径比 = 8/18。 */
    public static final double RING_INNER_RATIO = 8.0 / 18.0;

    public static void draw(PoseStack pose, MultiBufferSource buffers,
                            float radius, float alpha, int kind) {
        if (!Float.isFinite(radius) || radius <= 0 || !Float.isFinite(alpha)) return;
        alpha = Math.max(0, Math.min(1, alpha));
        RenderType layer = switch (kind) {
            case 1 -> VOID_CIRCLE_LAYER;
            case 2 -> TIME_RING_LAYER;
            case 3 -> TIME_CIRCLE_LAYER;
            case 4 -> FROST_MOON_RING_LAYER;
            case 5 -> FROST_MOON_CIRCLE_LAYER;
            case 6 -> FLAME_PILLAR_CIRCLE_LAYER;
            case 7 -> MILLE_FLORUM_LAYER;
            default -> VOID_RING_LAYER;
        };
        VertexConsumer buffer = buffers.getBuffer(layer);
        PoseStack.Pose matrix = pose.last();
        // 贴地 XZ quad，法线 +Y，UV 0..1
        vertex(matrix, buffer, -radius, -radius, 0, 0, alpha);
        vertex(matrix, buffer, -radius, radius, 0, 1, alpha);
        vertex(matrix, buffer, radius, radius, 1, 1, alpha);
        vertex(matrix, buffer, radius, -radius, 1, 0, alpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer,
                               float x, float z, float u, float v, float alpha) {
        buffer.vertex(pose.pose(), x, 0, z).color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), 0, 1, 0).endVertex();
    }

    private VoidTideGroundRenderer() {}
}
