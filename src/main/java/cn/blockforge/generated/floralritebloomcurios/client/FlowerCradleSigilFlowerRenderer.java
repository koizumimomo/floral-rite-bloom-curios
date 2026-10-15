package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilFlowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 落花：始终面向相机，伴随下飘的轻微摆动。
 * r65：改用自发光渲染类型，黑暗中也发亮。
 *
 * 注：用户的 flower_cradle_sigil_flower.geo.json 描述了一个简单平面几何，
 * 这里走 billboard 路径同样能渲染贴图并节省 GeckoLib 加载开销。
 * 未来若要切换到 GeckoLib 模型，把本类改成 GeoEntityRenderer<FlowerCradleSigilFlowerEntity> 即可。
 */
public class FlowerCradleSigilFlowerRenderer extends EntityRenderer<FlowerCradleSigilFlowerEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/entity/flower_cradle_sigil_flower.png");
    /** r65：自发光+深度写入+双面，落花在黑暗中也发亮。 */
    private static final RenderType GLOW_TYPE = FRBRenderTypes.emissiveGlow(TEX);

    public FlowerCradleSigilFlowerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FlowerCradleSigilFlowerEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(GLOW_TYPE);
        float age = entity.tickCount + partialTick;
        float sway = (float) Math.sin(age * 0.3F) * 8.0F;

        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sway));
        QuadBills.cross(pose, buf, 2, 0.7F, 0.9F, 0.0F,
                1.0F, 1.0F, 1.0F, 1.0F, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerCradleSigilFlowerEntity entity) {
        return TEX;
    }
}