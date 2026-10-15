package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 附魔与声望：
 * 【星界花·群星的彼岸】在附魔台选择附魔时提升 2 级（终极饰品 +4 级）；
 * 【森语藤·生命之呼唤】使村民视玩家为英雄声望，首次交易面板打开时全单立减（每多 1 件花仪饰品多减 1）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class CharmUtilityEvents {

    @SubscribeEvent
    public static void onEnchantLevelSet(EnchantmentLevelSetEvent event) {
        if (event.getLevel().isClientSide) return;
        BlockPos pos = event.getPos();
        Player user = event.getLevel().getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6.0D, false);
        if (user == null) return;
        int boost;
        if (FRBState.hasUltimate(user)) boost = 4;
        else if (FRBState.active(user, CharmType.ASTRAL_BLOOM)) boost = 2;
        else return;
        event.setEnchantLevel(Math.min(30, event.getEnchantLevel() + boost));
    }

    @SubscribeEvent
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof AbstractVillager villager)) return;
        Player player = event.getEntity();
        if (!FRBState.active(player, CharmType.FOREST_WHISPER)) return;
        String flag = "FRBRep_" + player.getUUID();
        if (villager.getPersistentData().getBoolean(flag)) return;
        villager.getPersistentData().putBoolean(flag, true);
        int discount = 3 + FRBState.flowerCharmCount(player);
        for (MerchantOffer offer : villager.getOffers()) {
            offer.addToSpecialPriceDiff(-discount);
        }
    }

    private CharmUtilityEvents() {}
}
