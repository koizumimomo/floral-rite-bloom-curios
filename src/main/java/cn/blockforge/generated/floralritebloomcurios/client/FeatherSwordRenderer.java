package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FeatherSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * r96 羽剑渲染器——改用 GeckoLib geo 模型（feather_sword.geo.json）。
 *
 * 警告期：在路线上方贴一张平铺地面的红色矩形 quad（feather_sword_warn.png）。
 * 全期：渲染羽剑 geo 模型，剑尖朝 -Z（实体前方 = 冲锋方向），按 DATA_SCALE 放大。
 *   警告期剑悬浮蓄势；冲锋期由实体位置随路线推进。
 */
public class FeatherSwordRenderer extends GeoEntityRenderer<FeatherSwordEntity> {

    /** r96：羽剑冲锋预警长条贴图（红色半透明）。 */
    private static final ResourceLocation WARN_TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/effect/feather_sword_warn.png");
    private static final int FULL_LIGHT = 0xF000F0;

    public FeatherSwordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FeatherSwordGeoModel());
    }

    @Override
    public void render(FeatherSwordEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        // ---- 警告期：贴地红色直线矩形 ----
        int state = entity.getState();
        if (state == FeatherSwordEntity.STATE_WARNING) {
            float sx = (float) entity.getX();
            float sz = (float) entity.getZ();
            float ex = entity.getEndX();
            float ez = entity.getEndZ();
            float dx = ex - sx;
            float dz = ez - sz;
            float len = (float) Math.hypot(dx, dz);
            float halfW = entity.getHalfWidth();
            if (len > 0.001F) {
                pose.pushPose();
                // r90：红线贴地渲染——高度以同步的 groundY 为准
                float groundDy = entity.getGroundY() - (float) entity.getY() + 0.03F;
                // 平移到路线中点
                pose.translate(dx / 2.0F, groundDy, dz / 2.0F);
                // 矩形长度沿局部 X 轴，需让局部 X 对齐冲锋方向 (dx, dz)。
                // YP 旋转矩阵把局部 +X 映射到 (cosθ, 0, -sinθ)，故 θ = atan2(-dz, dx)。
                float barAngle = (float) Math.toDegrees(Math.atan2(-dz, dx));
                pose.mulPose(Axis.YP.rotationDegrees(barAngle));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                int remain = entity.getWarningRemaining();
                float flash = 0.45F + 0.35F * (1.0F - remain / (float) FeatherSwordEntity.WARNING_TICKS);
                // 矩形从 -len/2 到 +len/2，左缘(UV=0)对齐出生点，右缘(UV=1)对齐目标点
                drawFlatRect(pose, buffers, len, halfW * 2.0F, 1.0F, 1.0F, 1.0F, flash);
                pose.popPose();
            }
        }

        // 强制用同步的 DATA_YAW 驱动模型朝向（非生物实体 yRot 客户端可能未同步）
        float dataYaw = entity.getYawDeg();
        entity.setYRot(dataYaw);
        entity.yRotO = dataYaw;
        // 剑模型由 super.render 渲染
        super.render(entity, dataYaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void applyRotations(FeatherSwordEntity animatable, PoseStack poseStack,
                                  float ageInTicks, float netHeadYaw, float headPitch) {
        // r99：禁用 GeckoLib 默认的 (180 - yaw) 旋转，改由 preRender 按冲锋方向精确控制，
        //      确保 top（剑尖）朝向冲刺终点、down（剑柄）朝向冲刺起点。
    }

    @Override
    public void preRender(PoseStack poseStack, FeatherSwordEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // r99/r101：显式绕 Y 轴旋转，使模型 +Z 轴（剑尖/top）对齐冲锋方向。
        //      模型 +Z = 剑尖、-Z = 剑柄，故旋转角取 -yaw（Minecraft yaw 为左手系，
        //      YP 正向旋转会把 +Z 转到 -yaw 方向，必须取反才指向终点）。
        float yaw = animatable.getYawDeg();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        int state = animatable.getState();
        // 警告期：剑悬浮在地面 +1.0 处蓄势；冲锋期：贴地面 +0.5
        float hoverY = (state == FeatherSwordEntity.STATE_WARNING)
                ? 1.0F + 0.1F * (float) Math.sin((animatable.tickCount + partialTick) * 0.3F)
                : 0.5F;
        poseStack.translate(0.0F, hoverY, 0.0F);

        // 警告期：剑轻微抖动
        if (state == FeatherSwordEntity.STATE_WARNING) {
            float shake = 0.15F * (float) Math.sin((animatable.tickCount + partialTick) * 0.8F);
            poseStack.translate(0.0F, 0.0F, shake * 0.1F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(shake * 8.0F));
        }

        // 按实体数据缩放
        float scale = animatable.getScale();
        this.scaleWidth = scale;
        this.scaleHeight = scale;
    }

    /** 在当前 PoseStack 局部 XY 平面画一个矩形（中心在原点，宽=w 沿 X，高=h 沿 Y）。 */
    private static void drawFlatRect(PoseStack pose, MultiBufferSource buffers,
                                     float w, float h, float r, float g, float b, float a) {
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(WARN_TEX));
        PoseStack.Pose p = pose.last();
        float hw = w / 2.0F;
        float hh = h / 2.0F;
        buf.vertex(p.pose(), -hw, -hh, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        buf.vertex(p.pose(), hw, -hh, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        buf.vertex(p.pose(), hw, hh, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        buf.vertex(p.pose(), -hw, hh, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    public RenderType getRenderType(FeatherSwordEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
