package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * 飘落花瓣粒子：旋转下坠并随风摇曳，颜色由注册时的常量决定。
 */
public class PetalParticle extends TextureSheetParticle {

    private final double swayPhase;
    private float rollSpeed;

    protected PetalParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz, int color) {
        super(level, x, y, z);
        this.setSize(0.1F, 0.1F);
        this.xd = vx * 0.35D;
        this.yd = vy * 0.2D - 0.05D;
        this.zd = vz * 0.35D;
        this.quadSize *= this.random.nextFloat() * 0.6F + 0.7F;
        this.roll = this.random.nextFloat() * (float) Math.PI;
        this.oRoll = this.roll;
        this.rollSpeed = (0.6F - this.random.nextFloat()) * 0.05F;
        this.gravity = 0.03F;
        this.lifetime = 40;
        this.swayPhase = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.setColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                (color & 255) / 255.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime || this.y < this.getGroundY()) {
            this.remove();
        } else {
            this.roll += this.rollSpeed;
            float sway = (float) Math.sin((this.age + this.swayPhase * 20.0D) * 0.12D) * 0.02F;
            this.move(this.xd + sway * 0.1D, this.yd, this.zd + sway * 0.1D);
            this.yd -= this.gravity;
            this.xd *= this.friction;
            this.zd *= this.friction;
        }
    }

    private double getGroundY() {
        return this.y - 64.0D; // 花瓣允许飘远，不做逐格贴地判定
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, int color) {
        return (type, level, x, y, z, vx, vy, vz) -> {
            PetalParticle p = new PetalParticle((ClientLevel) level, x, y, z, vx, vy, vz, color);
            p.pickSprite(sprites);
            return p;
        };
    }
}
