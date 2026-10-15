package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.block.entity.FloralAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 花仪台座的供品渲染：台面上方悬浮缓转展示所放的物品。
 */
public class FloralAltarRenderer implements BlockEntityRenderer<FloralAltarBlockEntity> {

    public FloralAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FloralAltarBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = be.getOffering();
        if (stack.isEmpty()) return;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.92D + Math.sin((be.getLevel().getGameTime() + partialTick) * 0.06D) * 0.03D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(((be.getLevel().getGameTime() + partialTick) * 1.6F) % 360.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, buffer, be.getLevel(), 0);
        poseStack.popPose();
    }
}
