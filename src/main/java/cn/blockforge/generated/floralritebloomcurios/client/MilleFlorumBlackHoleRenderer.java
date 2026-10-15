package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.MilleFlorumBlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 万花凋零黑洞实体渲染：4 片十字紫黑色球面 + 缓慢自转 + 脉动收缩。
 *
 * 注：实际"黑洞吞噬感"由 boss_finale.fsh 后处理 shader 提供，
 * 这里只画一个紫黑色发光球作为视觉占位（让玩家直观看到位置）。
 */
public class MilleFlorumBlackHoleRenderer extends EntityRenderer<MilleFlorumBlackHoleEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/particle/void_core.png");
    private static final int FULL_LIGHT = 0xF000F0;

    public MilleFlorumBlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MilleFlorumBlackHoleEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        float age = entity.tickCount + partialTick;
        float spin = (age * 1.5F) % 360.0F;
        // r112：黑洞尺寸 ×2（原 1.6±0.2，混在花瓣粒子里太小看不出来）。
        // 改大后 boss_finale.fsh 的引力透镜扭曲区域也更醒目。
        float pulse = 3.2F + 0.4F * (float) Math.sin(age * 0.18F);
        float alpha = 0.85F + 0.1F * (float) Math.sin(age * 0.25F);

        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
        QuadBills.cross(pose, buf, 6, pulse, pulse, 0.0F,
                0.05F, 0.0F, 0.05F, alpha, FULL_LIGHT);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MilleFlorumBlackHoleEntity entity) {
        return TEX;
    }
}