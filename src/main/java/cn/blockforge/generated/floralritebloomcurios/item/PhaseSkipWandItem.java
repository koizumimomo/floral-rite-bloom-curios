package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * r87 创造模式调试工具「相位之杖」：右键芙莉艾丝 → 立即强制进入下一阶段。
 * 仅创造模式生效；不消耗、不损耐久。复用 beginTransition 正常转阶段流程。
 * 贴图复用 textures/entity/boss_phase_sigils/zhen.png（复制为 item/frb_phase_wand.png）。
 */
public class PhaseSkipWandItem extends Item {

    public PhaseSkipWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide) {
            return target instanceof FriliasEntity ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp) || !sp.isCreative()) return InteractionResult.PASS;
        if (!(target instanceof FriliasEntity boss)) return InteractionResult.PASS;
        ServerLevel level = (ServerLevel) player.level();
        boolean ok = boss.frb$debugForceNextPhase(level);
        sp.displayClientMessage(Component.translatable(
                ok ? "frb.debug.phase_skip" : "frb.debug.phase_skip_fail")
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        if (ok) {
            level.playSound(null, boss.getX(), boss.getY() + 1.0D, boss.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F, 1.4F);
            sp.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("frb.debug.phase_skip_wand.hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
