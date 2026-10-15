package cn.blockforge.generated.floralritebloomcurios.block;

import cn.blockforge.generated.floralritebloomcurios.block.entity.FloralAltarBlockEntity;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.ritual.RitualManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 花仪台座：手持物品右键把“一份”供品放上台面（空手右键取回）；
 * 手持无相花种右键则以其为阵眼举行花仪——台座上的供品会被消耗。
 * 骨粉催熟草方块有小概率让花仪台座破土而出。空手即可破坏，掉落台上供品。
 */
public class FloralAltarBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D),
            Block.box(6.5D, 3.0D, 6.5D, 9.5D, 7.0D, 9.5D),
            Block.box(3.0D, 6.0D, 3.0D, 13.0D, 12.0D, 13.0D));

    public FloralAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloralAltarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        // 手持无相花种：以本台座为阵眼举行花仪（供品取周围台座 + 背包）
        if (FRBItems.isFormlessSeed(held) && level instanceof ServerLevel slevel) {
            if (RitualManager.attempt(slevel, pos, player)) {
                return InteractionResult.sidedSuccess(false);
            }
            return InteractionResult.PASS;
        }
        // 手持丰饶：尝试花仪锻造（七仪各铭刻一次）；不满足也不把丰饶放上供品台
        if (held.getItem() instanceof cn.blockforge.generated.floralritebloomcurios.item.BountifulItem
                && level instanceof ServerLevel slevel) {
            RitualManager.attemptForge(slevel, pos, player, false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof FloralAltarBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (held.isEmpty()) {
                // 空手（未潜行也可）：取回供品
                ItemStack stored = be.getOffering();
                if (!stored.isEmpty()) {
                    be.setOffering(ItemStack.EMPTY);
                    if (!player.addItem(stored)) {
                        player.drop(stored, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
                }
            } else if (be.isEmpty()) {
                ItemStack one = held.copyWithCount(1);
                be.setOffering(one);
                held.shrink(1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.5F);
            } else if (be.getOffering().is(held.getItem())
                    && be.getOffering().getCount() < be.getOffering().getMaxStackSize()) {
                be.getOffering().grow(1);
                held.shrink(1);
                be.setChanged(); // 台座 setChanged 已重写为自动发同步包
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.5F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FloralAltarBlockEntity be) {
            ItemStack stored = be.getOffering();
            if (!stored.isEmpty()) {
                popResource(level, pos, stored);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
