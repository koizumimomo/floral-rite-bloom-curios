package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 不知道哪里来的酒：
 * <ul>
 *   <li>右键饮用 → 自身获得「喝醉了」效果（30 秒）</li>
 *   <li>右键生物 → 给目标施加「喝醉了」效果（30 秒），可用于让 Boss 变形停手</li>
 * </ul>
 * 物品来源：聊天栏输入带「酒」字的消息即可获得（无需作弊）。
 */
public class MysteriousWineItem extends Item {

    /**
     * 喝醉效果持续时长（tick）：30 秒。
     * <p>
     * <b>改动此值会影响客户端动画时钟</b>：{@code EntityAnimation} 用「本值 − 效果剩余时长」
     * 反推动画已进行的秒数，所以它必须等于实际施加效果时用的时长。
     */
    public static final int DRUNK_DURATION = 20 * 30;
    /** 饮用动画时长（tick）。 */
    private static final int USE_DURATION = 32;
    /** 使用后冷却（tick）：1 秒，防止连点刷屏。 */
    private static final int COOLDOWN = 20;

    public MysteriousWineItem(Properties props) {
        super(props);
    }

    // ---- 自身饮用 ----

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(FRBEffects.DRUNK.get(), DRUNK_DURATION, 0, true, true));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        if (entity instanceof Player player && !player.isCreative()) {
            stack.shrink(1);
        }
        return stack;
    }

    // ---- 右键生物施加喝醉 ----

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        target.addEffect(new MobEffectInstance(FRBEffects.DRUNK.get(), DRUNK_DURATION, 0, true, true));
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.floral_rite_bloom_curios.mysterious_wine.hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
