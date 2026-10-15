package cn.blockforge.generated.floralritebloomcurios.block;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

/**
 * r74：月见草作物——六阶段生长（0..5），成熟后右键或破坏收获 1~2 个月见草，
 * 享受时运加成（与原版马铃薯 / 胡萝卜同类行为）。
 *
 * - 土壤：耕地或泥土类
 * - 光照：生长需亮度 ≥ 8（白天照常生长）
 * - 收获时机：age == MAX_AGE 任意时刻右键
 */
public class EveningPrimroseBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 5);
    public static final int MAX_AGE = 5;

    /** 各阶段碰撞箱（格随生长长高，与六张贴图 8/13/19/24/27/30 像素对应。 */
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
    };

    public EveningPrimroseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.FARMLAND) || below.is(BlockTags.DIRT);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 与 FlowerPlantBlock 一致：14% 概率 / tick，亮度 >= 8
        if (state.getValue(AGE) < MAX_AGE && random.nextFloat() < 0.14F
                && level.getRawBrightness(pos, 0) >= 8) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, state.getValue(AGE) + 1));
        }
    }

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(AGE, Math.min(state.getValue(AGE) + 1, MAX_AGE)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) == MAX_AGE && !level.isClientSide) {
            harvest(level, pos, state, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** r74：收获 = 1 + [0, fortune/3] 颗月见草，享受时运加成（与土豆/胡萝卜一致行为）。 */
    private void harvest(Level level, BlockPos pos, BlockState state, Player player) {
        int fortune = player.getMainHandItem().getEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE);
        int count = 1 + level.random.nextInt(2); // 1..2
        if (fortune > 0) {
            count += level.random.nextInt(fortune + 1); // +0..fortune
        }
        popResource(level, pos, new ItemStack(FRBItems.EVENING_PRIMROSE.get(), count));
        level.destroyBlock(pos, false, player);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(FRBItems.EVENING_PRIMROSE_SEED.get());
    }
}