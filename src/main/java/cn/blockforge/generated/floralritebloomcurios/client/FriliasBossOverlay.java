package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 通过血条自身的阶段语言键识别，兼容同屏多 Boss 与重新加入。 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FriliasBossOverlay {
    /** 前 7 阶段（自然~枯萎）血条外框：textures/gui/boss/{phase}.png（320×46，内填充由代码画色块）。 */
    private static final ResourceLocation[] FRAMES = java.util.Arrays.stream(FriliasEntity.PHASE_KEYS)
            .map(key -> new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/boss/" + key + ".png"))
            .toArray(ResourceLocation[]::new);
    /** r96：时间阶段血条用独立的 frame + fill 贴图（328×38 / 256×16，内填充为贴图而非色块）。 */
    private static final ResourceLocation TIME_FRAME = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/gui/boss/time_progress_frame.png");
    private static final ResourceLocation TIME_FILL = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/gui/boss/time_progress_fill.png");
    /** 时间阶段血条外框宽高（像素）。 */
    private static final int TIME_FRAME_W = 328;
    private static final int TIME_FRAME_H = 38;
    /** 内填充区在外框内的相对坐标（与读条 HUD 一致：36~292 / 11~27）。 */
    private static final float TIME_FILL_X0 = 36.0F / TIME_FRAME_W;
    private static final float TIME_FILL_X1 = 292.0F / TIME_FRAME_W;
    private static final float TIME_FILL_Y0 = 11.0F / TIME_FRAME_H;
    private static final float TIME_FILL_Y1 = 27.0F / TIME_FRAME_H;

    @SubscribeEvent
    public static void draw(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!(event.getBossEvent().getName().getContents() instanceof TranslatableContents text)) return;
        int phase = -1;
        // r96：遍历全部 PHASE_KEYS（含 time），原 PHASE_COUNT=7 会漏掉时间阶段
        for (int i = 0; i < FriliasEntity.PHASE_KEYS.length; i++) {
            if (text.getKey().equals("frb.boss.bar." + FriliasEntity.PHASE_KEYS[i])) { phase = i; break; }
        }
        if (phase < 0) return;
        event.setCanceled(true);
        var gui = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;

        if (phase == 7) {
            // 时间阶段：用 frame + fill 贴图渲染
            float scale = Math.min(1.0F, (gui.guiWidth() - 12) / (float) TIME_FRAME_W);
            int y = event.getY();
            gui.pose().pushPose();
            gui.pose().translate((gui.guiWidth() - TIME_FRAME_W * scale) / 2.0F, y, 0);
            gui.pose().scale(scale, scale, 1);
            // 背景暗条
            int fillX0 = (int) (TIME_FILL_X0 * TIME_FRAME_W);
            int fillX1 = (int) (TIME_FILL_X1 * TIME_FRAME_W);
            int fillY0 = (int) (TIME_FILL_Y0 * TIME_FRAME_H);
            int fillY1 = (int) (TIME_FILL_Y1 * TIME_FRAME_H);
            gui.fill(fillX0, fillY0, fillX1, fillY1, 0xFF140E1B);
            // 内填充：按进度裁剪 fill 贴图宽度
            float prog = Mth.clamp(event.getBossEvent().getProgress(), 0.0F, 1.0F);
            int fillW = (int) ((fillX1 - fillX0) * prog);
            if (fillW > 0) {
                // blit 裁剪：从 fill 贴图左侧取 fillW 像素宽的条
                gui.blit(TIME_FILL, fillX0, fillY0, 0F, 0F, fillW, fillY1 - fillY0,
                        (int) (TIME_FILL_X1 * TIME_FRAME_W) - fillX0, fillY1 - fillY0);
            }
            // 外框
            gui.blit(TIME_FRAME, 0, 0, 0F, 0F, TIME_FRAME_W, TIME_FRAME_H, TIME_FRAME_W, TIME_FRAME_H);
            gui.drawCenteredString(font, event.getBossEvent().getName(), TIME_FRAME_W / 2, -9, 0xFFFFFF);
            gui.pose().popPose();
            event.setIncrement((int) (TIME_FRAME_H * scale) + 14);
        } else {
            // 前 7 阶段：{phase}.png 外框 + 代码画色块内填充
            float scale = Math.min(1.0F, (gui.guiWidth() - 12) / 320.0F);
            int y = event.getY();
            gui.pose().pushPose();
            gui.pose().translate((gui.guiWidth() - 320 * scale) / 2.0F, y, 0);
            gui.pose().scale(scale, scale, 1);
            gui.fill(25, 23, 295, 32, 0xFF140E1B);
            int width = Mth.clamp((int) (270 * event.getBossEvent().getProgress()), 0, 270);
            int color = phase == 6 ? 0x9e4dbc : FriliasEntity.PHASE_COLORS[phase];
            if (width > 0) {
                gui.fill(25, 23, 25 + width, 32, 0xFF000000 | color);
                gui.fill(25, 23, 25 + width, 25, 0x55FFFFFF);
            }
            gui.blit(FRAMES[phase], 0, 0, 0F, 0F, 320, 46, 320, 46);
            gui.drawCenteredString(font, event.getBossEvent().getName(), 160, -9, 0xFFFFFF);
            gui.pose().popPose();
            event.setIncrement((int) (46 * scale) + 14);
        }
    }

    // r69（bug2）：读条 HUD 已移到 boss 头顶世界空间渲染（FriliasRenderer#drawChargeBar），
    // 随实体一起消失——修复"击败 boss 后读条贴图残留屏幕底部"的问题。

    private FriliasBossOverlay() {}
}
