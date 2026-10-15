package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务端看门狗：Server thread 每次开 tick 喂一次狗（ServerTickEvent.START）；
 * 独立 daemon 监控线程发现超过 8 秒没人喂狗（死循环导致后续 tick 永不开始 /
 * 锁死锁导致 tick 挂起——两种都覆盖），就把全部线程栈转储到 logs/frb_watchdog_<时间戳>.log。
 * 转储在监控线程执行，Server thread 仍卡在原地，栈即真凶。
 * （前两版分别漏抓"卡住中无法 dump"和"WAITING 状态被排除"；暂停菜单会照常发 tick 事件，不会误报。）
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FRBWatchdog {

    private static final long FROZEN_NANOS = 8_000_000_000L;
    private static volatile long lastFeed = System.nanoTime();
    private static final AtomicBoolean DUMPING = new AtomicBoolean(false);
    private static volatile boolean started;

    /** 服务器创建时即启动监控：世界加载阶段（首个 tick 之前）的卡死也能抓到。
     *  r85：看门狗暂时停用（早期排查 boss 战卡死用），需要时把两个 @SubscribeEvent 注解恢复即可。 */
    // @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        startMonitor();
    }

    // @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            lastFeed = System.nanoTime();
            startMonitor();
        }
    }

    private static synchronized void startMonitor() {
        if (started) return;
        started = true;
        Thread monitor = new Thread(FRBWatchdog::monitorLoop, "FRB-Watchdog");
        monitor.setDaemon(true);
        monitor.start();
    }

    private static void monitorLoop() {
        while (true) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                return;
            }
            long frozen = System.nanoTime() - lastFeed;
            if (frozen > FROZEN_NANOS && DUMPING.compareAndSet(false, true)) {
                dump(frozen);
                lastFeed = System.nanoTime();
                DUMPING.set(false);
            }
        }
    }

    private static void dump(long frozenNanos) {
        Path file = Path.of("logs", "frb_watchdog_"
                + LocalDateTime.now().toString().replace(':', '-') + ".log");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            w.println("No server tick for ~" + (frozenNanos / 1_000_000_000L)
                    + "s (dead loop or deadlock) at " + Instant.now());
            w.println("Full thread dump (Server thread stack top = frozen code):");
            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                w.println();
                w.println("=== " + e.getKey().getName() + " [" + e.getKey().getState() + "] ===");
                for (StackTraceElement el : e.getValue()) {
                    w.println("  at " + el);
                }
            }
            LogUtils.getLogger().error("[FRBWatchdog] No server tick for ~{}s, thread dump -> {}",
                    frozenNanos / 1_000_000_000L, file);
        } catch (IOException ignored) {
        }
    }

    private FRBWatchdog() {}
}
