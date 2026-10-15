package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.PetalVolleyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 花瓣 / 叶片弹：面向相机的 billboard 面片，飞行时自旋。
 * 占位贴图复用自然系粒子贴图（nature_petal / nature_leaf）。
 */
public class PetalVolleyRenderer extends EntityRenderer<PetalVolleyEntity> {
    private static final ResourceLocation PETAL =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/particle/nature_petal.png");
    private static final ResourceLocation LEAF =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/particle/nature_leaf.png");
    private static final float SIZE = 0.4F;

    public PetalVolleyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PetalVolleyEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        ResourceLocation tex = entity.isLeaf() ? LEAF : PETAL;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(tex));
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 15.0F));
        poseStack.translate(0.0D, -SIZE / 2.0D, 0.0D);
        QuadBills.quadUv(poseStack, buffer, SIZE, SIZE, 0.0F, 0.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 1.0F, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PetalVolleyEntity entity) {
        return entity.isLeaf() ? LEAF : PETAL;
    }
}
