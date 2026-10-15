package cn.blockforge.generated.floralritebloomcurios.ritual;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 七大花仪仪式定义：高度 / 天气 / 生物群系 / 附近方块 / 供品材料。
 * 手持“无相花种”右键花仪祭坛时按顺序匹配第一个完全满足的仪式。
 */
public final class Rituals {

    public enum WeatherNeed { ANY, CLEAR, RAIN, SNOW }

    /** 供品：物品 × 数量；returnBucket 表示容器类供品（岩浆桶）消耗后把空桶退回背包。 */
    public record Cost(Item item, int count, boolean returnBucket) {
        public Cost(Item item, int count) {
            this(item, count, false);
        }
    }

    public record Def(CharmType type, int minAlt, int maxAlt, WeatherNeed weather,
                      List<Block> nearbyAny, int nearbyCount,
                      List<Cost> costs) {}

    public static final Map<CharmType, Def> DEFS = new EnumMap<>(CharmType.class);

    static {
        DEFS.put(CharmType.FROST_CHIME, new Def(CharmType.FROST_CHIME, 60, 320, WeatherNeed.SNOW,
                List.of(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.ICE), 1,
                List.of(new Cost(Items.SNOWBALL, 4))));
        DEFS.put(CharmType.ASTRAL_BLOOM, new Def(CharmType.ASTRAL_BLOOM, 130, 320, WeatherNeed.CLEAR,
                List.of(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN), 1,
                List.of(new Cost(Items.ENDER_PEARL, 1))));
        DEFS.put(CharmType.SKY_PILLAR, new Def(CharmType.SKY_PILLAR, 145, 320, WeatherNeed.CLEAR,
                List.of(Blocks.LIGHTNING_ROD, Blocks.SCAFFOLDING), 1,
                List.of(new Cost(Items.PHANTOM_MEMBRANE, 1))));
        DEFS.put(CharmType.SKY_TEAR, new Def(CharmType.SKY_TEAR, 0, 90, WeatherNeed.RAIN,
                List.of(Blocks.WATER), 2,
                List.of(new Cost(Items.SPONGE, 1), new Cost(Items.HEART_OF_THE_SEA, 1))));
        DEFS.put(CharmType.FOREST_WHISPER, new Def(CharmType.FOREST_WHISPER, 55, 160, WeatherNeed.ANY,
                List.of(Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.SEAGRASS, Blocks.FERN,
                        Blocks.LARGE_FERN, Blocks.PODZOL, Blocks.MOSS_BLOCK), 2,
                List.of(new Cost(Items.MOSS_BLOCK, 1))));
        DEFS.put(CharmType.EMBER_VINE, new Def(CharmType.EMBER_VINE, 0, 45, WeatherNeed.ANY,
                List.of(Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE, Blocks.SOUL_SOIL), 1,
                List.of(new Cost(Items.LAVA_BUCKET, 1, true),
                        new Cost(Items.MAGMA_BLOCK, 1),
                        new Cost(Items.COAL_BLOCK, 1))));
        DEFS.put(CharmType.BLACK_SPIRIT, new Def(CharmType.BLACK_SPIRIT, 0, 72, WeatherNeed.ANY,
                List.of(Blocks.SOUL_SAND, Blocks.SOUL_SOIL), 1,
                List.of(new Cost(Items.ROTTEN_FLESH, 4))));
    }

    /** 返回 null 表示仪式达成；否则返回失败原因语言键。 */
    public static String check(Level level, BlockPos altarPos, Def def, Offerings src) {
        String envFail = checkEnvironment(level, altarPos, def);
        if (envFail != null) return envFail;
        // 引子与供品
        for (Cost cost : def.costs) {
            if (src.count(cost.item()) < cost.count()) {
                return "frb.ritual.fail.cost";
            }
        }
        if (src.count(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.FORMLESS_SEED.get()) < 1
                || src.count(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.PRIMORDIAL_PETAL.get()) < 2) {
            return "frb.ritual.fail.cost";
        }
        return null;
    }

    /** 仅环境检查（高度 / 天气 / 群系 / 附近方块）；丰饶花仪锻造复用。返回 null 表示环境达成。 */
    public static String checkEnvironment(Level level, BlockPos altarPos, Def def) {
        int y = altarPos.getY();
        if (y < def.minAlt || y > def.maxAlt) {
            return "frb.ritual.fail.alt";
        }
        switch (def.weather) {
            case CLEAR -> {
                if (level.isRaining()) return "frb.ritual.fail.weather";
            }
            case RAIN -> {
                if (!level.isRaining() || !hasPrecipitation(level, altarPos, Biome.Precipitation.RAIN))
                    return "frb.ritual.fail.weather";
            }
            case SNOW -> {
                if (!level.isRaining() || !hasPrecipitation(level, altarPos, Biome.Precipitation.SNOW))
                    return "frb.ritual.fail.weather";
            }
            default -> {}
        }
        if (def.type == CharmType.FOREST_WHISPER && !level.getBiome(altarPos).is(net.minecraft.tags.BiomeTags.IS_FOREST)) {
            return "frb.ritual.fail.biome";
        }
        if (!def.nearbyAny.isEmpty() && countNearby(level, altarPos, def) < def.nearbyCount) {
            return "frb.ritual.fail.blocks";
        }
        return null;
    }

    /** 创造模式祭坛只复用供品定义，不检查位置、天气、群系和附近方块。 */
    public static String checkCreative(Def def, Offerings src) {
        for (Cost cost : def.costs) {
            if (src.count(cost.item()) < cost.count()) return "frb.ritual.fail.cost";
        }
        if (src.count(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.FORMLESS_SEED.get()) < 1
                || src.count(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.PRIMORDIAL_PETAL.get()) < 2) {
            return "frb.ritual.fail.cost";
        }
        return null;
    }

    private static boolean hasPrecipitation(Level level, BlockPos pos, Biome.Precipitation want) {
        Biome biome = level.getBiome(pos).value();
        return biome.getPrecipitationAt(pos) == want;
    }

    private static int countNearby(Level level, BlockPos center, Def def) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 3, 3))) {
            BlockState state = level.getBlockState(p);
            if (def.nearbyAny().stream().anyMatch(state::is)) {
                n++;
            } else {
                var fl = state.getFluidState();
                if (fl.is(FluidTags.LAVA) && def.nearbyAny().contains(Blocks.LAVA)) {
                    n++;
                } else if (fl.is(FluidTags.WATER) && fl.isSource() && def.nearbyAny().contains(Blocks.WATER)) {
                    n++;
                }
            }
        }
        return n;
    }

    private Rituals() {}
}
