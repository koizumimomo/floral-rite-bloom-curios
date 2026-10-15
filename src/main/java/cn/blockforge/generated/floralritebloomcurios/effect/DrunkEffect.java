package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 喝醉了：获得此效果的实体会像果冻一样挤压回弹（客户端渲染 mixin 驱动），
 * 且芙莉艾丝 Boss 处于此状态时不会释放任何技能。
 */
public class DrunkEffect extends FRBEffect {

    public DrunkEffect() {
        // 中性效果：既非纯增益也非纯减益（对玩家是趣味变形，对 Boss 是控制）
        super(MobEffectCategory.NEUTRAL, 0xd4a05a, "drunk");
    }
}
