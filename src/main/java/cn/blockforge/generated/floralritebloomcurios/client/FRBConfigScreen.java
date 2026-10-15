package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * r85：游戏内 Boss 配置界面（模组列表 → Floral Rite: Bloom → Config）。
 * <p>
 * 顶部分类页签（通用/防御/基础CD/8 阶段），中间条目滚动列表：
 * 布尔/枚举 = 按钮切换，数值 = 输入框（回车或失焦生效，越界自动钳制）。
 * 修改立即写入 config/frb_boss.toml（Forge 自动保存并重载）。
 */
public class FRBConfigScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int HEADER_H = 48;
    private static final int FOOTER_H = 28;
    private static final int VALUE_W = 150;

    private final Screen parent;
    private final List<FRBConfig.Category> categories = FRBConfig.categories();
    private int tab = 0;
    private double scroll;
    /** 当前页的控件（Button / EditBox），与 entries 一一对应。 */
    private final List<net.minecraft.client.gui.components.AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    public FRBConfigScreen(Screen parent) {
        super(Component.literal("Floral Rite: Bloom 配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        rowWidgets.clear();
        tabButtons.clear();

        // 顶部分类页签（两排自动换行）
        int x = 8, y = 24;
        for (int i = 0; i < categories.size(); i++) {
            FRBConfig.Category cat = categories.get(i);
            int w = this.font.width(cat.name()) + 14;
            if (x + w > this.width - 8) {
                x = 8;
                y += 20;
            }
            final int idx = i;
            Button b = Button.builder(Component.literal(cat.name()), btn -> switchTab(idx))
                    .bounds(x, y, w, 16).build();
            tabButtons.add(b);
            addRenderableWidget(b);
            x += w + 4;
        }

        buildRows();

        addRenderableWidget(Button.builder(Component.literal("完成"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 24, 100, 18).build());
    }

    private void switchTab(int idx) {
        commitEdits();
        this.tab = idx;
        this.scroll = 0;
        init();
    }

    private void buildRows() {
        int valueX = this.width / 2 + 40;
        for (FRBConfig.Entry e : categories.get(tab).entries()) {
            if (e.kind == FRBConfig.Entry.Kind.BOOL) {
                Button b = Button.builder(boolText(e), btn -> {
                    e.cycle();
                    btn.setMessage(boolText(e));
                }).bounds(valueX, 0, VALUE_W, 18).build();
                rowWidgets.add(b);
                addRenderableWidget(b);
            } else if (e.kind == FRBConfig.Entry.Kind.ENUM) {
                Button b = Button.builder(Component.literal(e.getText()), btn -> {
                    e.cycle();
                    btn.setMessage(Component.literal(e.getText()));
                }).bounds(valueX, 0, VALUE_W, 18).build();
                rowWidgets.add(b);
                addRenderableWidget(b);
            } else {
                EditBox box = new EditBox(this.font, valueX, 0, VALUE_W, 18, Component.literal(e.label));
                box.setValue(e.getText());
                box.setResponder(s -> {
                    if (e.setText(s)) {
                        box.setTextColor(0xE0E0E0);
                    } else if (!s.isEmpty()) {
                        box.setTextColor(0xFF5555); // 无法解析 → 红字提示
                    }
                });
                rowWidgets.add(box);
                addRenderableWidget(box);
            }
        }
        layoutRows();
    }

    /** 切换页签/关屏前把输入框当前文本全部提交一次。 */
    private void commitEdits() {
        List<FRBConfig.Entry> entries = categories.get(tab).entries();
        for (int i = 0; i < entries.size() && i < rowWidgets.size(); i++) {
            if (rowWidgets.get(i) instanceof EditBox box) {
                String s = box.getValue();
                if (!s.isEmpty()) {
                    entries.get(i).setText(s);
                }
            }
        }
    }

    private Component boolText(FRBConfig.Entry e) {
        boolean v = (Boolean) e.value.get();
        return Component.literal(v ? "§a开" : "§c关");
    }

    private void layoutRows() {
        int contentTop = HEADER_H + 22;
        int contentBottom = this.height - FOOTER_H;
        for (int i = 0; i < rowWidgets.size(); i++) {
            var w = rowWidgets.get(i);
            int y = contentTop + i * ROW_H - (int) scroll;
            w.setY(y);
            w.visible = y >= contentTop - 20 && y <= contentBottom - 4;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxScroll = Math.max(0, categories.get(tab).entries().size() * ROW_H
                - (this.height - HEADER_H - FOOTER_H - 22));
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - delta * ROW_H));
        layoutRows();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        layoutRows();
        // 标题
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        // 条目行（标签 + 提示），控件由父类渲染
        int contentTop = HEADER_H + 22;
        int contentBottom = this.height - FOOTER_H;
        List<FRBConfig.Entry> entries = categories.get(tab).entries();
        g.enableScissor(0, contentTop - 4, this.width, contentBottom);
        for (int i = 0; i < entries.size(); i++) {
            int y = contentTop + i * ROW_H - (int) scroll;
            if (y < contentTop - 20 || y > contentBottom - 4) continue;
            FRBConfig.Entry e = entries.get(i);
            g.drawString(this.font, e.label, this.width / 2 - 170, y + 5, 0xFFFFFF);
            g.drawString(this.font, "§7" + e.tip, this.width / 2 - 170, y + 14, 0x888888);
        }
        g.disableScissor();
        super.render(g, mouseX, mouseY, partialTick);
        // 页签高亮：当前页下划线
        if (tab < tabButtons.size()) {
            Button b = tabButtons.get(tab);
            g.fill(b.getX(), b.getY() + b.getHeight(), b.getX() + b.getWidth(),
                    b.getY() + b.getHeight() + 1, 0xFF7CE68B);
        }
    }

    @Override
    public void onClose() {
        commitEdits();
        this.minecraft.setScreen(parent);
    }
}
