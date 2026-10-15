package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 霜之华（世界之花 Boss · 霜华阶段）：攻击伤害 -50%。阶段结束由 Boss 统一清除。
 */
public class FrostBloomEffect extends FRBEffect {
    // 1.20.1 要求 UUID 字符串，见 UuidIds。
    private static final String DAMAGE_ID = UuidIds.of("frb_frost_bloom_damage");

    public FrostBloomEffect() {
        super(MobEffectCategory.HARMFUL, 0xbfe8ff, "frost_bloom");
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                DAMAGE_ID, -0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
