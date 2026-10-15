package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 云之华（世界之花 Boss · 云雨阶段）：攻击力 -75%，且生命恢复与流失反转 ——
 * 拥有生命恢复时会持续流失生命，每 2 秒额外扣除生命。
 */
public class CloudGraceEffect extends FRBEffect {
    // 1.20.1 要求 UUID 字符串，见 UuidIds。
    private static final String DAMAGE_ID = UuidIds.of("frb_cloud_grace_damage");

    public CloudGraceEffect() {
        super(MobEffectCategory.HARMFUL, 0x3a59d8, "cloud_grace");
        this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                DAMAGE_ID, -0.75D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            if (entity.hasEffect(MobEffects.REGENERATION)) {
                entity.hurt(entity.level().damageSources().magic(), 2.0F);
            } else {
                entity.hurt(entity.level().damageSources().magic(), 1.0F);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }
}
