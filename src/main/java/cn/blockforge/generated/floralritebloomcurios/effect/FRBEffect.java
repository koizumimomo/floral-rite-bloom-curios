package cn.blockforge.generated.floralritebloomcurios.effect;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 效果名保持原版纯名称契约，详细描述由独立语言键提供给配方查看器。 */
public class FRBEffect extends MobEffect {
    private final String descriptionKey;

    protected FRBEffect(MobEffectCategory category, int color, String effectId) {
        super(category, color);
        this.descriptionKey = "effect." + FloralRiteBloom.MOD_ID + "." + effectId + ".description";
    }

    public Component descriptionLine() {
        return Component.translatable(descriptionKey)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColor())));
    }
}
