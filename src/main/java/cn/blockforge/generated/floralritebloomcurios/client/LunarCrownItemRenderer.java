package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.item.LunarCrownItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * r88：月之庇护的 BEWLR 渲染器。
 *
 * GeckoLib 的 GeoItemRenderer 完全忽略原版物品模型（models/item/lunar_crown.json）
 * 里的 display 变换——用户在该文件 gui 节调的抬高/缩放/旋转不会生效。
 * 这里在 renderInGui 中按原版 gui display 语义手动施加与 lunar_crown.json 相同的参数：
 * rotation [15,-150,5]、translation [0,1,0]（像素，1px=1/16 格）、scale 1.1。
 * 变换原点取模型中心，与原版 ItemTransform 的 translate(0.5) 中心化思路一致。
 */
public class LunarCrownItemRenderer extends GeoItemRenderer<LunarCrownItem> {

    public LunarCrownItemRenderer() {
        super(new LunarCrownGeoModel());
    }

    // r90：GUI 变换暂时回退到 GeckoLib 默认行为，等后续再回来修
    // @Override
    // protected void renderInGui(ItemDisplayContext context, PoseStack pose,
    //                            MultiBufferSource buffers, int packedLight, int packedOverlay) { ... }
}
