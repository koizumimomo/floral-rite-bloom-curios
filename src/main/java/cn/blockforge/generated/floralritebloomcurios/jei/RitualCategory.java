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
            recipe.nearbyFluids.forEach(fluid -> slot.addFluidStack(fluid, 1000L));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROW_NEARBY).addItemStack(recipe.output);
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
