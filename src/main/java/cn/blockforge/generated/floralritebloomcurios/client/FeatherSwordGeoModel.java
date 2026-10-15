package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FeatherSwordEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * r96：羽剑 GeckoLib 模型——用户在 BlockBench 重做后导出 feather_sword.geo.json。
 * 单骨骼 "group"，贴图 feather_sword.png（16×16）。
 */
public class FeatherSwordGeoModel extends GeoModel<FeatherSwordEntity> {

    @Override
    public ResourceLocation getModelResource(FeatherSwordEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "geo/entity/feather_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FeatherSwordEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/feather_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FeatherSwordEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "animations/empty.animation.json");
    }
}
