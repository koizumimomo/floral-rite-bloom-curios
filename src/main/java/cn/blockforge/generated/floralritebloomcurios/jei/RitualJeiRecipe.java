package cn.blockforge.generated.floralritebloomcurios.jei;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.ritual.RitualRecipes;
import cn.blockforge.generated.floralritebloomcurios.ritual.Rituals;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * 花仪仪式的 JEI 只读视图。数据全部取自单一数据源：七仪读 {@link Rituals#DEFS}
 * + {@link RitualRecipes#baseCosts()}，月相仪式读 {@link RitualRecipes#lunar}，
 * 丰饶三段式读 {@link RitualRecipes#bountifulCraft()} / {@link RitualRecipes#unsealCosts()}。
 * 与游戏内 {@code RitualManager} 的判定/消耗同源：以后增删材料只改 RitualRecipes / Rituals，
 * JEI 页面自动跟着变，本类不再维护任何手写材料表。
 *
 * r72：本类实现 {@link FRBRecipeRegistry.RitualLike}，
 * 这样既可以走 JEI 的 {@code RitualCategory}，又可以注册到中央注册表。
 */
public final class RitualJeiRecipe implements FRBRecipeRegistry.RitualLike {

    /** 丰饶专属条目类型（null = 七仪种子凝结）。 */
    public enum BountifulKind { CRAFT, UNSEAL, FORGE }

    @org.jetbrains.annotations.Nullable
    public final CharmType type;
    @org.jetbrains.annotations.Nullable
    public final BountifulKind bkind;
    @org.jetbrains.annotations.Nullable
    public final RitualRecipes.LunarKind lkind;
    public final ItemStack output;
    /** 需要消耗的物品：每格一列可选物品（丰饶的花灵饰品一格可选七种）。 */
    public final List<List<ItemStack>> offerings;
    /** 环境要求里“有物品”的方块。 */
    public final List<ItemStack> nearbyItems;
    /** 环境要求里的流体（水 / 岩浆）。 */
    public final List<Fluid> nearbyFluids;
    public final int minAlt;
    public final int maxAlt;
    public final int nearbyCount;
    public final Rituals.WeatherNeed weather;
    public final boolean needsForest;
    public final boolean returnsBucket;

    private RitualJeiRecipe(Rituals.Def def) {
        this.type = def.type();
        this.bkind = null;
        this.lkind = null;
        this.output = new ItemStack(FRBItems.seedOf(def.type()));
        this.minAlt = def.minAlt();
        this.maxAlt = def.maxAlt();
        this.nearbyCount = def.nearbyCount();
        this.weather = def.weather();
        this.needsForest = def.type() == CharmType.FOREST_WHISPER;

        this.offerings = new ArrayList<>();
        // r84：公共引子（花种×1 + 花瓣×2）读 RitualRecipes.baseCosts()
        for (Rituals.Cost cost : RitualRecipes.baseCosts()) {
            this.offerings.add(List.of(new ItemStack(cost.item(), cost.count())));
        }
        boolean bucket = false;
        for (Rituals.Cost cost : def.costs()) {
            this.offerings.add(List.of(new ItemStack(cost.item(), cost.count())));
            bucket |= cost.returnBucket();
        }
        this.returnsBucket = bucket;

        this.nearbyItems = new ArrayList<>();
        this.nearbyFluids = new ArrayList<>();
        for (Block block : def.nearbyAny()) {
            if (block == Blocks.WATER) {
                this.nearbyFluids.add(Fluids.WATER);
            } else if (block == Blocks.LAVA) {
                this.nearbyFluids.add(Fluids.LAVA);
            } else {
                // 细雪方块本身没有物品形态，用细雪桶代表它
                ItemStack stack = block == Blocks.POWDER_SNOW
                        ? new ItemStack(Items.POWDER_SNOW_BUCKET)
                        : new ItemStack(block.asItem());
                if (!stack.isEmpty() && !stack.is(Items.AIR)) {
                    this.nearbyItems.add(stack);
                }
            }
        }
    }

    /** 丰饶三段式条目：合成（封印）/ 解封（世界之花意志）/ 强化（七灵印刻）。 */
    private RitualJeiRecipe(BountifulKind kind) {
        this.type = null;
        this.bkind = kind;
        this.lkind = null;
        this.minAlt = 0;
        this.maxAlt = 320;
        this.nearbyCount = 0;
        this.weather = Rituals.WeatherNeed.ANY;
        this.needsForest = false;
        this.returnsBucket = false;

        this.offerings = new ArrayList<>();
        switch (kind) {
            case CRAFT -> {
                // r84：材料读 RitualRecipes.bountifulCraft()（固定供品 + 任意花灵饰品）
                RitualRecipes.BountyDef def = RitualRecipes.bountifulCraft();
                this.output = new ItemStack(FRBItems.BOUNTIFUL.get());
                for (Rituals.Cost cost : def.costs()) {
                    this.offerings.add(List.of(new ItemStack(cost.item(), cost.count())));
                }
                List<ItemStack> anyCharm = new ArrayList<>();
                for (Item item : def.anyOneOf()) {
                    anyCharm.add(new ItemStack(item));
                }
                this.offerings.add(anyCharm);
            }
            case UNSEAL -> {
                // r84：解封供品读 RitualRecipes.unsealCosts()
                this.output = unsealedBountiful();
                this.offerings.add(List.of(new ItemStack(FRBItems.BOUNTIFUL.get())));
                for (Rituals.Cost cost : RitualRecipes.unsealCosts()) {
                    this.offerings.add(List.of(new ItemStack(cost.item(), cost.count())));
                }
            }
            default -> {
                // 解封的丰饶 + 对应花灵饰品（七仪各一次，逐仪 +10%）
                ItemStack engraved = unsealedBountiful();
                cn.blockforge.generated.floralritebloomcurios.util.BountifulRites
                        .mark(engraved, CharmType.ALL[0]);
                this.output = engraved;
                this.offerings.add(unsealedBountifulList());
                List<ItemStack> charms = new ArrayList<>();
                for (CharmType t : CharmType.ALL) {
                    charms.add(new ItemStack(FRBItems.charmOf(t)));
                }
                this.offerings.add(charms);
            }
        }

        this.nearbyItems = new ArrayList<>();
        this.nearbyFluids = new ArrayList<>();
    }

    /** r74：月相仪式条目（月见草种子 / 月之水晶 / 月之庇护）；r84 起材料全部读 RitualRecipes.lunar()。 */
    private RitualJeiRecipe(RitualRecipes.LunarKind kind) {
        this.type = null;
        this.bkind = null;
        this.lkind = kind;
        this.minAlt = 0;
        this.maxAlt = 320;
        this.nearbyCount = 0;
        this.weather = Rituals.WeatherNeed.ANY;
        this.needsForest = false;
        this.returnsBucket = false;

        this.offerings = new ArrayList<>();
        this.nearbyItems = new ArrayList<>();
        this.nearbyFluids = new ArrayList<>();

        RitualRecipes.LunarDef def = RitualRecipes.lunar(kind);
        this.output = new ItemStack(def.output());
        // 手持引子（如月冠的月之水晶碎片、月之水晶的紫水晶碎片）也消耗 1 个，一并展示
        if (def.catalyst() != null) {
            this.offerings.add(List.of(new ItemStack(def.catalyst(), 1)));
        }
        for (Rituals.Cost cost : def.costs()) {
            this.offerings.add(List.of(new ItemStack(cost.item(), cost.count())));
        }
        if (def.amethystEnv()) {
            this.nearbyItems.add(new ItemStack(Blocks.AMETHYST_CLUSTER));
            this.nearbyFluids.add(Fluids.WATER);
        }
    }

    private static List<ItemStack> unsealedBountifulList() {
        return List.of(unsealedBountiful());
    }

    /** 解封态丰饶（带解封标记的展示堆）。 */
    private static ItemStack unsealedBountiful() {
        ItemStack s = new ItemStack(FRBItems.BOUNTIFUL.get());
        cn.blockforge.generated.floralritebloomcurios.util.BountifulRites.unseal(s);
        return s;
    }

    /** 与 {@link CharmType#ALL} 同序生成仪式条目，末尾追加丰饶合成 / 解封 / 强化 + r74 月相三联。 */
    public static List<RitualJeiRecipe> all() {
        List<RitualJeiRecipe> list = new ArrayList<>();
        for (CharmType type : CharmType.ALL) {
            Rituals.Def def = Rituals.DEFS.get(type);
            if (def != null) {
                list.add(new RitualJeiRecipe(def));
            }
        }
        list.add(new RitualJeiRecipe(BountifulKind.CRAFT));
        list.add(new RitualJeiRecipe(BountifulKind.UNSEAL));
        list.add(new RitualJeiRecipe(BountifulKind.FORGE));
        // r74：月相仪式链（月见草种子 → 月之水晶 → 月之庇护）
        list.add(new RitualJeiRecipe(RitualRecipes.LunarKind.PRIMROSE_SEED));
        list.add(new RitualJeiRecipe(RitualRecipes.LunarKind.LUNAR_SHARD));
        list.add(new RitualJeiRecipe(RitualRecipes.LunarKind.LUNAR_CROWN));
        return list;
    }

    // ---- r72：实现 FRBRecipeRegistry.RitualLike 接口（让 builtin 既可入注册表也可直接 addRecipes）----

    @Override
    public ResourceLocation id() {
        // id = mod + "jei/ritual/" + type(枚举名) 或 bkind/lkind 名
        String suffix;
        if (bkind != null) {
            suffix = "bountiful_" + bkind.name().toLowerCase();
        } else if (lkind != null) {
            suffix = "lunar_" + lkind.name().toLowerCase();
        } else if (type != null) {
            suffix = type.name().toLowerCase();
        } else {
            suffix = "unknown";
        }
        return new ResourceLocation(
                cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom.MOD_ID,
                "jei/ritual/" + suffix);
    }

    @Override
    public ItemStack icon() {
        return output;
    }

    @Override
    public Component title() {
        if (bkind != null) {
            return switch (bkind) {
                case CRAFT -> Component.translatable("frb.jei.ritual.bountiful.craft");
                case UNSEAL -> Component.translatable("frb.jei.ritual.bountiful.unseal");
                case FORGE -> Component.translatable("frb.jei.ritual.bountiful.forge");
            };
        }
        if (lkind != null) {
            return switch (lkind) {
                case PRIMROSE_SEED -> Component.translatable("frb.jei.ritual.evening_primrose");
                case LUNAR_SHARD -> Component.translatable("frb.jei.ritual.lunar_shard");
                case LUNAR_CROWN -> Component.translatable("frb.jei.ritual.lunar_crown");
            };
        }
        if (type != null) {
            return Component.translatable("frb.jei.ritual." + type.name().toLowerCase());
        }
        return Component.literal("Ritual");
    }

    @Override
    public List<ItemStack> inputs() {
        // 把 offerings（每格一列可选）展平成一个 List<ItemStack>，取每格首个代表
        List<ItemStack> flat = new ArrayList<>();
        for (List<ItemStack> slot : offerings) {
            if (!slot.isEmpty()) flat.add(slot.get(0));
        }
        return flat;
    }

    @Override
    public boolean needsAltar() {
        // 仪式都需要祭坛
        return true;
    }

    @Override
    public boolean isUnseal() {
        return bkind == BountifulKind.UNSEAL;
    }

    /** 条件文字行（海拔 / 天气 / 群系 / 附近方块 / 留桶）。 */
    public List<Component> conditionLines() {
        List<Component> lines = new ArrayList<>();
        if (bkind != null) {
            lines.add(Component.translatable("frb.jei.cond.none"));
            lines.add(Component.translatable(
                    bkind == BountifulKind.CRAFT ? "frb.jei.cond.bountiful"
                            : bkind == BountifulKind.UNSEAL ? "frb.jei.cond.unseal"
                            : "frb.jei.cond.forge"));
            return lines;
        }
        if (lkind != null) {
            // r77：月相仪式只标注时间（材料已在 JEI 物品栏中显示），不再重复列出。
            lines.add(Component.translatable("frb.jei.cond.time.night"));
            return lines;
        }
        if (type == null) {
            return lines;
        }
        lines.add(altitudeLine());
        lines.add(Component.translatable("frb.jei.cond.weather", weatherName()));
        if (needsForest) {
            lines.add(Component.translatable("frb.jei.cond.biome",
                    Component.translatable("frb.jei.biome.forest")));
        }
        lines.add(Component.translatable("frb.jei.cond.nearby",
                Component.literal(Integer.toString(nearbyCount))));
        if (returnsBucket) {
            lines.add(Component.translatable("frb.jei.cond.bucket"));
        }
        return lines;
    }

    private Component altitudeLine() {
        Component min = Component.literal(Integer.toString(minAlt));
        Component max = Component.literal(Integer.toString(maxAlt));
        if (minAlt <= 0 && maxAlt >= 320) {
            return Component.translatable("frb.jei.cond.alt.any");
        }
        if (maxAlt >= 320) {
            return Component.translatable("frb.jei.cond.alt.min", min);
        }
        if (minAlt <= 0) {
            return Component.translatable("frb.jei.cond.alt.max", max);
        }
        return Component.translatable("frb.jei.cond.alt.range", min, max);
    }

    private Component weatherName() {
        return switch (weather) {
            case CLEAR -> Component.translatable("frb.jei.weather.clear");
            case RAIN -> Component.translatable("frb.jei.weather.rain");
            case SNOW -> Component.translatable("frb.jei.weather.snow");
            default -> Component.translatable("frb.jei.weather.any");
        };
    }
}
