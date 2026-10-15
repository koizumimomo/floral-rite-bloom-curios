package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 丰饶的通用事件：左键锁定走客户端准心拾取 + LockTargetPacket（见 BountifulClient / FRBNetwork），
 * 服务端只做校验与加锁；玩家 tick 维持手持属性加成。
 *
 * r75：shift + 右键取消当前锁定目标（用户截图 #10）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class BountifulEvents {

    /** 锁定目标（服务端校验后调用，客户端拾取见 BountifulClient）。 */
    public static void lockTarget(Player player, LivingEntity target) {
        player.getPersistentData().putUUID(BountifulItem.TAG_LOCK, target.getUUID());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), player);
        player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
        player.displayClientMessage(Component.translatable("frb.bountiful.locked", target.getName())
                .withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * r77：shift + 左键（PlayerInteractEvent.LeftClickBlock 是挖方块，改用
     * LivingAttackEvent / AttackEntityEvent 区分。丰饶的"锁定"是用 attack key 客户端拾取
     * 触发的——所以取消锁定用 AttackEntityEvent + shift 检测；这样和"左键选目标"手感一致，
     * 不会出现"shift + 任意右键"误触取消）。
     */
    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isShiftKeyDown()) return;
        boolean mainHand = player.getMainHandItem().getItem() instanceof BountifulItem;
        boolean offHand = player.getOffhandItem().getItem() instanceof BountifulItem;
        if (!mainHand && !offHand) return;
        if (!player.getPersistentData().hasUUID(BountifulItem.TAG_LOCK)) return;
        player.getPersistentData().remove(BountifulItem.TAG_LOCK);
        player.displayClientMessage(Component.translatable("frb.bountiful.unlock")
                .withStyle(ChatFormatting.YELLOW), true);
        player.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.8F, 1.4F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        BountifulItem.tickHeld(player);
        refreshLockGlow(player);
    }

    /** 锁定无时限：手持丰饶期间持续刷新锁定目标的发光，让"还在锁定"始终可见。 */
    private static void refreshLockGlow(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof BountifulItem)) return;
        if (!player.getPersistentData().hasUUID(BountifulItem.TAG_LOCK)) return;
        if (player.tickCount % 60 != 0) return;
        if (((ServerLevel) player.level()).getEntity(player.getPersistentData().getUUID(BountifulItem.TAG_LOCK))
                instanceof LivingEntity locked && locked.isAlive()) {
            locked.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0), player);
        }
    }

    private BountifulEvents() {}
}
