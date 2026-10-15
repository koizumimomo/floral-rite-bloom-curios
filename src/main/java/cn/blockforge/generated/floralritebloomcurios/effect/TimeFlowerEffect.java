package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 时之华（芙莉艾丝 · 时间阶段）：造成的伤害降低 70%，持续整个阶段。
 * 阶段结束由 Boss 统一清除（与霜之华 / 云之华同一套生命周期）。
 */
public class TimeFlowerEffect extends FRBEffect {
    private static final String DAMAGE_ID = UuidIds.of("frb_time_flower_damage");

    public TimeFlowerEffect() {
        super(MobEffectCategory.HARMFUL, 0xe0e0e0, "time_flower");
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                DAMAGE_ID, -0.70D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
