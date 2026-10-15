package cn.blockforge.generated.floralritebloomcurios.block;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.ritual.RitualManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 创造模式专用花仪祭坛：无视环境、高度、天气和群系条件。 */
public final class CreativeRitualAltarBlock extends Block {
    public CreativeRitualAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("frb.creative_altar.only_creative")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // r74：手持月之水晶 → 直接产出月之庇护（不消耗任何材料，便于测试）
        if (held.is(FRBItems.LUNAR_SHARD.get())) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (level instanceof ServerLevel serverLevel) {
                ItemStack result = new ItemStack(FRBItems.LUNAR_CROWN.get());
                if (!player.getInventory().add(result)) player.drop(result, false);
                player.displayClientMessage(Component.translatable("frb.lunar.crown.done")
                        .withStyle(ChatFormatting.GOLD), false);
            }
            return InteractionResult.SUCCESS;
        }
        // r74：手持紫水晶碎片 → 直接产出月之水晶（避免月相概率问题）
        if (held.is(Items.AMETHYST_SHARD)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (level instanceof ServerLevel serverLevel) {
                ItemStack result = new ItemStack(FRBItems.LUNAR_SHARD.get());
                if (!player.getInventory().add(result)) player.drop(result, false);
                player.displayClientMessage(Component.translatable("frb.lunar.shard.done")
                        .withStyle(ChatFormatting.GOLD), false);
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.is(FRBItems.FORMLESS_SEED.get())) {
            // 手持丰饶：创造祭坛跳过环境的锻造
            if (held.getItem() instanceof cn.blockforge.generated.floralritebloomcurios.item.BountifulItem) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (level instanceof ServerLevel serverLevel) {
                    RitualManager.attemptForge(serverLevel, pos, player, true);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("frb.creative_altar.hint")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel
                && RitualManager.attemptCreative(serverLevel, pos, player)) {
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("frb.ritual.fail.cost")
                .withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }
}
