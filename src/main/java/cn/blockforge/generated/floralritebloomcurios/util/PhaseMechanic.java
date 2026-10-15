package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Boss 机制调度单元：每阶段注册若干 PhaseMechanic，
 * 由 FriliasEntity.aiStep() 在 timer % cooldown == 0 时统一调度。
 *
 * 解锁等级（mechanicTier）由 FriliasEntity#hurt 按血量阈值（70% / 40%）推进；
 * 应使用 #unlockedAt() 决定该机制何时才能投入战场。
 */
public interface PhaseMechanic {

    /** 机制所属阶段（0=自然, 1=焚尽, 2=霜华, 3=云雨, 4=天空, 5=虚空, 6=枯萎）。 */
    int phase();

    /** 触发间隔（tick）。 */
    int cooldown();

    /** 机制开始生效所需最低 mechanicTier；0 表示阶段一开始即可，1 表示 70% 血量之后，2 表示 40% 之后。 */
    default int unlockedAt() {
        return 0;
    }

    /** 服务端 tick 钩子；timer 已经被外部步进到当前值。 */
    void tick(FriliasEntity boss, ServerLevel level, int timer);

    /**
     * 多人模式开关：默认所有机制在单人 / 多人下都释放。
     * 若重写返回 false，则要求该 boss 战斗内 fightersInRange >= 2 才允许释放；
     * 不满足时该 tick 不触发但不重置 cd（cd 周期内只检测一次）。
     */
    default boolean unlockedInMultiplayer() {
        return true;
    }
}