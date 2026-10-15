package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import mezz.jei.api.recipe.RecipeType;

/**
 * JEI 配方类型：花仪仪式、花灵生长。
 * 只在 JEI 存在时被加载（所有引用都来自 {@link FRBJeiPlugin} 及其分类）。
 */
public final class FRBRecipeTypes {

    public static final RecipeType<RitualJeiRecipe> RITUAL =
            RecipeType.create(FloralRiteBloom.MOD_ID, "ritual", RitualJeiRecipe.class);

    public static final RecipeType<GrowthJeiRecipe> GROWTH =
            RecipeType.create(FloralRiteBloom.MOD_ID, "growth", GrowthJeiRecipe.class);

    private FRBRecipeTypes() {}
}
