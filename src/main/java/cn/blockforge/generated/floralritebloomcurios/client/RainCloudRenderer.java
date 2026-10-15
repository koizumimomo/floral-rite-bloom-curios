package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.entity.RainCloudEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 积雨云渲染：r64 改用用户提供的 rain_cloud.geo.json GeckoLib 模型（替代原 cross billboard）。
 * r96：模型几乎看不见——改用 emissive 渲染层让云朵全亮度发光，无视环境光照。
 */
public class RainCloudRenderer extends GeoEntityRenderer<RainCloudEntity> {

    public RainCloudRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RainCloudGeoModel());
        // geo pivot 是 0,37,-5，BlockBench 模型默认 24 单位 = 1 格，
        // 这里把模型缩 0.04 倍（即 1/24 ≈ 0.042）让云朵约为 1×1×1 格尺寸
        this.scaleHeight = 0.04F;
        this.scaleWidth = 0.04F;
    }

    /** r96：让积雨云始终全亮度发光（无视环境光），避免在永恒花园自定义天空下几乎看不见。 */
    @Override
    public RenderType getRenderType(RainCloudEntity animatable, ResourceLocation texture,
                                    net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}