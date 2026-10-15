package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.AoeWarnSigilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 通用 AOE 预警法阵渲染器：贴地画一张 aoe_circle.png，
 * 按实体同步的 RGB 染色（荆棘=绿，冰刺=蓝）。
 *
 * 与花之摇篮法阵（FlowerCradleSigilRenderer）区别：
 * - 贴图不同（aoe_circle.png vs flower_cradle_sigil.png），玩家一眼区分技能类型；
 * - 不支持粒子包边（花之摇篮独占，对应"踩塔"的 FF14 式提示）；
 * - 不旋转、不脉冲，保持预警读数的稳定可读。
 */
public class AoeWarnSigilRenderer extends EntityRenderer<AoeWarnSigilEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/effect/aoe_circle.png");
    private static final RenderType GLOW_TYPE = FRBRenderTypes.emissiveGlow(TEX);

    public AoeWarnSigilRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(AoeWarnSigilEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(GLOW_TYPE);
        pose.pushPose();
        pose.translate(0.0D, 0.1D, 0.0D);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        // 半径 2 格（直径 4）——与花之摇篮法阵判定半径一致，玩家可凭大小直读判定范围
        QuadBills.quad(pose, buf, 4.0F, 4.0F, -2.0F,
                entity.getR(), entity.getG(), entity.getB(), 0.92F, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AoeWarnSigilEntity entity) {
        return TEX;
    }
}
