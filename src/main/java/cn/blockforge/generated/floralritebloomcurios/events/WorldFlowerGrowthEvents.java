package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 骨粉催生世界之花：对泥土类方块用骨粉时，有小概率在附近长出一株 <b>age = 0</b> 的世界之花。
 * <p>
 * 这是世界之花的第二条获取路径。第一条是世界生成（见
 * {@code data/.../worldgen/placed_feature/world_flower.json}，只在花朵类群系里按 1/16 区块出现）。
 * 两条路径都不需要"先有一株世界之花"，补上了原本"种子只能从世界之花/ Boss 掉落拿"的死循环。
 * <p>
 * 与 {@link AltarSpawnEvents} 的花仪台座互不干扰：两者监听同一个事件但各自独立掷骰，
 * 同一次骨粉理论上可以两者都触发，也可能都不触发。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class WorldFlowerGrowthEvents {

    /** 每次对地面用骨粉时，尝试长出世界之花的概率。 */
    private static final float CHANCE = 0.05F;
    /** 相对被催熟方块的水平搜索半径（格）。 */
    private static final int RADIUS = 4;

    @SubscribeEvent
    public static void onBonemealGround(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // BlockTags.DIRT 覆盖草方块、泥土、粗泥、灰化土、菌丝、扎根泥土、苔藓块等
        if (!event.getBlock().is(BlockTags.DIRT)) return;

        RandomSource random = level.getRandom();
        if (random.nextFloat() >= CHANCE) return;

        BlockPos origin = event.getPos();
        for (int tries = 0; tries < 10; tries++) {
            BlockPos target = origin.offset(
                    random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    random.nextInt(5) - 2,
                    random.nextInt(RADIUS * 2 + 1) - RADIUS);
            // 脚下得是泥土类；目标格与上方一格都得是空气，避免卡进方块里
            if (!level.getBlockState(target.below()).is(BlockTags.DIRT)) continue;
            if (!level.getBlockState(target).isAir()) continue;
            if (!level.getBlockState(target.above()).isAir()) continue;

            level.setBlockAndUpdate(target, FRBBlocks.WORLD_FLOWER.get().defaultBlockState());
            level.playSound(null, target, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5D,
                    target.getY() + 0.5D, target.getZ() + 0.5D, 12, 0.3D, 0.4D, 0.3D, 0.05D);
            return;
        }
    }

    private WorldFlowerGrowthEvents() {}
}
