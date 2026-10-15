package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 芙莉艾丝的背后光环：花瓣外环与银河字母内环以不同速度反向旋转。
 *  boss 自身光环永远走手绘面片路径（128 像素贴图）；
 *  玩家的像素/高清双环 r91 起改走 PlayerHaloRenderer 的 geo 模型，与本类无关。
 */
public final class FriliasBackRingLayer extends RenderLayer<FriliasEntity, FriliasModel> {
    private static final ResourceLocation RING_TEXTURE = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/entity/frilias_back_ring.png");
    /** 自发光+深度写入（原版 translucent_emissive 不写深度会被远处生物和云遮挡），五层共用一个类型以合批。 */
    private static final RenderType RING_TYPE = FRBRenderTypes.backRing(RING_TEXTURE);

    public FriliasBackRingLayer(RenderLayerParent<FriliasEntity, FriliasModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       FriliasEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float time = entity.tickCount + partialTick;
        float breathe = Mth.sin(time * 0.055F) * 0.012F;

        poseStack.pushPose();
        // 先绑定 Boss 头部骨骼，再把环心放到后脑：模型空间 -Y 朝上（实体渲染翻转），
        // 上一轮 -8.2/16 实际把环抬高了；按用户反馈反向相同数值回到 -1.8/16（原 -5.0 基础下移 0.2 格）。
        getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.0D, -1.8D / 16.0D + breathe, 5.0D / 16.0D);
        // r75：跟随 boss 当前阶段取主题色 tint，五层不再写死 R/G/B 让"背后法阵永远自然色"。
        int phase = entity.getFrbPhase();
        int colorRGB = (phase >= 0 && phase < FriliasEntity.PHASE_COLORS.length)
                ? FriliasEntity.PHASE_COLORS[phase] : 0x7ce68b;
        renderRingPair(poseStack, buffers, time, colorRGB);
        poseStack.popPose();
    }

    /**
     * r90：按新 2×2 贴图重写——每层只取一个完整圆环区域。
     * 像素贴图 128×128：TL(0,0)-(64,64)=花环、TR(64,0)-(128,64)=魔法环。
     * BL/BR 是花瓣/星芒碎片（非圆环），不再当环渲染。
     * 两层同心反向旋转：花环（外层，-45°/s）+ 魔法环（内层，+90°/s）。
     */
    static void renderRingPair(PoseStack poseStack, MultiBufferSource buffers, float time, int phaseColor) {
        float[] rgb = rgbFloats(phaseColor);
        // 花环层：UV (0,0)-(64,64)，外层，-45°/s
        renderRing(poseStack, buffers, 40.0F / 16.0F, 1.5F / 16.0F, 0, 0, -time * 2.25F,
                lerp(rgb[0], 1.0F, 0.10F), lerp(rgb[1], 1.0F, 0.10F), lerp(rgb[2], 1.0F, 0.10F), 0.92F);
        // 魔法环层：UV (64,0)-(128,64)，内层，+90°/s
        renderRing(poseStack, buffers, 28.0F / 16.0F, 0.0F, 64, 0, time * 4.5F,
                lerp(rgb[0], 1.0F, 0.55F), lerp(rgb[1], 1.0F, 0.55F), lerp(rgb[2], 1.0F, 0.55F), 0.96F);
    }

    private static float[] rgbFloats(int rgb) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F};
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void renderRing(PoseStack poseStack, MultiBufferSource buffers,
                                   float size, float depth, int u0, int v0, float rotation,
                                   float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, depth);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        VertexConsumer buffer = buffers.getBuffer(RING_TYPE);
        quad(poseStack.last(), buffer, size, u0, v0, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float size, int u0, int v0,
                             float red, float green, float blue, float alpha) {
        float half = size * 0.5F;
        float uStart = u0 / 128.0F;
        float uEnd = (u0 + 64) / 128.0F;
        float vStart = v0 / 128.0F;
        float vEnd = (v0 + 64) / 128.0F;
        vertex(pose, buffer, -half, -half, 0.0F, uStart, vStart, red, green, blue, alpha);
        vertex(pose, buffer, half, -half, 0.0F, uEnd, vStart, red, green, blue, alpha);
        vertex(pose, buffer, half, half, 0.0F, uEnd, vEnd, red, green, blue, alpha);
        vertex(pose, buffer, -half, half, 0.0F, uStart, vEnd, red, green, blue, alpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer,
                               float x, float y, float z, float u, float v,
                               float red, float green, float blue, float alpha) {
        buffer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
