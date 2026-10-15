package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 速度参数承载目标坐标，40 刻沿弧线汇聚至花心。 */
public final class ConvergingPetalParticle extends TextureSheetParticle {
    private final double startX, startY, startZ, targetX, targetY, targetZ;

    private ConvergingPetalParticle(ClientLevel level, double x, double y, double z,
                                    double tx, double ty, double tz, SpriteSet sprites) {
        super(level, x, y, z);
        startX = x; startY = y; startZ = z;
        targetX = tx; targetY = ty; targetZ = tz;
        lifetime = 40;
        hasPhysics = false;
        quadSize = 0.10F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z; oRoll = roll;
        if (++age >= lifetime) { remove(); return; }
        double t = age / (double) lifetime;
        double arc = Math.sin(t * Math.PI) * 0.65D;
        x = startX + (targetX - startX) * t + arc * Math.cos(t * Math.PI * 2);
        y = startY + (targetY - startY) * t + arc;
        z = startZ + (targetZ - startZ) * t + arc * Math.sin(t * Math.PI * 2);
        roll += 0.12F;
        alpha = Math.min(1.0F, (lifetime - age) / 8.0F);
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, tx, ty, tz) ->
                new ConvergingPetalParticle(level, x, y, z, tx, ty, tz, sprites);
    }
}
