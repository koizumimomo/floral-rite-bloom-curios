package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * r77：花灵主动技能冷却 HUD。
 *
 * <p>三种技能（按用户截图 #2 提到的三联能力）共享一套堆叠渲染：
 * <ul>
 *   <li>frost_chime  寒风铃·回溯（FrostChimeRecall）—— 30 秒</li>
 *   <li>astral_bloom 星界花·传送（AstralBlink）—— 30 秒</li>
 *   <li>floral_eternal 万花万草之灵·终末花环（TerminusWreath）—— 180 秒</li>
 * </ul>
 * </p>
 *
 * <p>显示规则：
 * <ol>
 *   <li>每个技能独立记录"按下键的时刻 + 冷却时长"；</li>
 *   <li>剩余冷却 > 0 才显示在 HUD；</li>
 *   <li>冷却归零自动从堆叠移除，下一个 HUD 自动上移（模仿 HUD+ 的效果叠加显示）；</li>
 *   <li>按用户要求：单个图标宽 ≈ 玩家血量条一半；位置由 config 决定；编辑模式（Hold Key + I）
 *   解锁鼠标左键拖动修改坐标。</li>
 * </ol>
 *
 * <p>注意：服务端的 NBT 才是真实冷却，客户端这个仅供显示。即使网络延迟按下不会让玩家绕过
 * 服务端的 useCooldown 判定。</p>
 */
public final class AbilityHudOverlay implements IGuiOverlay {

    /** 玩家血条宽度（按 1.0 倍 812 像素算一半 ≈ 91 px = 单个图标宽）。 */
    private static final int ICON_W = 91;
    /** r78：图标高度缩短到 18 px（原 24 太满，盖住边框花纹）。 */
    private static final int ICON_H = 18;
    /** 三个图标垂直堆叠间距。 */
    private static final int ICON_PAD = 1;
    /** r78：fill 内边距（左右各留 2 px，让 frame 边缘花纹不被覆盖）。 */
    private static final int FILL_PAD = 2;

    private static final ResourceLocation FRAME_FROST =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/frost_progress_frame.png");
    private static final ResourceLocation FILL_FROST =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/frost_progress_fill.png");
    private static final ResourceLocation FRAME_ASTRAL =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/astral_progress_frame.png");
    private static final ResourceLocation FILL_ASTRAL =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/astral_progress_fill.png");
    private static final ResourceLocation FRAME_FLORAL =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/floral_progress_frame.png");
    private static final ResourceLocation FILL_FLORAL =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/floral_progress_fill.png");

    /**
     * 客户端本地记录每个技能的"按下时刻 + 冷却时长"。
     * 静态以便 AbilityHudOverlay / 任何按键处理器共享。游戏重载后清空（不是存档数据）。
     */
    public static final class LocalCooldown {
        public long usedAtMs;   // System.currentTimeMillis
        public long durationMs; // 冷却总时长
        public LocalCooldown(long usedAtMs, long durationMs) {
            this.usedAtMs = usedAtMs;
            this.durationMs = durationMs;
        }
        public float progress01() {
            long elapsed = System.currentTimeMillis() - usedAtMs;
            return Math.max(0.0F, Math.min(1.0F, 1.0F - elapsed / (float) durationMs));
        }
        public boolean isFinished() {
            return System.currentTimeMillis() - usedAtMs >= durationMs;
        }
    }

    /** 客户端本地冷却表（注意：只用于显示，服务端校验独立完成）。 */
    public static final java.util.Map<Kind, LocalCooldown> COOLDOWNS = new java.util.EnumMap<>(Kind.class);

    public enum Kind {
        FROST_RECALL(cn.blockforge.generated.floralritebloomcurios.abilities.FrostChimeRecall.COOLDOWN_TICKS),
        ASTRAL_BLINK(cn.blockforge.generated.floralritebloomcurios.abilities.AstralBlink.BLINK_COOLDOWN),
        TERMINUS_WREATH(cn.blockforge.generated.floralritebloomcurios.abilities.TerminusWreath.COOLDOWN_TICKS);

        public final long cooldownTicks;
        Kind(long cooldownTicks) { this.cooldownTicks = cooldownTicks; }
        public long cooldownMs() { return cooldownTicks * 50L; }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().player == null) return;
        if (!FRBConfig.showHud()) return;

        // 清理过期的本地冷却
        COOLDOWNS.entrySet().removeIf(e -> e.getValue().isFinished());

        // 按用户要求的顺序堆叠（FROST → ASTRAL → FLORAL）。每个 ICON_H + ICON_PAD。
        int x = screenWidth / 2 + FRBConfig.hudX();
        int y = screenHeight - FRBConfig.hudY();
        // 由远及近绘制，让第一个技能在最底（与 HUD+ 效果一致）
        java.util.List<Kind> active = new java.util.ArrayList<>();
        for (Kind k : Kind.values()) if (COOLDOWNS.containsKey(k)) active.add(k);
        // bottom-up 堆叠：active.get(0) 显示在最底
        for (int i = 0; i < active.size(); i++) {
            Kind k = active.get(i);
            int drawY = y - (i + 1) * (ICON_H + ICON_PAD);
            drawEntry(graphics, x, drawY, k, COOLDOWNS.get(k));
        }
    }

    private void drawEntry(GuiGraphics graphics, int x, int y, Kind k, LocalCooldown cd) {
        ResourceLocation frame;
        ResourceLocation fill;
        switch (k) {
            case FROST_RECALL -> { frame = FRAME_FROST; fill = FILL_FROST; }
            case ASTRAL_BLINK -> { frame = FRAME_ASTRAL; fill = FILL_ASTRAL; }
            case TERMINUS_WREATH -> { frame = FRAME_FLORAL; fill = FILL_FLORAL; }
            default -> { return; }
        }
        // 进度条 fill（按剩余冷却比例缩减宽度，留 FILL_PAD 让 frame 边缘花纹可见）
        int fillW = Math.max(0, Math.min(ICON_W - FILL_PAD * 2, (int) ((ICON_W - FILL_PAD * 2) * cd.progress01())));
        if (fillW > 0) {
            graphics.blit(fill, x + FILL_PAD, y, 0, 0, fillW, ICON_H, ICON_W - FILL_PAD * 2, ICON_H);
        }
        // 边框 frame 全宽
        RenderSystem.enableBlend();
        graphics.blit(frame, x, y, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        // 倒计时文字
        long leftMs = cd.durationMs - (System.currentTimeMillis() - cd.usedAtMs);
        long sec = Math.max(0, leftMs / 1000);
        String text = sec > 0 ? (sec + "s") : "ok";
        graphics.drawString(Minecraft.getInstance().font, text,
                x + ICON_W / 2 - Minecraft.getInstance().font.width(text) / 2,
                y + ICON_H / 2 - 4, 0xFFFFFFFF, false);
    }

    /** 给指定技能刷一次"按下"时间（按键处理器在 client tick 检测到按下时调）。 */
    public static void markUse(Kind kind) {
        COOLDOWNS.put(kind, new LocalCooldown(System.currentTimeMillis(), kind.cooldownMs()));
    }

    public AbilityHudOverlay() {}
}