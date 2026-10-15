package cn.blockforge.generated.floralritebloomcurios.world;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Set;

/**
 * 永恒花园是 Boss 挑战场地：维度内不可破坏方块（挖掘取消、爆炸只伤实体不毁方块），
 * 野生生物禁止生成（自然刷怪 / 刷怪笼等），但玩家带来的召唤物 / 刷怪蛋等主动生成放行
 * ——若一律取消，部分召唤类 mod 的"每 tick 补齐召唤物"逻辑会无限重试导致服务线程死循环。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GardenProtection {

    /** 视为"野外生成"的原因：这些被取消；其余（召唤 / 指令 / 蛋 / 繁殖等玩家行为）放行。 */
    private static final Set<MobSpawnType> WILD_REASONS = EnumSet.of(
            MobSpawnType.NATURAL, MobSpawnType.SPAWNER, MobSpawnType.PATROL,
            MobSpawnType.REINFORCEMENT, MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.STRUCTURE, MobSpawnType.TRIGGERED);

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level
                && level.dimension() == EternalGarden.DIMENSION_KEY) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof Level level
                && level.dimension() == EternalGarden.DIMENSION_KEY
                && !(event.getEntity() instanceof FriliasEntity)
                && WILD_REASONS.contains(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension() == EternalGarden.DIMENSION_KEY) {
            // 清空受影响方块列表：爆炸保留对实体（含 Boss）的伤害，但不破坏地形
            event.getExplosion().getToBlow().clear();
        }
    }

    private GardenProtection() {}
}
