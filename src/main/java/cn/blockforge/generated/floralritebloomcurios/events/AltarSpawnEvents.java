package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 骨粉催熟草方块时，有小概率让一株花仪台座从草皮下“钻”出来
 * （在原地附近随机挑一块顶面为空气的草方块放置，不取消原版骨粉行为）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class AltarSpawnEvents {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onBonemealGrass(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!event.getBlock().is(Blocks.GRASS_BLOCK)) return;
        if (RANDOM.nextFloat() > 0.12F) return;

        BlockPos origin = event.getPos();
        for (int tries = 0; tries < 10; tries++) {
            BlockPos target = origin.offset(
                    RANDOM.nextInt(9) - 4, RANDOM.nextInt(5) - 2, RANDOM.nextInt(9) - 4);
            if (level.getBlockState(target).is(Blocks.GRASS_BLOCK)
                    && level.getBlockState(target.above()).isAir()
                    && level.getBlockState(target.above(2)).isAir()) {
                BlockPos pos = target.above();
                level.setBlockAndUpdate(pos, FRBBlocks.FLORAL_ALTAR.get().defaultBlockState());
                level.playSound(null, pos, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D,
                        pos.getY() + 0.6D, pos.getZ() + 0.5D, 20, 0.4D, 0.5D, 0.4D, 0.1D);
                return;
            }
        }
    }

    private AltarSpawnEvents() {}
}
