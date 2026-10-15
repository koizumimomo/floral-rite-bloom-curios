package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：服务端确认饰品主动技能成功触发，通知客户端刷新冷却 HUD。
 * r96：修复"按键即进冷却"——只有服务端真正放行（饰品在位、冷却归零、落点合法）才下发。
 */
public record AbilityCooldownPacket(int kindOrdinal) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kindOrdinal);
    }

    public static AbilityCooldownPacket decode(FriendlyByteBuf buf) {
        return new AbilityCooldownPacket(buf.readVarInt());
    }
}
