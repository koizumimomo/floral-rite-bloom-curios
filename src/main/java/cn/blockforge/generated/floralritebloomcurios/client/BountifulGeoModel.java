package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.item.BountifulItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

/**
 * 丰饶的 GeckoLib 模型（与铁魔法盔甲/镰刀同一渲染链，替代手写 BountifulRenderer）：
 * geo 定位 assets/floral_rite_bloom_curios/geo/bountiful.geo.json（Blockbench bedrock per-face UV 导出），
 * 动画定位 animations/bountiful.animation.json（当前为空文件，仅静态姿态），
 * 贴图沿用 textures/entity/feng_rao.png（64x64）。
 * 物品手持角度仍由 models/item/bountiful.json 的 display 提供
 * （ItemRenderer 在调用 BEWLR 之前统一应用 display transform）。
 */
public class BountifulGeoModel extends DefaultedItemGeoModel<BountifulItem> {

    public BountifulGeoModel() {
        super(new ResourceLocation(FloralRiteBloom.MOD_ID, "bountiful"));
    }

    /** geo 定位走 DefaultedItemGeoModel 默认路径 geo/item/bountiful.geo.json（文件已在此处），无需 override。 */
    @Override
    public ResourceLocation getAnimationResource(BountifulItem animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "animations/bountiful.animation.json");
    }

    @Override
    public ResourceLocation getTextureResource(BountifulItem animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/feng_rao.png");
    }
}
