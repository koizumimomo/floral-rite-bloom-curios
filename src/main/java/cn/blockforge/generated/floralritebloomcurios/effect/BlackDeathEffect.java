package cn.blockforge.generated.floralritebloomcurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import cn.blockforge.generated.floralritebloomcurios.entity.DeathFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 黑死病：每 0.5 秒流失生命（无视护甲，可致死）。对玩家不致死，至少保留 1 点生命。
 */
public class BlackDeathEffect extends FRBEffect {
    public BlackDeathEffect() {
        super(MobEffectCategory.HARMFUL, 0x5a3d6e, "black_death");
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof FriliasEntity || entity instanceof DeathFlowerEntity) {
            entity.removeEffect(this);
            return;
        }
        if (!entity.level().isClientSide) {
            float dmg = 1.0F + amplifier;
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                float keep = Math.min(dmg, Math.max(entity.getHealth() - 1.0F, 0.0F));
                if (keep > 0.0F) {
                    entity.hurt(entity.level().damageSources().generic(), keep);
                }
            } else {
                entity.hurt(entity.level().damageSources().magic(), dmg);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
