package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 适配入口（JEI 为可选前置，未安装时本类不会被加载）：
 * 1) 花仪仪式 —— 七场仪式的供品与环境条件；
 * 2) 花灵生长 —— 花种到成熟植株再到饰品的全过程，含世界之花。
 */
@JeiPlugin
public class FRBJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new RitualCategory(helper), new GrowthCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // r72：先注册内置配方 → 再叠加第三方注册（kubejs / 自定义 mod）。
        // 第三方通过 StartupEvents.post_init 阶段调用 FRBRecipeRegistry.registerRitual/registerGrowth。
        // 顺序：第三方注册晚于内置，所以列表尾部是自定义配方；
        // addRecipes() 接受 List，输出顺序保留注册顺序。
        FRBRecipeRegistry.registerBuiltin();
        registration.addRecipes(FRBRecipeTypes.RITUAL, FRBRecipeRegistry.getRituals());
        registration.addRecipes(FRBRecipeTypes.GROWTH, FRBRecipeRegistry.getGrowths());

        // 常见材料的说明页，省得玩家在合成表里绕
        registration.addIngredientInfo(new ItemStack(FRBItems.RITUAL_ALTAR.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("frb.jei.info.altar"));
        registration.addIngredientInfo(new ItemStack(FRBItems.FORMLESS_SEED.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("frb.jei.info.formless"));
        registration.addIngredientInfo(new ItemStack(FRBItems.SEED_OF_REALM.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("frb.jei.info.realm"));
        registration.addIngredientInfo(new ItemStack(FRBItems.FLORAL_ALTAR.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("frb.jei.info.pedestal"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack altar = new ItemStack(FRBItems.RITUAL_ALTAR.get());
        ItemStack pedestal = new ItemStack(FRBItems.FLORAL_ALTAR.get());
        registration.addRecipeCatalyst(altar, FRBRecipeTypes.RITUAL);
        registration.addRecipeCatalyst(pedestal, FRBRecipeTypes.RITUAL);
        registration.addRecipeCatalyst(altar, FRBRecipeTypes.GROWTH);
    }
}
