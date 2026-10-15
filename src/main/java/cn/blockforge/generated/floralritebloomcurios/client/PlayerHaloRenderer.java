package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * r91：玩家"万花万草之灵"神环——改走 GeckoLib geo 模型（此前是手写面片 + 128/1024 双 UV）。
 * <ul>
 *   <li>HALO_STYLE 配置实时切换：PIXELATED → frilias_back_ring_pix.geo.json + frilias_back_ring.png；
 *       HIGH_RES → frilias_back_ring.geo.json + back_ring_3.png。</li>
 *   <li>不使用 GeckoLib 动画系统：旋转在 {@link #preRender} 里直接驱动骨骼，
 *       外环 -45°/s、内环 +90°/s，与 r90 手写环节奏一致。</li>
 *   <li>渲染走自定义 emissiveGlow（满光自发光、写深度、双面、完整 sampler state），
 *       天然不写顶点 tint——之前玩家环被自然绿染色的问题不再出现。</li>
 * </ul>
 * 注意：模型骨骼名必须是 flower_ring / petal_ring / out（外组）与 glyph_ring（内环），
 * 重新导出 pix 模型时保持同名，否则对应层不会转。
 */
public final class PlayerHaloRenderer extends GeoObjectRenderer<PlayerHaloRenderer.HaloAnimatable> {

    public static final PlayerHaloRenderer INSTANCE = new PlayerHaloRenderer();

    /** 外环骨骼（花环/花瓣/外圈光层），-45°/s。 */
    private static final String[] OUTER_BONES = {"flower_ring", "petal_ring", "out"};
    /** 内环符文，+90°/s。 */
    private static final String INNER_BONE = "glyph_ring";

    /** 本帧动画时间（tick，含 partialTick），由 Layer 渲染前写入。 */
    private float animTime;

    private PlayerHaloRenderer() {
        super(new HaloGeoModel());
    }

    /** 在当前 PoseStack 锚点渲染神环；模型自带根骨骼 pivot，Layer 需先抵消后定位。 */
    public void renderHalo(PoseStack poseStack, MultiBufferSource buffers, float time) {
        this.animTime = time;
        // renderType/vertexConsumer 传 null：defaultRender 会走 getTextureLocation + 重写的 getRenderType
        this.render(poseStack, HaloAnimatable.INSTANCE, buffers, null, null,
                LightTexture.FULL_BRIGHT);
    }

    @Override
    public void preRender(PoseStack poseStack, HaloAnimatable animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // GeoObjectRenderer.preRender 末尾硬编码 translate(0.5, 0.51, 0.5)（实体居中偏移），
        // 但神环由 Layer 精确定位，必须抵消该偏移，否则环整体向右上偏移约 8px。
        poseStack.translate(-0.5D, -0.51D, -0.5D);
        // 与 r90 手写面片同节奏：外环 -time*2.25°/tick，内环 +time*4.5°/tick；弧度制
        float outer = -animTime * 2.25F * Mth.DEG_TO_RAD;
        float inner = animTime * 4.5F * Mth.DEG_TO_RAD;
        for (String name : OUTER_BONES) {
            model.getBone(name).ifPresent(bone -> bone.setRotZ(outer));
        }
        model.getBone(INNER_BONE).ifPresent(bone -> bone.setRotZ(inner));
    }

    @Override
    public RenderType getRenderType(HaloAnimatable animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        // 神环发光面片：emissive 满光 + 写深度 + NO_CULL（前环镜像时也可见）
        return FRBRenderTypes.emissiveGlow(texture);
    }

    /** 无控制器的静态 animatable：玩家不是 GeckoLib 实体，模型旋转全部手动驱动，不需要动画缓存数据。 */
    public static final class HaloAnimatable implements GeoAnimatable {
        static final HaloAnimatable INSTANCE = new HaloAnimatable();

        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return this.cache;
        }

        @Override
        public double getTick(Object object) {
            return 0;
        }
    }

    /** 双模型/双贴图：每次渲染按 FRBConfig.haloStyle() 实时选择，改配置重载资源后即时生效。 */
    private static final class HaloGeoModel extends GeoModel<HaloAnimatable> {
        private static final ResourceLocation GEO_HIGH_RES =
                loc("geo/item/armor/frilias_back_ring.geo.json");
        private static final ResourceLocation GEO_PIXEL =
                loc("geo/item/armor/frilias_back_ring_pix.geo.json");
        private static final ResourceLocation TEX_HIGH_RES =
                loc("textures/entity/back_ring_3.png");
        private static final ResourceLocation TEX_PIXEL =
                loc("textures/entity/frilias_back_ring.png");

        private static ResourceLocation loc(String path) {
            return new ResourceLocation(FloralRiteBloom.MOD_ID, path);
        }

        @Override
        public ResourceLocation getModelResource(HaloAnimatable animatable) {
            return FRBConfig.haloStyle() == FRBConfig.HaloStyle.HIGH_RES
                    ? GEO_HIGH_RES : GEO_PIXEL;
        }

        @Override
        public ResourceLocation getTextureResource(HaloAnimatable animatable) {
            return FRBConfig.haloStyle() == FRBConfig.HaloStyle.HIGH_RES
                    ? TEX_HIGH_RES : TEX_PIXEL;
        }

        @Override
        public ResourceLocation getAnimationResource(HaloAnimatable animatable) {
            return null; // 无 animation json，旋转手动驱动
        }
    }
}
