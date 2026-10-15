package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * 花灵生长分类：花种 → 三个生长阶段的植株贴图 → 成熟后右键收获的花灵饰品。
 * 阶段贴图直接复用 block 贴图，保证页面里的样子和游戏里一致。
 */
public class GrowthCategory implements IRecipeCategory<GrowthJeiRecipe> {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 122;

    private static final int[] STAGE_X = {26, 48, 70};
    private static final int ROW = 4;
    private static final int TEXT_TOP = 50;
    private static final int TEXT_LINE = 10;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Map<ResourceLocation, IDrawable> stageDrawables = new HashMap<>();

    public GrowthCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(FRBItems.PRIMORDIAL_PETAL.get()));
        this.arrow = helper.getRecipeArrow();
        for (GrowthJeiRecipe recipe : GrowthJeiRecipe.all()) {
            for (ResourceLocation tex : recipe.stages) {
                stageDrawables.computeIfAbsent(tex, t -> helper.createDrawable(t, 0, 0, 16, 16));
            }
        }
    }

    @Override
    public RecipeType<GrowthJeiRecipe> getRecipeType() {
        return FRBRecipeTypes.GROWTH;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("frb.jei.category.growth");
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
    public void setRecipe(IRecipeLayoutBuilder builder, GrowthJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, ROW).addItemStack(recipe.seed);
        if (!recipe.output.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 124, ROW).addItemStack(recipe.output);
        }
        if (recipe.bonemealable) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 2, 26)
                    .addItemStack(new ItemStack(Items.BONE_MEAL));
        }
    }

    @Override
    public void draw(GrowthJeiRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphics gui, double mouseX, double mouseY) {
        // 生长阶段
        for (int i = 0; i < recipe.stages.size() && i < STAGE_X.length; i++) {
            IDrawable stage = stageDrawables.get(recipe.stages.get(i));
            if (stage != null) {
                stage.draw(gui, STAGE_X[i], ROW);
            }
        }
        arrow.draw(gui, 98, ROW + 1);

        Font font = Minecraft.getInstance().font;
        if (recipe.bonemealable) {
            gui.drawString(font, Component.translatable("frb.jei.growth.bonemeal.short"),
                    24, 30, 0x7E7E7E);
        }
        int y = TEXT_TOP;
        for (Component line : recipe.infoLines()) {
            for (var wrapped : font.split(line, WIDTH - 4)) {
                gui.drawString(font, wrapped, 2, y, 0x404040, false);
                y += TEXT_LINE;
            }
        }
    }
}
