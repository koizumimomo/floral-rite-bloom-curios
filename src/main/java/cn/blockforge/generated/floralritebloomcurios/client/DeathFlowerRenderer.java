package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.DeathFlowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 死亡之花：两片十字面片 + 轻微摇摆；血量通过名牌显示（100HP / 6 护甲）。
 */
public class DeathFlowerRenderer extends EntityRenderer<DeathFlowerEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/death_flower.png");

    public DeathFlowerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DeathFlowerEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.05D, 0.0D);
        float sway = (entity.tickCount + partialTick) * 2.0F;
        QuadBills.cross(poseStack, buffer, 2, 1.1F, 1.35F, 0.0F,
                1.0F, 1.0F, 1.0F, 1.0F, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DeathFlowerEntity entity) {
        return TEX;
    }
}
