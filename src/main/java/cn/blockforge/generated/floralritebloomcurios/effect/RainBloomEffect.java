package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * 雨之华（云雨阶段 debuff）：每层 -5% 最大生命值，最高 12 层（= -60%）。
 *
 * 实现方式：用 AttributeModifier Operation.MULTIPLY_BASE；
 * 每次叠加 amplifier +1 时覆写更高幅度的 modifier（先清旧再加新），
 * 保证"叠层数 = 单 modifier 数值"，避免堆叠多个 modifier 后计算爆炸。
 */
public class RainBloomEffect extends FRBEffect {

    private static final UUID ID = UuidIds.uuid("frb_rain_bloom_max_hp");
    private static final float PER_STACK = -0.05F; // 每层 -5%

    public RainBloomEffect() {
        super(MobEffectCategory.HARMFUL, 0x3a59d8, "rain_bloom");
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        var attr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(ID);
        // r95：修 BUG——原用 MULTIPLY_BASE 语义为 result += base*amount，传入 0.95 会 +95% 生命（不降反增）。
        // 改用 MULTIPLY_TOTAL：result *= (1 + amount)，amount 为负即减少。
        float amount = PER_STACK * (amplifier + 1); // amplifier=0→-0.05（-5%）, =11→-0.60（-60%）
        attr.addPermanentModifier(new AttributeModifier(ID, "frb.rain_bloom",
                amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        var attr = attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(ID);
    }
}