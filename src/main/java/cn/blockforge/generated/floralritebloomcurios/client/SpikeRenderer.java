package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.SpikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 地刺实体同步驱动贴地法阵、破土生长和退场，不依赖粒子设置。 */
public class SpikeRenderer extends EntityRenderer<SpikeEntity> {
    private static final ResourceLocation ICE = texture("ice_spike");
    private static final ResourceLocation THORN = texture("thorn_bark");
    private static final ResourceLocation NATURE_SIGIL = texture("nature_sigil");
    private static final ResourceLocation FROST_SIGIL = texture("frost_sigil");
    private final ModelPart thorn = ThornModel.create();

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/" + name + ".png");
    }

    public SpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpikeEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        float fuse = entity.getFuse() - partialTick;
        boolean nature = entity.getKind() == SpikeEntity.KIND_THORN;
        float fade = Mth.clamp((fuse + 8.0F) / 8.0F, 0.0F, 1.0F);
        float appear = Mth.clamp((31.0F - fuse) / 5.0F, 0.0F, 1.0F);
        float pulse = 0.85F + 0.15F * Mth.sin((30.0F - fuse) * 0.3F);
        pose.pushPose();
        pose.translate(0, 0.035D, 0);
        pose.mulPose(Axis.YP.rotationDegrees((30.0F - fuse) * (nature ? 1.2F : -1.2F)));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        QuadBills.quad(pose, buffers.getBuffer(RenderType.entityTranslucent(nature ? NATURE_SIGIL : FROST_SIGIL)),
                2.7F, 2.7F, -1.35F, 1, 1, 1, appear * fade * pulse, 0xF000F0);
        pose.popPose();

        // 预警期只露出微小芽尖，最后八 tick 从地面长出，命中后收回。
        float growth = Mth.clamp((8.0F - fuse) / 8.0F, 0.0F, 1.0F);
        growth = (0.04F + 0.96F * growth * growth) * fade;
        pose.pushPose();
        if (nature) {
            pose.mulPose(Axis.YP.rotationDegrees(entity.getId() * 137.5F));
            pose.scale(1.0F, -growth, 1.0F);
            thorn.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(THORN)),
                    light, OverlayTexture.NO_OVERLAY);
        } else {
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(ICE));
            QuadBills.cross(pose, buffer, 3, 1.25F, 3.2F * growth, 0,
                    1, 1, 1, fade, 0xF000F0);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpikeEntity entity) {
        return entity.getKind() == SpikeEntity.KIND_THORN ? THORN : ICE;
    }
}
