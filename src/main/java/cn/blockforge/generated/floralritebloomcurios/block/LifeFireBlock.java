package cn.blockforge.generated.floralritebloomcurios.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 生之焰：焚尽阶段的 Boss 专用火焰——现在改成地面附层方块（极薄薄层，无碰撞）。
 *
 * 与 r61 旧版的对比：
 * - 旧版继承 BaseFireBlock、有六向连接 state、铺在方块侧面；
 * - 新版直接继承 Block，行为类似 snow_layer：玩家踩上去受 inGame 伤害，不再蔓延、不再老化；
 * - 阶段结束由 Boss 的 sweepLifeFire() 统一清除；
 * - 渲染层走 cutout（FRBSetup 中已注册）。
 *
 * 附层模型文件：models/block/life_fire_overlay.json（贴图 textures/block/life_fire_overlay.png）。
 */
public class LifeFireBlock extends Block {

    /** 踩上去每 10 tick 2 点 inFire 伤害（原版火焰是 1 点）。 */
    private static final int DAMAGE_INTERVAL = 10;
    private static final float DAMAGE_AMOUNT = 2.0F;

    public LifeFireBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /**
     * 可被附层方块（生之焰 / 虚空侵蚀）原地替换的植物：任意花、草、紫水晶簇。
     * r65：不再在植物上方浮空生成附层，而是直接替换植物本体。
     * 虚空侵蚀（VoidRiftEntity）同样使用本判定，保持两者行为一致。
     */
    public static boolean isReplaceableFlora(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.AMETHYST_CLUSTER)
                || state.is(Blocks.LARGE_AMETHYST_BUD)
                || state.is(Blocks.MEDIUM_AMETHYST_BUD)
                || state.is(Blocks.SMALL_AMETHYST_BUD);
    }

    /** 生之焰永不下沉、不会因为下方方块被挖而消失。 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** 踩踏伤害：每 DAMAGE_INTERVAL tick 对穿过生之焰的生物造成 inFire 伤害。 */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) return;
        if (entity.isSteppingCarefully() && entity instanceof Player) return; // Shift 蹲走不踩
        if (entity.tickCount % DAMAGE_INTERVAL != 0) return;
        if (!entity.fireImmune()) {
            entity.setSecondsOnFire(2);
        }
        entity.hurt(level.damageSources().inFire(), DAMAGE_AMOUNT);
    }

    /** 永不产生掉落（用 Properties#noLootTable() 即可，不需要重写 shouldDropLoot）。 */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}