package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 自动把「花灵伤害」的颜色条目写进训练假人（dummmmmmy）的客户端配置
 * （config/dummmmmmy-client.toml 的 [visuals.damage_type_colors] 小节）。
 * dummy 的颜色表在 config 加载时一次性读入内存缓存——因此必须赶在它加载前改文件：
 * mod 构造阶段早于所有 config 加载，写完本局立即生效；
 * 若文件尚未生成（首次安装 dummy），退而在 ClientSetup、以及进入主菜单的第一帧兜底补写（下一局生效）。
 * 写入幂等：已有 floral 条目则跳过，绝不动其它内容。
 */
public final class DummyDamageColorInjector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DUMMY_MODID = "dummmmmmy";
    private static final String SECTION = "[visuals.damage_type_colors]";
    /** 花灵伤害显示色：RGB #FF00C8（浅粉），按 dummy 的 #AARRGGBB 格式对齐现有条目（alpha 00）。 */
    private static final String ENTRY = "\"floral_rite_bloom_curios:floral\" = \"#00FF00C8\"";
    private static boolean done;
    private static boolean tickHooked;

    private DummyDamageColorInjector() {}

    /** mod 构造时调用：早于 dummy 的 config 加载 → 本局立即生效。 */
    public static void injectEarly() {
        if (!FMLEnvironment.dist.isClient() || !ModList.get().isLoaded(DUMMY_MODID)) return;
        tryInject("构造期");
    }

    /** ClientSetup 时兜底（dummy 刚装上、构造期文件还未生成的情形）。 */
    public static void injectAtClientSetup() {
        if (!FMLEnvironment.dist.isClient() || !ModList.get().isLoaded(DUMMY_MODID)) return;
        tryInject("ClientSetup");
        if (!done && !tickHooked) {
            tickHooked = true;
            MinecraftForge.EVENT_BUS.addListener(DummyDamageColorInjector::onClientTick);
        }
    }

    /** 主菜单第一帧兜底：dummy 的默认配置此时已生成。补写需下一局生效，日志说明。 */
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || done) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen) || mc.level != null) return;
        tryInject("主菜单兜底");
    }

    /** 读 toml → 缺条目则插入 [visuals.damage_type_colors] 小节首行。幂等且不改其它内容。 */
    private static void tryInject(String phase) {
        if (done) return;
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve("dummmmmmy-client.toml");
            if (!Files.exists(file)) return;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            boolean inSection = false;
            int sectionIdx = -1;
            for (int i = 0; i < lines.size(); i++) {
                String t = lines.get(i).trim();
                if (t.startsWith("[") && t.endsWith("]")) {
                    inSection = t.equals(SECTION);
                    if (inSection && sectionIdx < 0) sectionIdx = i;
                    continue;
                }
                if (inSection && t.contains("floral_rite_bloom_curios:floral")) {
                    done = true; // 已有条目
                    return;
                }
            }
            if (sectionIdx < 0) return; // 小节不存在（异常配置），不动它
            lines.add(sectionIdx + 1, "\t" + ENTRY);
            Files.write(file, lines, StandardCharsets.UTF_8);
            done = true;
            LOGGER.info("[FRB] 已在 dummmmmmy-client.toml 注册花灵伤害颜色 #FF00C8（{}写入）", phase);
        } catch (Exception e) {
            done = true; // 防止每帧重试刷日志；下一局会重试
            LOGGER.warn("[FRB] 写入 dummmmmmy 颜色配置失败（{}）：{}", phase, e.toString());
        }
    }
}
