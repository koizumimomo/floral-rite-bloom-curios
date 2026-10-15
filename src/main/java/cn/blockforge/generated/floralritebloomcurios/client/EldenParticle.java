package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

/**
 * 艾尔登风格发光粒子（贴图来自 Iron-Elden-Ring-Particle-library）。
 * 无重力漂浮、缓慢旋转、随寿命平方淡出；growth 控制每帧尺寸增减（负值收缩、正值扩张）。
 */
public class EldenParticle extends TextureSheetParticle {

    private final float growth;
    private float rollSpeed;

    protected EldenParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz,
                            float scale, int lifetime, float growth, float alpha) {
        super(level, x, y, z);
        this.setSize(0.2F, 0.2F);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize = scale * (0.85F + this.random.nextFloat() * 0.3F);
        this.lifetime = lifetime;
        this.growth = growth;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.rollSpeed = (this.random.nextFloat() - 0.5F) * 0.02F;
        this.setAlpha(alpha);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.roll += this.rollSpeed;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.quadSize *= 1.0F + this.growth;
        float life = (float) this.age / (float) this.lifetime;
        this.setAlpha(Math.max(0.0F, 1.0F - life * life));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** 漂浮渐隐的微粒 / 光球。 */
    public static ParticleProvider<SimpleParticleType> glow(SpriteSet sprites, float scale, int lifetime, float growth) {
        return (type, level, x, y, z, vx, vy, vz) -> {
            EldenParticle p = new EldenParticle((ClientLevel) level, x, y, z, vx, vy, vz,
                    scale, lifetime, growth, 1.0F);
            p.pickSprite(sprites);
            return p;
        };
    }

    /** 快速扩张的脉冲环。 */
    public static ParticleProvider<SimpleParticleType> ring(SpriteSet sprites, float scale, float growth) {
        return (type, level, x, y, z, vx, vy, vz) -> {
            EldenParticle p = new EldenParticle((ClientLevel) level, x, y, z, vx, vy, vz,
                    scale, 14, growth, 0.95F);
            p.pickSprite(sprites);
            return p;
        };
    }
}
