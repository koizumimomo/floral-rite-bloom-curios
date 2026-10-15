package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.SpikeVolleyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 荆棘 / 冰刺飞弹渲染：4 片十字面片（复用 SpikeRenderer 已有的贴图路径），随时间自转。
 */
public class SpikeVolleyRenderer extends EntityRenderer<SpikeVolleyEntity> {

    private static final ResourceLocation THORN = texture("thorn_bark");
    private static final ResourceLocation ICE = texture("ice_spike");
    private static final int FULL_LIGHT = 0xF000F0;

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/" + name + ".png");
    }

    public SpikeVolleyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpikeVolleyEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        boolean nature = entity.getKind() == SpikeVolleyEntity.KIND_THORN;
        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(nature ? THORN : ICE));
        float age = entity.tickCount + partialTick;
        float spin = (age * 14.0F) % 360.0F;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.4F);
        pose.pushPose();
        // 让面片朝向飞行方向：先按 yaw 转，再横摆 90 度让 quad 立起来
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        QuadBills.cross(pose, buf, 4, 0.55F * pulse, 0.55F * pulse, 0.0F,
                1.0F, 1.0F, 1.0F, 1.0F, FULL_LIGHT);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpikeVolleyEntity entity) {
        return entity.getKind() == SpikeVolleyEntity.KIND_THORN ? THORN : ICE;
    }
}