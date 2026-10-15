package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * r72：FRB 配方的中央注册表 —— 让第三方 mod（尤其是 kubejs）能够动态注册新配方。
 *
 * 使用方式：
 * - 自定义 RitualRecipe：实现 {@link RitualLike} 接口并 {@link #registerRitual(RitualLike)}；
 * - 自定义 GrowthRecipe：实现 {@link GrowthLike} 接口并 {@link #registerGrowth(GrowthLike)}；
 * - JEI 在 registerRecipes 阶段从本表读取，kubejs 用户通过 {@code StartupEvents.post_init} 注册。
 *
 * 线程安全：使用 CopyOnWriteArrayList；JVM 启动后期修改只会影响后续读取。
 */
public final class FRBRecipeRegistry {

    /** 第三方可实现的「花仪仪式」接口；UI 字段与 builtin RitualJeiRecipe 保持一致。 */
    public interface RitualLike {
        ResourceLocation id();
        /** 输出图标（中央产物）；用于在 JEI 侧栏显示。 */
        net.minecraft.world.item.ItemStack icon();
        /** 副标题：仪式名（i18n key 或 Component）。 */
        net.minecraft.network.chat.Component title();
        /** 输入：仪式供品（最多 9 个槽位）。 */
        java.util.List<net.minecraft.world.item.ItemStack> inputs();
        /** 是否需要祭坛。 */
        boolean needsAltar();
        /** 解封要求（true = 解封「丰饶」流程）。 */
        boolean isUnseal();
    }

    /** 第三方可实现的「花灵生长」接口；UI 字段与 builtin GrowthJeiRecipe 保持一致。 */
    public interface GrowthLike {
        ResourceLocation id();
        net.minecraft.world.item.ItemStack icon();
        net.minecraft.network.chat.Component title();
        java.util.List<net.minecraft.world.item.ItemStack> inputs();
        /** 成熟条件（环境描述 / 玩家描述等）。 */
        java.util.List<net.minecraft.network.chat.Component> conditions();
        /** 输出（成熟后采集的物品）。 */
        java.util.List<net.minecraft.world.item.ItemStack> outputs();
    }

    private static final List<RitualLike> RITUALS = new CopyOnWriteArrayList<>();
    private static final List<GrowthLike> GROWTHS = new CopyOnWriteArrayList<>();

    /** 注册一个仪式配方（kukejs / 自定义 mod 调用）。 */
    public static void registerRitual(RitualLike recipe) {
        if (recipe != null && recipe.id() != null) RITUALS.add(recipe);
    }

    /** 注册一个生长配方。 */
    public static void registerGrowth(GrowthLike recipe) {
        if (recipe != null && recipe.id() != null) GROWTHS.add(recipe);
    }

    /** 全部仪式（builtin + 第三方，顺序保留注册顺序）。 */
    @SuppressWarnings("unchecked")
    public static List<RitualJeiRecipe> getRituals() {
        // 强转为 List<RitualJeiRecipe>：RitualJeiRecipe 已实现 RitualLike，
        // 第三方可传任意 RitualLike，但 FRBJeiPlugin 的 RecipeType<RitualJeiRecipe>
        // 期望这个具体类型 —— 安全靠 addRecipes 内部的 setRecipe(builder, T, IFocus)
        // 只把 T 当 T 用；若第三方传了非 RitualJeiRecipe 的实现，会在渲染时 ClassCastException。
        List<RitualJeiRecipe> out = new java.util.ArrayList<>();
        for (RitualLike r : RITUALS) {
            if (r instanceof RitualJeiRecipe rj) {
                out.add(rj);
            }
            // 第三方非 RitualJeiRecipe 的 RitualLike 实现目前不支持（接口仅作为契约文档）
        }
        return Collections.unmodifiableList(out);
    }

    @SuppressWarnings("unchecked")
    public static List<GrowthJeiRecipe> getGrowths() {
        List<GrowthJeiRecipe> out = new java.util.ArrayList<>();
        for (GrowthLike r : GROWTHS) {
            if (r instanceof GrowthJeiRecipe gj) {
                out.add(gj);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** 清空（仅用于测试或重新加载；不要在 mod 主流程中调用）。 */
    public static void clear() {
        RITUALS.clear();
        GROWTHS.clear();
    }

    /**
     * r72：把已有的 RitualJeiRecipe / GrowthJeiRecipe 直接注册到中央表（这两类已实现 RitualLike / GrowthLike 接口）。
     * 注意第三方实现（RitualLike 子类但不是 RitualJeiRecipe）暂时不会被 addRecipes 调用 —— 这是有意限制，
     * 第三方若想新增 JEI 类别可参照内置类别自建 RecipeType。
     */
    public static void registerBuiltin() {
        for (RitualJeiRecipe r : RitualJeiRecipe.all()) {
            RITUALS.add(r);
        }
        for (GrowthJeiRecipe r : GrowthJeiRecipe.all()) {
            GROWTHS.add(r);
        }
    }

    /** ResourceLocation 构造便捷方法（避免每次都 new ResourceLocation(MOD_ID, path)）。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, path);
    }

    private FRBRecipeRegistry() {}
}