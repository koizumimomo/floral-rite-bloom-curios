package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * BOSS 机制实体的基类（extends Entity）：免疫一切伤害与击退。
 *
 * 用法：地刺 / 虚空裂隙 / 花之摇篮法阵 / 积雨云 等"非生物"机制实体
 * 都应当 extends MechanicEntity。
 *
 * 关键拦截点：
 * - hurt() 返回 false：拒绝所有伤害
 * - isInvulnerable() / isInvulnerableTo() 返回 true：通用无敌
 *
 * 注：canBeAffected / addEffect 属于 LivingEntity 接口，生物型机制实体（如死亡之花）
 * 应当 overrides 自身方法。
 */
public abstract class MechanicEntity extends Entity {

    public MechanicEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}