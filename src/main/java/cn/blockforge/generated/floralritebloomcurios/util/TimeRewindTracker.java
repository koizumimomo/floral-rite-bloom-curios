package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * I2 时间倒流（r67）：玩家位置历史环形缓冲。
 * boss 每 {@link #SAMPLE_INTERVAL} tick 采样一次周围玩家的位置与视角，
 * 倒流时取 60 tick（3 秒）前的样本。只记录位置/视角，不记录任何状态
 * （生命、效果、背包均不受回溯影响）。
 */
public final class TimeRewindTracker {

    /** 采样间隔（tick）。 */
    public static final int SAMPLE_INTERVAL = 5;
    /** 保留样本数：20 × 5 tick = 100 tick 历史（倒流取 12 个样本前 = 60 tick）。 */
    public static final int MAX_SAMPLES = 20;
    /** 倒流目标：60 tick（3 秒）前。 */
    public static final int REWIND_TICKS = 60;

    /** 单个位置样本。 */
    public record Sample(double x, double y, double z, float yaw, float pitch) {}

    private final Map<UUID, Deque<Sample>> history = new HashMap<>();

    /** 每 5 tick 调一次：记录 boss 周围 64 格内所有存活玩家的位置。 */
    public void sample(ServerLevel level, Entity anchor) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                anchor.getBoundingBox().inflate(64.0D))) {
            if (p.isSpectator()) continue;
            Deque<Sample> dq = this.history.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            dq.addLast(new Sample(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
            while (dq.size() > MAX_SAMPLES) {
                dq.pollFirst();
            }
        }
        // 清掉已离线/换维度/远离 boss 太久的条目，防止长战斗内存缓慢增长
        this.history.keySet().removeIf(id -> {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            return p == null || p.level() != level || !p.isAlive()
                    || p.distanceToSqr(anchor) > 128.0D * 128.0D;
        });
    }

    /**
     * 取玩家 {@code agoTicks} 前的位置样本（向旧方向对齐采样粒度）；历史不足时取最早一条，无历史返回 null。
     */
    public Sample lookup(UUID who, int agoTicks) {
        Deque<Sample> dq = this.history.get(who);
        if (dq == null || dq.isEmpty()) return null;
        int back = Math.max(1, agoTicks / SAMPLE_INTERVAL);
        if (back > dq.size()) back = dq.size();
        Iterator<Sample> it = dq.descendingIterator();
        Sample s = null;
        for (int i = 0; i < back && it.hasNext(); i++) {
            s = it.next();
        }
        return s;
    }
}
