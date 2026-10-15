package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 风刃渲染：4 片十字面片模拟气流轨迹。
 *
 * 注：暂无专门贴图（用户 F2 计划为"风刃/积雨云贴图（若不复用花瓣）"但 texture 未到位前，
 * 暂用现有的 nature_leaf.png 作为占位视觉）。
 */
public class WindBladeRenderer extends EntityRenderer<WindBladeEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/particle/nature_leaf.png");
    private static final int FULL_LIGHT = 0xF000F0;

    public WindBladeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WindBladeEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        float age = entity.tickCount + partialTick;
        float spin = (age * 12.0F) % 360.0F;
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        QuadBills.cross(pose, buf, 4, 0.5F, 0.5F, 0.0F,
                1.0F, 1.0F, 1.0F, 0.85F, FULL_LIGHT);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WindBladeEntity entity) {
        return TEX;
    }
}