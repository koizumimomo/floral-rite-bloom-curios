package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.block.FlowerPlantBlock;
import cn.blockforge.generated.floralritebloomcurios.block.FloralAltarBlock;
import cn.blockforge.generated.floralritebloomcurios.block.RitualAltarBlock;
import cn.blockforge.generated.floralritebloomcurios.block.CreativeRitualAltarBlock;
import cn.blockforge.generated.floralritebloomcurios.block.WorldFlowerBlock;
import cn.blockforge.generated.floralritebloomcurios.block.LifeFireBlock;
import cn.blockforge.generated.floralritebloomcurios.block.VoidErosionBlock;
import cn.blockforge.generated.floralritebloomcurios.block.EternalPortalBlock;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class FRBBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<RitualAltarBlock> RITUAL_ALTAR = BLOCKS.register("ritual_altar",
            () -> new RitualAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(3.0F, 8.0F)
                    .sound(SoundType.STONE).lightLevel(s -> 5).noOcclusion()));

    /** 创造模式花仪祭坛：只允许创造玩家使用，跳过环境条件但仍按正常仪式消耗供品。 */
    public static final RegistryObject<CreativeRitualAltarBlock> CREATIVE_RITUAL_ALTAR =
            BLOCKS.register("creative_ritual_altar", () -> new CreativeRitualAltarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 8.0F)
                            .sound(SoundType.AMETHYST).lightLevel(s -> 10).noOcclusion()));

    /** 花仪台座：骨粉催熟草方块随机出现，台上放置仪式供品。空手可破坏。 */
    public static final RegistryObject<FloralAltarBlock> FLORAL_ALTAR = BLOCKS.register("floral_altar",
            () -> new FloralAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).strength(0.2F)
                    .sound(net.minecraft.world.level.block.SoundType.GRASS).noOcclusion()));

    /** 七种花灵作物。 */
    public static final Map<CharmType, RegistryObject<FlowerPlantBlock>> FLOWERS = new EnumMap<>(CharmType.class);

    /** r74：月见草作物（六阶段生长，收获 1~2 个月见草；用于月之庇护仪式链）。 */
    public static final RegistryObject<cn.blockforge.generated.floralritebloomcurios.block.EveningPrimroseBlock> EVENING_PRIMROSE =
            BLOCKS.register("evening_primrose",
                    () -> new cn.blockforge.generated.floralritebloomcurios.block.EveningPrimroseBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                                    .noCollission().noLootTable().sound(SoundType.CROP)
                                    .lightLevel(s -> s.getValue(cn.blockforge.generated.floralritebloomcurios.block.EveningPrimroseBlock.AGE)
                                            == cn.blockforge.generated.floralritebloomcurios.block.EveningPrimroseBlock.MAX_AGE ? 3 : 0)));

    static {
        for (CharmType type : CharmType.ALL) {
            FLOWERS.put(type, BLOCKS.register(type.id + "_flower",
                    () -> new FlowerPlantBlock(type,
                            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                                    .noCollission().noLootTable().sound(SoundType.CROP)
                                    .lightLevel(s -> s.getValue(FlowerPlantBlock.AGE) == FlowerPlantBlock.MAX_AGE ? 3 : 0))));
        }
    }

    /** 生之焰：焚尽阶段的 Boss 专用火焰（地面附层方块，无碰撞；踩上去 2 点 inFire + 点燃）。 */
    public static final RegistryObject<LifeFireBlock> LIFE_FIRE = BLOCKS.register("life_fire",
            () -> new LifeFireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE).noCollission().instabreak().noLootTable()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> 15)));

    /** 虚空侵蚀地块附层（G2）：虚空裂隙在脚下铺设，踩上去 2 点魔法伤害 + 叠 void_erosion 效果。 */
    public static final RegistryObject<VoidErosionBlock> VOID_EROSION = BLOCKS.register("void_erosion",
            () -> new VoidErosionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak().noLootTable()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> 8)));

    /** 世界之花：种下后缓慢绽放，成熟时迎来世界之花 Boss。 */
    public static final RegistryObject<WorldFlowerBlock> WORLD_FLOWER = BLOCKS.register("world_flower",
            () -> new WorldFlowerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().noLootTable().sound(SoundType.WOOL)
                    .lightLevel(s -> s.getValue(WorldFlowerBlock.AGE) * 3)));

    /** 永恒之门：世界之花开门演出生成的传送门（右键传送，不可被生存破坏）。 */
    public static final RegistryObject<EternalPortalBlock> ETERNAL_PORTAL = BLOCKS.register("eternal_portal",
            () -> new EternalPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).noCollission().noOcclusion()
                    .strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.GLASS)
                    .lightLevel(s -> 11)));

    private FRBBlocks() {}
}
