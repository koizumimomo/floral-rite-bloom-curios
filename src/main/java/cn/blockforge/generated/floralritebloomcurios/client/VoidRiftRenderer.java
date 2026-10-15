package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.VoidRiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 虚空裂隙：竖直撕裂的十字面片，带轻微自旋与明暗脉动。
 */
public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/void_rift.png");

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoidRiftEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        float pulse = QuadBills.pulse(entity.tickCount + partialTick, 0.85F, 0.15F);
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D, 0.0D);
        QuadBills.cross(poseStack, buffer, 2, 1.35F, 2.3F, 0.0F,
                pulse, pulse, pulse, 1.0F, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VoidRiftEntity entity) {
        return TEX;
    }
}
