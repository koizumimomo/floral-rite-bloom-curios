package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** 运行环境小工具：仅客户端安全地取本地玩家（tooltip 渲染用）。 */
public final class FRBEnv {

    /** 客户端本地玩家；非客户端或无世界时返回 null。 */
    public static Player clientPlayer() {
        if (FMLEnvironment.dist != Dist.CLIENT) return null;
        return Minecraft.getInstance().player;
    }

    private FRBEnv() {}
}
