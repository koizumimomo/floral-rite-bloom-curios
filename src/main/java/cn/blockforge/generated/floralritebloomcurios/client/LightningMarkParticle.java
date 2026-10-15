package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 落雷头顶标记粒子：r64 用户反馈"粒子效果太大"——从 scale=1.2 缩小到 scale=0.5（缩小约 60%）。
 * 复用 EldenParticle.glow 让 4 帧 electricity_1~4 在 SpriteSet 内随机挑选，达到随机闪烁效果。
 */
public final class LightningMarkParticle {

    private LightningMarkParticle() {}

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        // scale=0.5：与原版粒子默认 0.5F 一致，符合玩家"缩小 60%"的诉求。
        return EldenParticle.glow(sprites, 0.5F, 30, -0.001F);
    }
}