package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.RainCloudEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 积雨云 GeckoLib 模型（r64）：用户提供的 rain_cloud.geo.json + rain_cloud.png。
 * 模型由多个 cube 拼成不规则云朵，geo 中 pivot=37×[0,37,-5]，这里 scale 0.045 还原现实尺寸。
 */
public class RainCloudGeoModel extends GeoModel<RainCloudEntity> {

    @Override
    public ResourceLocation getModelResource(RainCloudEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "geo/entity/rain_cloud.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RainCloudEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/rain_cloud.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RainCloudEntity animatable) {
        // 没有动画文件，返回一个空动画占位即可
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "animations/empty.animation.json");
    }
}