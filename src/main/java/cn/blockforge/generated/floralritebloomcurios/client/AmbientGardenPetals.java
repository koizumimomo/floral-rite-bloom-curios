package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒花园的环境花瓣：玩家身处维度内时，四周随机飘落各色花瓣
 * （类似灵魂沙 Valley 的灵魂粒子氛围）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class AmbientGardenPetals {

    private static final ResourceLocation GARDEN =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "eternal_garden");
    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!GARDEN.equals(mc.level.dimension().location())) return;
        // 平均每 3 tick 一片，玩家周围 ±20 格、高度 -4~+12 随机飘落
        if (RANDOM.nextInt(3) == 0) {
            double x = mc.player.getX() + Mth.randomBetween(RANDOM, -20.0F, 20.0F);
            double y = mc.player.getY() + Mth.randomBetween(RANDOM, -4.0F, 12.0F);
            double z = mc.player.getZ() + Mth.randomBetween(RANDOM, -20.0F, 20.0F);
            mc.level.addParticle(FRBParticles.GARDEN_PETAL.get(), x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private AmbientGardenPetals() {}
}
