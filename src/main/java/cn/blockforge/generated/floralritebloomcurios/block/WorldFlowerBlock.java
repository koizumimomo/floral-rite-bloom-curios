package cn.blockforge.generated.floralritebloomcurios.block;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

/**
 * 世界之花：种下“世界之花种子”后自然缓慢绽放，也可用骨粉催熟（进花园的钥匙）。
 * 完全盛开后的右键即是门扉：主世界的花送你进入【永恒花园】，
 * 花园里的花则召唤“于死亡中绽放的花”（潜行右键返回）。破坏未成熟个体只会掉回种子。
 */
public class WorldFlowerBlock extends BushBlock implements net.minecraft.world.level.block.BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    public static final int MAX_AGE = 4;

    public WorldFlowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
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
        int age = state.getValue(AGE);
        // 只负责自然生长；完全盛开后的苏醒由玩家在世界之花上右键触发。
        if (age < MAX_AGE && random.nextFloat() < 0.05F) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    6, 0.2D, 0.3D, 0.2D, 0.01D);
        }
    }

    // ---- 骨粉催熟（进入永恒花园的关键路径） ----

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                         BlockState state, boolean isClient) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    8, 0.3D, 0.4D, 0.3D, 0.02D);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (state.getValue(AGE) < MAX_AGE) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("frb.worldflower.waiting")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() == Level.OVERWORLD) {
            // 进园门槛（万花齐放）在 tryOpenGate 内验证；通过则播放开门演出，
            // 演出结束在世界之花正上方生成永恒之门（右键门进入永恒花园）。
            cn.blockforge.generated.floralritebloomcurios.block.EternalPortalBlock
                    .tryOpenGate((ServerLevel) level, pos, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() == cn.blockforge.generated.floralritebloomcurios.world.EternalGarden.DIMENSION_KEY) {
            if (player.isShiftKeyDown()) {
                cn.blockforge.generated.floralritebloomcurios.world.EternalGarden.leave(serverPlayer);
            } else if (!cn.blockforge.generated.floralritebloomcurios.world.EternalGarden
                    .summon(serverPlayer, (ServerLevel) level, pos)) {
                serverPlayer.displayClientMessage(Component.translatable("frb.worldflower.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(FRBItems.SEED_OF_REALM.get());
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(AGE) < MAX_AGE && !player.isCreative()) {
            popResource(level, pos, new ItemStack(FRBItems.SEED_OF_REALM.get()));
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
