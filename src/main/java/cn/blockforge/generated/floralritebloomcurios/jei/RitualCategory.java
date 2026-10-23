package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 花仪仪式分类：上方一排供品格（第一格是花仪祭坛），第二排是环境要求（方块 / 流体），
 * 右侧是仪式凝结出的花灵种子；下方文字列出海拔、天气、群系等条件。
 * 条件数值与 Rituals.DEFS 同源，改了仪式定义，页面自动跟着变。
 */
public class RitualCategory implements IRecipeCategory<RitualJeiRecipe> {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 146;

    private static final int[] OFFERING_X = {22, 40, 58, 76, 94};
    private static final int ROW_INGREDIENT = 4;
    private static final int ROW_NEARBY = 26;
    private static final int TEXT_TOP = 50;
    private static final int TEXT_LINE = 10;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public RitualCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(FRBItems.RITUAL_ALTAR.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<RitualJeiRecipe> getRecipeType() {
        return FRBRecipeTypes.RITUAL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("frb.jei.category.ritual");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RitualJeiRecipe recipe, IFocusGroup focuses) {
        // 仪式载体：花仪祭坛 / 花仪台座（台座上的供品优先被消耗）
        builder.addSlot(RecipeIngredientRole.CATALYST, 2, ROW_INGREDIENT)
                .addItemStack(new ItemStack(FRBItems.RITUAL_ALTAR.get()))
                .addItemStack(new ItemStack(FRBItems.FLORAL_ALTAR.get()));

        for (int i = 0; i < recipe.offerings.size() && i < OFFERING_X.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, OFFERING_X[i], ROW_INGREDIENT)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.offerings.get(i));
        }

        if (!recipe.nearbyItems.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 22, ROW_NEARBY)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.nearbyItems);
        }
        if (!recipe.nearbyFluids.isEmpty()) {
            var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 42, ROW_NEARBY);
            for (net.minecraft.world.level.material.Fluid fluid : recipe.nearbyFluids) {
                addFluidCompat(slot, fluid, 1000);
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROW_NEARBY).addItemStack(recipe.output);
    }

    /**
     * r113：跨 JEI 版本地添加流体槽位。
     *
     * <p>JEI 1.20.1 各版本的 {@code IRecipeSlotBuilder.addFluidStack} 重载签名不一致：
     * 早期（如 15.20.0）只有 {@code (Fluid, int)} 形态，后期（15.4x+）新增了
     * {@code (Fluid, long)}。编译期用的 API 桩无法同时覆盖所有版本，
     * 写死任何一种在别的版本上都会抛 {@code NoSuchMethodError}，
     * 进而让 JEI 里<b>每一条</b>花仪仪式配方注册失败。</p>
     *
     * <p>这里在运行期用反射挑当前 JEI 实际存在的重载来调用（单参 → int → long 依次尝试），
     * 彻底摆脱"桩与真实 API 版本耦合"的问题。全部失败时静默跳过，不影响其余槽位。</p>
     */
    private static void addFluidCompat(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot,
                                       net.minecraft.world.level.material.Fluid fluid, int amount) {
        Class<?> fluidCls = net.minecraft.world.level.material.Fluid.class;
        for (java.lang.reflect.Method m : mezz.jei.api.gui.builder.IRecipeSlotBuilder.class.getMethods()) {
            if (!"addFluidStack".equals(m.getName())) continue;
            Class<?>[] params = m.getParameterTypes();
            try {
                if (params.length == 1 && params[0] == fluidCls) {
                    m.invoke(slot, fluid);
                    return;
                }
                if (params.length == 2 && params[0] == fluidCls
                        && (params[1] == int.class || params[1] == long.class)) {
                    Object amt = (params[1] == int.class) ? (Object) amount : (Object) (long) amount;
                    m.invoke(slot, fluid, amt);
                    return;
                }
            } catch (Throwable ignored) {
                // 该重载在此 JEI 版本下不可用 → 试下一个
            }
        }
    }

    @Override
    public void draw(RitualJeiRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphics gui, double mouseX, double mouseY) {
        arrow.draw(gui, 116, ROW_NEARBY + 1);
        Font font = Minecraft.getInstance().font;
        int y = TEXT_TOP;
        for (Component line : recipe.conditionLines()) {
            for (var wrapped : font.split(line, WIDTH - 8)) {
                gui.drawString(font, wrapped, 4, y, 0x404040, false);
                y += TEXT_LINE;
            }
        }
        y += 3;
        for (var wrapped : font.split(Component.translatable("frb.jei.ritual.usage"), WIDTH - 8)) {
            gui.drawString(font, wrapped, 4, y, 0x555555, false);
            y += TEXT_LINE;
        }
    }
}
