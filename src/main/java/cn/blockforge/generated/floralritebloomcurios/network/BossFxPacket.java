package cn.blockforge.generated.floralritebloomcurios.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C：Boss 战镜头演出包。
 *
 * fx 是位标志，可同时叠加多种效果：
 *   FX_SHAKE     — 镜头震动 + FOV 脉冲
 *   FX_FLASH     — 全屏白闪
 *   FX_SHOCKWAVE — 径向扭曲冲击波
 *   FX_GRAY      — 全屏灰度褪色（枯萎）
 *   FX_TINT      — 转阶段主题色浸染（color = 0xRRGGBB）
 *   FX_MARKER    — 头顶标记（叠加在玩家头顶 1.5 格，本地持续渲染）
 *   FX_CIRCLE    — 地面预警圈（pos + radius + color，本地持续渲染）
 *
 * strength 0~1 控制强度，duration 为 tick 数。
 */
public record BossFxPacket(int fx, float strength, int duration, int color) {

    public static final int FX_SHAKE = 1;
    public static final int FX_FLASH = 2;
    public static final int FX_SHOCKWAVE = 4;
    public static final int FX_GRAY = 8;
    public static final int FX_TINT = 16;
    /** 头顶标记（duration 内每 tick 在目标玩家头顶 1.5 格渲染 FX_MARKER 帧）。 */
    public static final int FX_MARKER = 32;
    /** 地面预警圈（duration 内每 tick 在 groundPos + radius 处渲染预警环）。 */
    public static final int FX_CIRCLE = 64;
    /** 万花凋零终结技（duration 内持续触发 boss_finale shader pass，uBossPos 锚点）。 */
    public static final int FX_FINALE = 128;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(fx);
        buf.writeFloat(strength);
        buf.writeVarInt(duration);
        buf.writeVarInt(color);
    }

    public static BossFxPacket decode(FriendlyByteBuf buf) {
        return new BossFxPacket(buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }
}