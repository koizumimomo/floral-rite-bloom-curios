package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 潮湿（云雨阶段 debuff）：受到落雷伤害时 ×1.5。
 *
 * 该 effect 本身不做事——实际扣血倍增逻辑由 FloralRiteBloom 中的
 * LivingHurtEvent 钩子处理：检测 source 类型 + 玩家身上是否有潮湿，命中则 amount *= 1.5。
 *
 * 这个类仅作为效果注册载体 + GUI 图标 + 状态保存。
 */
public class DampnessEffect extends FRBEffect {

    public DampnessEffect() {
        super(MobEffectCategory.HARMFUL, 0x5f8dff, "dampness");
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // 不需要 tick 副作用
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 不需要服务端 tick，仅占位。
    }
}