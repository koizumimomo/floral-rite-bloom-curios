package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 霜蚀：每秒 1 点细雪（冻结）伤害，附带移速 -20%、攻击 -50%。
 * 与原版细雪伤害一致（freeze 伤害类型，被免疫细雪的效果可抵消）。
 */
public class FrostbiteEffect extends FRBEffect {
    // 1.20.1 的 MobEffect#addAttributeModifier 要求 id 必须是合法 UUID 字符串
    // （内部走 UUID.fromString），这里用可读名派生稳定 UUID。
    private static final String SPEED_ID = UuidIds.of("frb_frostbite_speed");
    private static final String DAMAGE_ID = UuidIds.of("frb_frostbite_damage");

    public FrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x9fdcff, "frostbite");
        this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                SPEED_ID, -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                DAMAGE_ID, -0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.hurt(entity.level().damageSources().freeze(), 1.0F + amplifier);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = Math.max(20 - amplifier * 5, 5);
        return duration % interval == 0;
    }
}
