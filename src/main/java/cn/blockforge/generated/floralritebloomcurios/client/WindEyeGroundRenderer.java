package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 风眼月环的局部 XZ 平面绘制工具；调用方负责预警生命周期与相机坐标变换。
 * 不自动订阅渲染事件、不自动改变 Boss 技能。
 */
public final class WindEyeGroundRenderer {
    public static final ResourceLocation TEXTURE = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/effect/wind_eye_4_18.png");
    public static final RenderType LAYER = RenderType.entityTranslucent(TEXTURE);
    public static final double INNER_TO_OUTER = 4.0 / 18.0;

    /** 默认外半径为 18。修改外半径时，风眼内半径同比例变化。 */
    public static void draw(PoseStack pose, MultiBufferSource buffers, float outerRadius, float alpha) {
        if (!Float.isFinite(outerRadius) || outerRadius <= 0 || !Float.isFinite(alpha)) return;
        alpha = Math.max(0, Math.min(1, alpha));
        VertexConsumer buffer = buffers.getBuffer(LAYER);
        PoseStack.Pose matrix = pose.last();
        // 从上方看为正面，法线为 +Y。局部原点即预警圆心。
        vertex(matrix, buffer, -outerRadius, -outerRadius, 0, 0, alpha);
        vertex(matrix, buffer, -outerRadius, outerRadius, 0, 1, alpha);
        vertex(matrix, buffer, outerRadius, outerRadius, 1, 1, alpha);
        vertex(matrix, buffer, outerRadius, -outerRadius, 1, 0, alpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer,
                               float x, float z, float u, float v, float alpha) {
        // r88：NO_OVERLAY 白色行；overlayCoords(0) 会恒叠 30% 暗红（同 QuadBills 修复）。
        buffer.vertex(pose.pose(), x, 0, z).color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), 0, 1, 0).endVertex();
    }

    private WindEyeGroundRenderer() {}
}
