package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 浸泡（云雨阶段万变水波 debuff）：受到的所有伤害 +25%。
 *
 * 与 {@link DampnessEffect} 同样为纯标记效果——实际伤害倍增由
 * CharmEvents.onLivingHurt 钩子处理：检测玩家身上是否有浸泡，
 * 命中则 amount *= 1.25。
 *
 * 玩家踏入万变水波地面时获得 5 秒该效果（可刷新），并叠加 1 层雨之华。
 */
public class SoggyEffect extends FRBEffect {

    /** 伤害提升系数：1.25 = +25%。 */
    public static final float DAMAGE_MULTIPLIER = 1.25F;

    public SoggyEffect() {
        super(MobEffectCategory.HARMFUL, 0x4fc3f7, "soggy");
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 纯标记，无 tick 副作用
    }
}
