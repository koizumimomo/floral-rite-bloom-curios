package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.item.LunarCrownItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

/**
 * r74：「月之庇护」GeckoLib 模型。
 * - geo  路径：assets/floral_rite_bloom_curios/geo/item/armor/lunar_crown.geo.json
 * - 贴图路径：assets/floral_rite_bloom_curios/textures/item/armor/lunar_crown.png
 * - 无动画（静态头冠）；curioTick 不写 NBT。
 */
public class LunarCrownGeoModel extends DefaultedItemGeoModel<LunarCrownItem> {

    public LunarCrownGeoModel() {
        // r88 修复：DefaultedItemGeoModel 会自动补全 geo/item/<subpath>.geo.json 与
        // textures/item/<subpath>.png。此前传 "item/armor/lunar_crown" 被拼成
        // geo/item/item/armor/lunar_crown.geo.json（不存在），导致 GeckoLib 每帧重抛
        // "Unable to find model" + 贴图 FileNotFound——InventoryHUD 每帧渲染背包中的
        // 头冠时触发，造成进游戏后持续卡顿。正确 subpath 为 "armor/lunar_crown"。
        super(new ResourceLocation(FloralRiteBloom.MOD_ID, "armor/lunar_crown"));
    }

    @Override
    public ResourceLocation getAnimationResource(LunarCrownItem animatable) {
        // 静态模型——GeckoLib 要求非空 ResourceLocation；指向一个不存在也 OK（fallback 跳过）
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "animations/lunar_crown.animation.json");
    }
    // r88：不再重写 getTextureResource——默认实现返回
    // textures/item/armor/lunar_crown.png（AnimatableTexture 需要这种含 textures/ 与 .png
    // 的完整路径；此前短路径 "item/armor/lunar_crown" 会被按原始资源路径加载而每帧报 FileNotFound）。
}