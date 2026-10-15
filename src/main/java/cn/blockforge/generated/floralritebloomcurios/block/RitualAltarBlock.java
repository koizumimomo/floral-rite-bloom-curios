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

/**
 * 花仪祭坛：手持无相花种右键祭坛举行仪式；手持紫水晶碎片 / 月之水晶走月相仪式。
 * 空手右键查看引导。
 */
public class RitualAltarBlock extends Block {

    public RitualAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(
                    held.is(FRBItems.FORMLESS_SEED.get())
                            || held.is(Items.AMETHYST_SHARD)
                            || held.is(FRBItems.LUNAR_SHARD.get()));
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        // r74：月之水晶仪式（手持月之水晶碎片）
        if (held.is(FRBItems.LUNAR_SHARD.get())) {
            if (RitualManager.attemptLunarCrown(serverLevel, pos, player)) return InteractionResult.SUCCESS;
            return InteractionResult.FAIL;
        }
        // r74：月之水晶仪式（手持紫水晶碎片）
        if (held.is(Items.AMETHYST_SHARD)) {
            if (RitualManager.attemptLunarShard(serverLevel, pos, player)) return InteractionResult.SUCCESS;
            return InteractionResult.FAIL;
        }
        if (held.is(FRBItems.FORMLESS_SEED.get())) {
            // 现有七花仪式；失败时回落到月见草种子仪式（环境夜 + 紫水晶簇 + 水）
            if (RitualManager.attempt(serverLevel, pos, player)) return InteractionResult.SUCCESS;
            if (RitualManager.attemptEveningPrimroseSeed(serverLevel, pos, player)) return InteractionResult.SUCCESS;
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
            player.displayClientMessage(
                    Component.translatable("frb.ritual.fail.cond").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        player.displayClientMessage(
                Component.translatable("frb.altar.hint").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }
}
