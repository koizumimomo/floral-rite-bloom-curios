package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 花灵作物生长的 JEI 只读视图：种子 → 各生长阶段的贴图 → 成熟后的花灵饰品。
 * 最后一条是 世界之花（催生 Boss，无物品产出）。
 */
public final class GrowthJeiRecipe implements FRBRecipeRegistry.GrowthLike {

    /** 花灵类型；世界之花为 null。 */
    public final CharmType type;
    public final ItemStack seed;
    /** 成熟产物；世界之花为 EMPTY。 */
    public final ItemStack output;
    /** 依次展示的生长阶段贴图（16×16 的方块 cross 贴图）。 */
    public final List<ResourceLocation> stages;
    public final boolean bonemealable;
    private final List<Component> info;

    private GrowthJeiRecipe(CharmType type, ItemStack seed, ItemStack output,
                            List<ResourceLocation> stages, boolean bonemealable,
                            List<Component> info) {
        this.type = type;
        this.seed = seed;
        this.output = output;
        this.stages = stages;
        this.bonemealable = bonemealable;
        this.info = info;
    }

    public List<Component> infoLines() {
        return info;
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/block/" + name + ".png");
    }

    public static List<GrowthJeiRecipe> all() {
        List<GrowthJeiRecipe> list = new ArrayList<>();
        for (CharmType type : CharmType.ALL) {
            List<Component> info = new ArrayList<>();
            info.add(Component.translatable("frb.jei.growth.light"));
            info.add(Component.translatable("frb.jei.growth.bonemeal"));
            info.add(Component.translatable("frb.jei.growth.harvest"));
            info.add(Component.translatable("frb.jei.growth.revert"));
            list.add(new GrowthJeiRecipe(type,
                    new ItemStack(FRBItems.seedOf(type)),
                    new ItemStack(FRBItems.charmOf(type)),
                    List.of(tex("flower_sprout_small"), tex("flower_sprout_mid"), tex(type.id + "_flower")),
                    true, info));
        }
        // 世界之花：只自然绽放，完全盛开时迎来 Boss
        List<Component> worldInfo = new ArrayList<>();
        worldInfo.add(Component.translatable("frb.jei.growth.world.light"));
        worldInfo.add(Component.translatable("frb.jei.growth.world.nobonemeal"));
        worldInfo.add(Component.translatable("frb.jei.growth.world.boss"));
        list.add(new GrowthJeiRecipe(null,
                new ItemStack(FRBItems.SEED_OF_REALM.get()),
                ItemStack.EMPTY,
                List.of(tex("flower_sprout_small"), tex("world_flower_bud"), tex("world_flower")),
                false, worldInfo));
        return list;
    }

    // ---- r72：实现 FRBRecipeRegistry.GrowthLike 接口 ----

    @Override
    public ResourceLocation id() {
        // id = mod + "jei/growth/" + type(枚举名) 或 "world"
        String suffix = type != null ? type.name().toLowerCase() : "world";
        return new ResourceLocation(
                cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom.MOD_ID,
                "jei/growth/" + suffix);
    }

    @Override
    public ItemStack icon() {
        return output.isEmpty() ? seed : output;
    }

    @Override
    public Component title() {
        if (type == null) {
            return Component.translatable("frb.jei.growth.world");
        }
        return Component.translatable("frb.jei.growth." + type.name().toLowerCase());
    }

    @Override
    public List<ItemStack> inputs() {
        List<ItemStack> in = new ArrayList<>();
        in.add(seed);
        return in;
    }

    @Override
    public List<Component> conditions() {
        return info;
    }

    @Override
    public List<ItemStack> outputs() {
        List<ItemStack> out = new ArrayList<>();
        if (!output.isEmpty()) out.add(output);
        return out;
    }
}
