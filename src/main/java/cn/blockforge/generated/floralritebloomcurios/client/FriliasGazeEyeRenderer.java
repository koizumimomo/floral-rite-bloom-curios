package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasGazeEyeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * r75：凝视之眼渲染器。
 * <ul>
 *   <li>固定朝向（不 billboard 跟随相机）——用户截图 #6 要求"大眼睛别转，让它是个实体固定在 boss 头上"；</li>
 *   <li>1.5x 放大（在原本 32px 粒子的基础上画 1.5 格高的四边形 + 自发光）；</li>
 *   <li>用 2 片 90° 角对置的竖直四边形（十字面片），从任何角度看都是同一张霜眼贴图。</li>
 * </ul>
 */
public class FriliasGazeEyeRenderer extends EntityRenderer<FriliasGazeEyeEntity> {

    /** 复用 FROST_EYE 粒子贴图（assets/floral_rite_bloom_curios/textures/particle/frost_eye.png）。 */
    private static final ResourceLocation TEX =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/particle/frost_eye.png");

    public FriliasGazeEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FriliasGazeEyeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        // 1.5x 放大：单片高度 1.5 格（用户要求），宽度同样 1.5。
        // 2 片十字面对置，从任何角度都能看到凝视之眼（不 billboard 的折中）。
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.05D, 0.0D);
        // 轻微呼吸缩放：1.5x 基础 + 0.05 振幅
        float breath = 1.5F + 0.05F * (float) Math.sin((entity.tickCount + partialTick) * 0.18D);
        // r75：渲染时不调用 lookAt / billboard——保持世界坐标固定朝向（不旋转跟随相机）。
        QuadBills.cross(poseStack, buffer, 2, breath, breath, -breath * 0.5F,
                0.65F, 0.90F, 1.0F, 0.95F, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FriliasGazeEyeEntity entity) {
        return TEX;
    }
}