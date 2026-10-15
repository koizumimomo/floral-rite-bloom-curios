package cn.blockforge.generated.floralritebloomcurios.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 森语藤的“催生”能力：对范围内作物 / 植物做一次骨粉判定，加速其生长。
 */
public final class GrowthBoost {

    public static void boost(ServerLevel level, BlockPos center, int radius, int tries) {
        var r = level.getRandom();
        for (int i = 0; i < tries; i++) {
            BlockPos p = center.offset(
                    r.nextIntBetweenInclusive(-radius, radius),
                    r.nextIntBetweenInclusive(-1, 2),
                    r.nextIntBetweenInclusive(-radius, radius));
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof BonemealableBlock growable
                    && growable.isValidBonemealTarget(level, p, state, false)) {
                growable.performBonemeal(level, r, p, state);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 4, 0.3D, 0.4D, 0.3D, 0.05D);
            }
        }
    }

    private GrowthBoost() {}
}
