package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Boss 战 BGM：战斗中（开场演出结束）播放，boss 消失自动停止。
 * <p>
 * r90：改用自管理 {@link TickableSoundInstance} 实现渐弱效果——
 * boss 终结时（die 或 milleFlorumExit），客户端检测到 isBattleMusicOn()=false 后
 * 在 40 tick 内线性降低音量到 0，到 0 后 stop()。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBBossMusicClient {

    private static FadeOutSound bgmInstance = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.player.tickCount % 20 != 0) return;

        boolean battle = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FriliasEntity f && f.isBattleMusicOn()) {
                battle = true;
                break;
            }
        }
        SoundManager sm = mc.getSoundManager();
        if (battle) {
            // 战斗中：若未播放则启动
            if (bgmInstance == null || bgmInstance.isStopped()) {
                bgmInstance = new FadeOutSound();
                sm.play(bgmInstance);
            }
        } else {
            // 非战斗：触发渐弱（若正在播放且未开始渐弱）
            if (bgmInstance != null && !bgmInstance.isStopped() && !bgmInstance.isFadingOut()) {
                bgmInstance.startFadeOut();
            }
        }
    }

    /** 自管理可循环 TickableSoundInstance，支持渐弱。 */
    private static class FadeOutSound extends AbstractTickableSoundInstance {
        private static final int FADE_TICKS = 40; // 2 秒渐弱
        private boolean fadingOut = false;
        private int fadeTick = 0;

        FadeOutSound() {
            super(FRBSounds.BOSS_MUSIC.get(), SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
            this.volume = 1.0F;
            this.delay = 0;
            this.relative = true;
        }

        boolean isFadingOut() {
            return fadingOut;
        }

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

    private FRBBossMusicClient() {}
}
