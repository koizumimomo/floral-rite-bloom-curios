package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.WaterRippleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * r96 万变水波渲染器：贴地画一张 water_ripple.png，
 * 半径 3 格（直径 6），淡蓝色半透明，带轻微脉冲呼吸感。
 */
public class WaterRippleRenderer extends EntityRenderer<WaterRippleEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/effect/water_ripple.png");
    private static final RenderType GLOW_TYPE = FRBRenderTypes.emissiveGlow(TEX);

    public WaterRippleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WaterRippleEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(GLOW_TYPE);
        pose.pushPose();
        pose.translate(0.0D, 0.1D, 0.0D);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        // 直径 6 格 = 半径 3，与判定半径一致
        float pulse = 0.85F + 0.15F * (float) Math.sin(entity.tickCount * 0.08D);
        QuadBills.quad(pose, buf, 6.0F, 6.0F, -3.0F,
                0.35F, 0.6F, 1.0F, pulse, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WaterRippleEntity entity) {
        return TEX;
    }
}
