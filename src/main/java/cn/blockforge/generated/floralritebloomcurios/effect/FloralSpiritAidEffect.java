package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * 花灵之助（B1 法阵 buff）：+20% 攻击伤害。
 *
 * 由花之摇篮落花命中地面时给场上玩家附加（持续 60s）。
 * 玩家处于法阵内提前获得（机制详见 Mechanics#flowerCradle）。
 */
public class FloralSpiritAidEffect extends FRBEffect {

    private static final UUID ID = UuidIds.uuid("frb_floral_spirit_aid_attack");

    public FloralSpiritAidEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xffd97a, "floral_spirit_aid");
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        var attr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attr == null) return;
        attr.removeModifier(ID);
        attr.addPermanentModifier(new AttributeModifier(ID, "frb.floral_spirit_aid",
                0.20D, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        var attr = attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(ID);
    }
}