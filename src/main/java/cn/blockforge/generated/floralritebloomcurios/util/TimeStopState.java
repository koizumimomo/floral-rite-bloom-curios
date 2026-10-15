package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * r73 · L5 时间静止状态：单一权威 static 状态，Mixins 都从这里读「当前维度是否处于时停」。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>只有 boss（{@link cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity}）
 *       会调用 <code>set/clear</code>，所以不需要 SavedData 持久化（梦幻终焉那种法术场景才需要）。</li>
 *   <li>用 IdentityHashMap 索引 Level 实例，多人模式每个维度独立判断，跨维度不串扰。</li>
 *   <li>Entity 列表用「快照时点位置」缓存，Mixins 想冻结某个具体实体时可以直接读这里。快照列表只存 UUID + 位置，
 *       不强行锁 entity 引用，避免反复 GC 跟踪。</li>
 * </ul>
 * <p>
 * <b>不要放在 cn.blockforge.generated.floralritebloomcurios.mixin.* 下</b>：
 * mixins.fl_rite.json 把 <code>cn.blockforge.generated.floralritebloomcurios.mixin.*</code> 声明为
 * 只能装 @Mixin 类的包。TimeStopState 是普通类，Mixin 运行时一旦尝试加载就报
 * <code>IllegalClassLoadError: ... is in a defined mixin package ... and cannot be referenced directly</code>。
 */
public final class TimeStopState {
    /**
     * 当前处于时停的维度集合（key=Level 实例，value=时停开始时的 gameTime）。
     * Mixin 端用 {@link #isActive(Level)} 查询，避免泄漏 Level 实例。
     */
    private static final Map<Level, Long> ACTIVE_LEVELS = Collections.synchronizedMap(new IdentityHashMap<>());

    /**
     * 客户端时停标志（多人模式下服务端的 ACTIVE_LEVELS 不会同步到客户端，
     * 由 BossFxPacket 的 FX_GRAY + TIME_PHASE_COLOR 触发时由 FRBBossFxClient 设置）。
     */
    private static volatile boolean clientActive = false;

    /**
     * 时停中所有被冻结实体的「快照位置」（key=UUID，value=时停开始时的位置）。
     * 注意：Mixins 只能读取，写入由 boss 的 {@code tickTimeStop()} 维护。
     */
    private static final Map<java.util.UUID, Vec3> FROZEN_POSITIONS = Collections.synchronizedMap(new java.util.HashMap<>());

    private TimeStopState() {}

    /** boss 在 <code>startTimeStop()</code> 里调用：登记维度 + 拉取冻结体位置。 */
    public static void setActive(Level level, long gameTime) {
        ACTIVE_LEVELS.put(level, gameTime);
    }

    /** boss 在 <code>tickTimeStop()</code> 倒数到 0 时调用：清空维度标记。 */
    public static void clearActive(Level level) {
        if (level == null) return;
        ACTIVE_LEVELS.remove(level);
        FROZEN_POSITIONS.clear();
    }

    /** Mixin 端查询：给定 Level 当前是否处于时停（同时检查服务端 map 与客户端标志）。 */
    public static boolean isActive(Level level) {
        if (clientActive) return true;
        return level != null && ACTIVE_LEVELS.containsKey(level);
    }

    /** 客户端设置时停激活状态（由 FRBBossFxClient 在收到 FX_GRAY+TIME_PHASE_COLOR 时调用）。 */
    public static void setClientActive(boolean active) {
        clientActive = active;
    }

    /** 时停开始时把冻结体位置灌进来（boss 调一次）。 */
    public static void putFrozenPositions(Map<java.util.UUID, Vec3> snapshot) {
        FROZEN_POSITIONS.putAll(snapshot);
    }

    /** 读单个实体的冻结位置；返回 null 表示「没冻 / 不需要冻」。 */
    public static Vec3 getFrozenPosition(java.util.UUID uuid) {
        return FROZEN_POSITIONS.get(uuid);
    }

    /** 是否被冻结（出现在快照表里）。 */
    public static boolean isFrozen(java.util.UUID uuid) {
        return FROZEN_POSITIONS.containsKey(uuid);
    }

    /** 调试用：当前时停维度数。 */
    public static int activeCount() {
        return ACTIVE_LEVELS.size();
    }

    /** 调试用：当前冻结实体数。 */
    public static int frozenCount() {
        return FROZEN_POSITIONS.size();
    }

    /** 调试用：所有当前时停维度（只读）。 */
    public static Set<Level> activeLevels() {
        return Collections.unmodifiableSet(ACTIVE_LEVELS.keySet());
    }
}