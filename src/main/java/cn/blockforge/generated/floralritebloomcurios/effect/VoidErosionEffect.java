package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * 虚空侵蚀（G2 地块 debuff）：每层 -1% 最大生命 / 攻击伤害 / 移速，最高 20 层。
 *
 * 离开侵蚀地块后每秒层数 -1（在 effect.applyEffectTick 里检查 lastTouched 时间），
 * 玩家持续踩在 void_erosion 方块上时由 VoidErosionBlock.stepOn 持续续期 + 加重叠层。
 */
public class VoidErosionEffect extends FRBEffect {

    private static final UUID MAX_HP_ID = UuidIds.uuid("frb_void_erosion_max_hp");
    private static final UUID ATK_ID = UuidIds.uuid("frb_void_erosion_attack");
    private static final UUID SPD_ID = UuidIds.uuid("frb_void_erosion_speed");
    private static final float PER_STACK = -0.01F;
    private static final String TAG_LAST = "frb_void_erosion_last";

    public VoidErosionEffect() {
        super(MobEffectCategory.HARMFUL, 0x6b2a8f, "void_erosion");
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (entity.tickCount % 20 != 0) return;
        double mul = 1.0D + PER_STACK * (amplifier + 1);
        apply(entity, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, MAX_HP_ID, mul);
        apply(entity, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATK_ID, mul);
        apply(entity, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, SPD_ID, mul);
        // 离开侵蚀地块后每秒 -1 层
        long now = entity.level().getGameTime();
        long last = entity.getPersistentData().getLong(TAG_LAST);
        if (last > 0 && now - last > 20 && amplifier > 0) {
            int newAmp = amplifier - 1;
            entity.removeEffect(this);
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(this,
                    600, newAmp, false, false, true));
        }
    }

    private void apply(LivingEntity entity, Attribute attr, UUID id, double mul) {
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance == null) return;
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, "frb.void_erosion",
                mul, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        var attr = attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MAX_HP_ID);
        attr = attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(ATK_ID);
        attr = attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(SPD_ID);
    }
}