/* 编译期桩（compile-time stub）：只声明本模组实际用到的 JEI API 形状，
 * 签名逐条对照真实 jei-1.20.1-*-api 的 class 文件写出，打包时整包排除，
 * 运行期使用的永远是玩家安装的 JEI 本体。
 *
 * r113：addFluidStack 的重载在不同 JEI 版本里签名不同 ——
 *       JEI 15.20.0（1.20.1 早期）没有 (Fluid, long)，而 15.49 才加的 (Fluid, long)。
 *       桩若只声明某一种，编译期无法察觉，运行期会抛
 *       NoSuchMethodError: IRecipeSlotBuilder.addFluidStack(Fluid, long)，
 *       导致 JEI 里所有花仪仪式配方注册失败（每个配方一条 broken recipe 错误）。
 *       这里把常见重载都声明出来（桩只影响编译期），运行期由
 *       RitualCategory.addFluidCompat() 用反射挑实际存在的那一个调用。 */
package mezz.jei.api.gui.builder;

import net.minecraft.world.level.material.Fluid;

public interface IRecipeSlotBuilder extends IIngredientAcceptor<IRecipeSlotBuilder> {

    IRecipeSlotBuilder addFluidStack(Fluid fluid);

    IRecipeSlotBuilder addFluidStack(Fluid fluid, int amount);

    IRecipeSlotBuilder addFluidStack(Fluid fluid, long amount);
}
