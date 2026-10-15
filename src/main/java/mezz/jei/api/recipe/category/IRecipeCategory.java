/* 编译期桩（compile-time stub）：只声明本模组实际用到的 JEI API 形状，
 * 签名逐条对照真实 jei-1.20.1-*-api 的 class 文件写出，打包时整包排除，
 * 运行期使用的永远是玩家安装的 JEI 本体。 */
package mezz.jei.api.recipe.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public interface IRecipeCategory<T> {

    RecipeType<T> getRecipeType();

    Component getTitle();

    IDrawable getBackground();

    IDrawable getIcon();

    default int getWidth() {
        return getBackground().getWidth();
    }

    default int getHeight() {
        return getBackground().getHeight();
    }

    void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    default void draw(T recipe, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {}
}
