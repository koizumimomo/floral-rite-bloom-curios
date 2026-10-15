package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * r77：HUD 编辑界面（参考 Black_Mage 的 HudEditScreen 风格）。
 * Shift + H 进入：解锁鼠标，可拖拽调整技能冷却 HUD 的位置。
 * 关闭时写入 FRBConfig 并保存到磁盘。
 */
public class HudEditScreen extends Screen {
    private static final ResourceLocation EDIT_FRAME =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/gui/hud/frost_progress_frame.png");

    private boolean dragging = false;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private int dragStartHudX;
    private int dragStartHudY;

    /** 单条 HUD 渲染尺寸（与 AbilityHudOverlay 一致：91x24）。 */
    private static final int ENTRY_W = 91;
    private static final int ENTRY_H = 24;
    /** 演示总高度（三个图标叠加）= 3 个高度 + 2 个间距 + padding */
    private static final int BLOCK_PAD = 4;
    private static final int BLOCK_H = 3 * ENTRY_H + 2 * 2 + BLOCK_PAD * 2;

    public HudEditScreen() {
        super(Component.translatable("frb.hud.edit_mode"));
    }

    @Override
    protected void init() {
        // Screen 自动解锁鼠标
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 半透明背景
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // HUD 当前位置（与 AbilityHudOverlay.render 同一公式）
        int baseX = this.width / 2 + FRBConfig.hudX();
        int baseY = this.height - FRBConfig.hudY();
        // 演示一个"未来位置"块（让用户能直接看到 HUD 大致位置）
        int blockX = baseX - BLOCK_PAD;
        int blockY = baseY - BLOCK_H;
        int blockW = ENTRY_W + BLOCK_PAD * 2;
        int blockH = BLOCK_H;
        graphics.renderOutline(blockX, blockY, blockW, blockH, 0xFFFFFFFF);
        // 把 frame 贴一次当示例纹理
        graphics.blit(EDIT_FRAME, blockX + BLOCK_PAD, blockY + BLOCK_PAD,
                0, 0, ENTRY_W, ENTRY_H, ENTRY_W, ENTRY_H);
        graphics.blit(EDIT_FRAME, blockX + BLOCK_PAD, blockY + BLOCK_PAD + ENTRY_H + 2,
                0, 0, ENTRY_W, ENTRY_H, ENTRY_W, ENTRY_H);
        graphics.blit(EDIT_FRAME, blockX + BLOCK_PAD, blockY + BLOCK_PAD + 2 * (ENTRY_H + 2),
                0, 0, ENTRY_W, ENTRY_H, ENTRY_W, ENTRY_H);

        // 提示文字
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font,
                Component.translatable("frb.hud.edit_mode"),
                this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("X: " + FRBConfig.hudX() + "  Y: " + FRBConfig.hudY())
                        .withStyle(ChatFormatting.AQUA),
                this.width / 2, 24, 0xAAFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int baseX = this.width / 2 + FRBConfig.hudX();
            int baseY = this.height - FRBConfig.hudY();
            int blockX = baseX - BLOCK_PAD;
            int blockY = baseY - BLOCK_H;
            if (mouseX >= blockX && mouseX <= blockX + ENTRY_W + BLOCK_PAD * 2
                    && mouseY >= blockY && mouseY <= blockY + BLOCK_H) {
                dragging = true;
                dragStartMouseX = (int) mouseX;
                dragStartMouseY = (int) mouseY;
                dragStartHudX = FRBConfig.hudX();
                dragStartHudY = FRBConfig.hudY();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            int dx = (int) mouseX - dragStartMouseX;
            int dy = (int) mouseY - dragStartMouseY;
            FRBConfig.setHudX(dragStartHudX + dx);
            FRBConfig.setHudY(dragStartHudY - dy);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}