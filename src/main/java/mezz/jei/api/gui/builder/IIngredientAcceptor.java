/* 编译期桩（compile-time stub）：只声明本模组实际用到的 JEI API 形状，
 * 签名逐条对照真实 jei-1.20.1-*-api 的 class 文件写出，打包时整包排除，
 * 运行期使用的永远是玩家安装的 JEI 本体。 */
package mezz.jei.api.gui.builder;

import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 泛型层级与真实 API 保持一致（THIS 自限定），保证调用点描述符一致。 */
public interface IIngredientAcceptor<THIS extends IIngredientAcceptor<THIS>> {

    THIS addItemStack(ItemStack itemStack);

    <I> THIS addIngredients(IIngredientType<I> ingredientType, List<I> ingredients);
}
