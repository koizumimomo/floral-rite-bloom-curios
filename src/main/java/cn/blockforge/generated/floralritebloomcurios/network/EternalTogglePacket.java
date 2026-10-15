package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * C2S：佩戴【万花万草之灵】时空手 Shift 右键切换聚合效果。
 * offHand=true → 寒风铃传送回溯开关；false → 森语藤催熟开关。
 * （饰品佩戴在 Curios 槽位时不持于手，PlayerInteractEvent.RightClickItem
 * 不会触发，因此由客户端 RightClickEmpty 事件发送本包。）
 */
public record EternalTogglePacket(boolean offHand) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(offHand);
    }

    public static EternalTogglePacket decode(FriendlyByteBuf buf) {
        return new EternalTogglePacket(buf.readBoolean());
    }
}
