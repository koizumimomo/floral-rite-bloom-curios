package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 花瓣印记：焚生藤的攻击会在目标身上叠层（不显示粒子、图标隐藏由 tooltip 表达），
 * 层数通过 amplifier 记录，事件系统负责在第 4 层后的下一次攻击引爆。
 */
public class PetalMarkEffect extends FRBEffect {
    public PetalMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xff7a3c, "petal_mark");
    }
}
