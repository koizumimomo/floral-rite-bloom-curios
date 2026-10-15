package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 聊天触发道具：玩家在聊天栏发送包含「酒」字的消息时，
 * 获得一份「不知道哪里来的酒」。无需作弊、不限制游戏模式。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class ChatRewardEvents {

    private ChatRewardEvents() {}

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String msg = event.getMessage().getString();
        if (msg == null || !msg.contains("酒")) return;
        // 给玩家一份酒（若背包满则掉落到脚边）
        ItemStack wine = new ItemStack(FRBItems.MYSTERIOUS_WINE.get());
        if (!player.getInventory().add(wine)) {
            player.drop(wine, false);
        }
        player.displayClientMessage(Component.translatable("frb.chat.wine_obtained")
                .withStyle(ChatFormatting.GOLD), true);
    }
}
