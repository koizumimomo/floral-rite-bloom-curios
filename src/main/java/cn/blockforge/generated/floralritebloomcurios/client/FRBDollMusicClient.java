package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r101：喝醉了果冻回弹 BGM——本地玩家持有「喝醉了」效果时循环播放玩偶音乐，
 * 效果消失后 2 秒渐弱停止（仿 friendswine doll music）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBDollMusicClient {

    private static FadeOutSound dollInstance = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.player.tickCount % 10 != 0) return;

        boolean drunk = mc.player.hasEffect(FRBEffects.DRUNK.get());
        SoundManager sm = mc.getSoundManager();
        if (drunk) {
            if (dollInstance == null || dollInstance.isStopped()) {
                dollInstance = new FadeOutSound();
                sm.play(dollInstance);
            }
        } else {
            if (dollInstance != null && !dollInstance.isStopped() && !dollInstance.isFadingOut()) {
                dollInstance.startFadeOut();
            }
        }
    }

    /** 自管理可循环 TickableSoundInstance，支持渐弱。 */
    private static class FadeOutSound extends AbstractTickableSoundInstance {
        private static final int FADE_TICKS = 40;
        private boolean fadingOut = false;
        private int fadeTick = 0;

        FadeOutSound() {
            super(FRBSounds.DOLL_MUSIC.get(), SoundSource.RECORDS, RandomSource.create());
            this.looping = true;
            this.volume = 0.8F;
            this.delay = 0;
            this.relative = true;
        }

        boolean isFadingOut() { return fadingOut; }

        void startFadeOut() {
            fadingOut = true;
            fadeTick = 0;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeTick++;
                float t = 1.0F - (float) fadeTick / FADE_TICKS;
                this.volume = Math.max(0.0F, t);
                if (fadeTick >= FADE_TICKS) {
                    this.volume = 0.0F;
                    this.stop();
                }
            }
        }
    }

    private FRBDollMusicClient() {}
}
