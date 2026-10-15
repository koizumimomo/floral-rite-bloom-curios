/* 编译期桩（compile-time stub）：只声明本模组实际用到的 JEI API 形状，
 * 签名逐条对照真实 jei-1.20.1-*-api 的 class 文件写出，打包时整包排除，
 * 运行期使用的永远是玩家安装的 JEI 本体。 */
package mezz.jei.api.helpers;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface IGuiHelper {

    IDrawableStatic createDrawable(ResourceLocation resourceLocation, int u, int v, int width, int height);

    IDrawableStatic createBlankDrawable(int width, int height);

    IDrawable createDrawableItemStack(ItemStack itemStack);

    IDrawableStatic getRecipeArrow();
}
