package cn.blockforge.generated.floralritebloomcurios.world;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraftforge.registries.DeferredRegister;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 永恒花园专用生成器：双倍频 SimplexNoise 起伏的草甸地形（基岩 + 泥土 + 草方块，
 * 表面随机苔藓块/苔石等变体，花草与紫水晶簇点缀）。完全自写地形生成：
 * - 禁止一切结构生成（createStructures 为空）；
 * - 世界生成阶段不生成生物（运行时自然刷怪由 GardenProtection 拦截）。
 * 维度内不可破坏（见 GardenProtection）。
 */
public final class EternalGardenChunkGenerator extends FlatLevelSource {
    public static final Codec<EternalGardenChunkGenerator> CODEC =
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings")
                    .xmap(EternalGardenChunkGenerator::new, EternalGardenChunkGenerator::settings)
                    .codec();

    /** mod 构造期挂到 mod 总线，FML 在注册窗口期统一写入（直接 Registry.register 会撞上已冻结的原版注册表）。 */
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, FloralRiteBloom.MOD_ID);

    public static final Supplier<Codec<EternalGardenChunkGenerator>> CODEC_SUPPLIER =
            CHUNK_GENERATORS.register("eternal_garden", () -> CODEC);

    public EternalGardenChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---- 噪声地形（r64：超平坦，无高低差） ----

    /** 固定种子保证地形稳定可复现（维度不可破坏，种子变化没有 Gameplay 意义）。 */
    private static final SimplexNoise NOISE_LOW = new SimplexNoise(RandomSource.create(0x46EA6DE4L));
    private static final SimplexNoise NOISE_HIGH = new SimplexNoise(RandomSource.create(0xB0505EEDL));
    private static final int BASE_Y = 5;

    /**
     * 地表高度（r64）：恒为 BASE_Y。
     * 用户反馈"维度还是修改一下生成规律，不要高低差了，还是超平坦吧"——
     * 保留两个噪声对象以维持原本的随机种子分布，
     * 但 surfaceHeight 只返回固定高度，地形变为超平坦。
     */
    private static int surfaceHeight(int x, int z) {
        // 仍然取样以保持与之前 RandomSource 相同的调用顺序（避免破坏现有种子）
        NOISE_LOW.getValue(x * 0.012D, z * 0.012D);
        NOISE_HIGH.getValue(x * 0.05D + 100.0D, z * 0.05D - 100.0D);
        return BASE_Y;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunkAccess) {
        // 不调用 super：超平坦只会铺固定高度层级，且此处拿不到 heightmap，装饰列会全部失效。
        // 改为按噪声函数直接推算地表高度，同步生成（与原版 FlatLevelSource 一致）。
        return CompletableFuture.completedFuture(generate(chunkAccess));
    }

    /** 表面随机替换的变体石头（顺序即概率累加序）。 */
    private static final Block[] SURFACE_VARIANTS = {
            Blocks.MOSS_BLOCK, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE,
            Blocks.STONE, Blocks.ANDESITE, Blocks.COARSE_DIRT, Blocks.PODZOL,
            Blocks.AMETHYST_BLOCK};
    private static final float[] SURFACE_CHANCE = {0.10F, 0.05F, 0.04F, 0.03F, 0.03F, 0.02F, 0.02F, 0.015F};

    /** 草地上随机绽放的小花。 */
    private static final Block[] FLOWERS = {
            Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
            Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.ORANGE_TULIP, Blocks.RED_TULIP,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.TORCHFLOWER};

    /** 表面装饰概率（每列）：短草 / 小花 / 紫水晶簇（单格高，维度内不出现两格高植物）。 */
    private static final float CHANCE_GRASS = 0.035F;
    private static final float CHANCE_FLOWER = 0.055F;
    private static final float CHANCE_AMETHYST = 0.004F;

    private ChunkAccess generate(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = pos.getMinBlockX() + x;
                int wz = pos.getMinBlockZ() + z;
                int h = surfaceHeight(wx, wz);
                // 每列独立随机（由 chunk 坐标 + 列坐标定种，重生成结果稳定）
                RandomSource random = RandomSource.create(
                        ((long) pos.x * 0x4F9939C93L) ^ ((long) pos.z * 0x1D3E1A5F53L)
                                ^ ((long) x * 0x2545F4914F6CDD1DL) ^ ((long) z * 0x9E3779B97F4A7C15L));
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

                // 基岩 + 泥土填充
                chunk.setBlockState(m.set(wx, minY, wz), Blocks.BEDROCK.defaultBlockState(), false);
                for (int y = minY + 1; y < h; y++) {
                    chunk.setBlockState(m.set(wx, y, wz), Blocks.DIRT.defaultBlockState(), false);
                }

                // 表面：草方块按概率替换为苔藓块/苔石等变体
                BlockState surface = Blocks.GRASS_BLOCK.defaultBlockState();
                float roll = random.nextFloat();
                for (int i = 0; i < SURFACE_VARIANTS.length; i++) {
                    if (roll < SURFACE_CHANCE[i]) {
                        surface = SURFACE_VARIANTS[i].defaultBlockState();
                        break;
                    }
                    roll -= SURFACE_CHANCE[i];
                }
                chunk.setBlockState(m.set(wx, h, wz), surface, false);

                // 只有草方块/苔藓块能承载植物与水晶
                if (!surface.is(Blocks.GRASS_BLOCK) && !surface.is(Blocks.MOSS_BLOCK)) continue;
                BlockPos above = new BlockPos(wx, h + 1, wz);
                float deco = random.nextFloat();
                if (deco < CHANCE_GRASS) {
                    chunk.setBlockState(above, Blocks.GRASS.defaultBlockState(), false);
                } else if (deco < CHANCE_GRASS + CHANCE_FLOWER) {
                    chunk.setBlockState(above,
                            FLOWERS[random.nextInt(FLOWERS.length)].defaultBlockState(), false);
                } else if (deco < CHANCE_GRASS + CHANCE_FLOWER + CHANCE_AMETHYST) {
                    chunk.setBlockState(above, Blocks.AMETHYST_CLUSTER.defaultBlockState(), false);
                }
            }
        }
        return chunk;
    }

    // ---- 禁结构 / 无原生成生物 / 供寻路与出生点使用的地表查询 ----

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState state,
                                 StructureManager structureManager, ChunkAccess chunk,
                                 StructureTemplateManager templateManager) {
        // 永恒花园是纯 Boss 挑战场地：禁止一切结构（含其他 mod 的结构）生成
    }

    @Override
    public void spawnOriginalMobs(net.minecraft.server.level.WorldGenRegion region) {
        // 世界生成阶段不放置任何生物；运行时自然刷怪由 GardenProtection#onCheckSpawn 统一拦截
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level,
                             RandomState random) {
        return surfaceHeight(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minY = level.getMinBuildHeight();
        int h = surfaceHeight(x, z);
        BlockState[] states = new BlockState[h + 1 - minY];
        states[0] = Blocks.BEDROCK.defaultBlockState();
        for (int i = 1; i < states.length - 1; i++) {
            states[i] = Blocks.DIRT.defaultBlockState();
        }
        states[states.length - 1] = Blocks.GRASS_BLOCK.defaultBlockState();
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> info, RandomState random,
                                   BlockPos pos) {
        info.add("Eternal Garden noise height: " + surfaceHeight(pos.getX(), pos.getZ()));
    }
}
