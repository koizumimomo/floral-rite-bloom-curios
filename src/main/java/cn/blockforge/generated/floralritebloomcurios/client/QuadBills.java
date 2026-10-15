package cn.blockforge.generated.floralritebloomcurios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 世界空间十字面片绘制工具：把一张贴图以 N 片相互旋转的竖直四边形画出，
 * 从任意角度看都有实体轮廓（冰刺 / 荆棘 / 虚空裂隙 / 符文用）。
 */
public final class QuadBills {

    /** 画 N 片绕 Y 轴均分的竖直四边形（底边在局部 y=yOff）。 */
    public static void cross(PoseStack pose, VertexConsumer buffer, int quads,
                             float width, float height, float yOff,
                             float r, float g, float b, float a, int light) {
        for (int i = 0; i < quads; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 180.0F / quads));
            quad(pose, buffer, width, height, yOff, r, g, b, a, light);
            pose.popPose();
        }
    }

    /** 单片竖直四边形（局部 XY 平面，x 居中，底边 y=yOff）。 */
    public static void quad(PoseStack pose, VertexConsumer buffer, float width, float height,
                            float yOff, float r, float g, float b, float a, int light) {
        PoseStack.Pose p = pose.last();
        float hw = width / 2.0F;
        vertex(p, buffer, -hw, yOff, 0.0F, 0.0F, 1.0F, r, g, b, a, light);
        vertex(p, buffer, hw, yOff, 0.0F, 1.0F, 1.0F, r, g, b, a, light);
        vertex(p, buffer, hw, yOff + height, 0.0F, 1.0F, 0.0F, r, g, b, a, light);
        vertex(p, buffer, -hw, yOff + height, 0.0F, 0.0F, 0.0F, r, g, b, a, light);
    }

    /** 单片四边形，自定义 UV 矩形（花瓣象限用）。 */
    public static void quadUv(PoseStack pose, VertexConsumer buffer, float width, float height,
                              float u0, float v0, float u1, float v1,
                              float r, float g, float b, float a, int light) {
        PoseStack.Pose p = pose.last();
        float hw = width / 2.0F;
        vertex(p, buffer, -hw, 0.0F, 0.0F, u0, v1, r, g, b, a, light);
        vertex(p, buffer, hw, 0.0F, 0.0F, u1, v1, r, g, b, a, light);
        vertex(p, buffer, hw, height, 0.0F, u1, v0, r, g, b, a, light);
        vertex(p, buffer, -hw, height, 0.0F, u0, v0, r, g, b, a, light);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer buffer, float x, float y, float z,
                               float u, float v, float r, float g, float b, float a, int light) {
        // r88：overlay 必须用 NO_OVERLAY（pack(0,10)，白色行）。传 0 会采样 OverlayTexture
        // 第 0 行——ABGR 0xB20000FF 的暗红，shader 执行 mix(红, 原色, 0.7)，
        // 导致虚空裂隙 / 花之摇篮法阵 / 落花 / boss 头顶读条等所有经本工具绘制的面片恒染暗红。
        buffer.vertex(p.pose(), x, y, z).color(r, g, b, a)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(p.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    public static float pulse(float time, float base, float amp) {
        return Mth.clamp(base + Mth.sin(time * 0.15F) * amp, 0.0F, 1.0F);
    }

    private QuadBills() {}
}
