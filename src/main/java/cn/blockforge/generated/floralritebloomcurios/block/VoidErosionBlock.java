package cn.blockforge.generated.floralritebloomcurios.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 虚空侵蚀地块附层：虚空裂隙首次显形后在地面铺一层。
 *
 * - 每 20 tick（1 秒）造成 2 点虚空伤害（damageSource magic）；
 * - 同时附加一层【虚空侵蚀】效果（最高 20 层，每层 -1% 最大生命 / 攻击 / 移速）；
 * - 离开侵蚀地块后每秒层数 -1（外部 void_erosion_effect 处理）；
 * - 仅由 Boss 虚空阶段生成；玩家手动破坏不掉任何东西、无掉落。
 *
 * 模型：models/block/void_erosion_overlay.json；贴图 textures/block/void_erosion_overlay.png。
 */
public class VoidErosionBlock extends Block {

    private static final int DAMAGE_INTERVAL = 20;
    private static final float DAMAGE_AMOUNT = 2.0F;

    public VoidErosionBlock(BlockBehaviour.Properties props) {
        super(props);
    }

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

    /** 踩踏伤害 + 叠层效果（无碰撞附层用 entityInside，原版火焰同理）。 */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (entity.tickCount % DAMAGE_INTERVAL != 0) return;
        // 2 点虚空伤害（magic 伤害类型，被抗性提升 / 保护效果可减免）
        entity.hurt(level.damageSources().magic(), DAMAGE_AMOUNT);
        // 叠一层虚空侵蚀
        var instance = player.getEffect(cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects.VOID_EROSION.get());
        int nextAmp = (instance == null ? -1 : instance.getAmplifier()) + 1;
        if (nextAmp >= 20) nextAmp = 19;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects.VOID_EROSION.get(),
                600, nextAmp, false, false, true));
        // 标记所在刻，玩家离开时由 effect 类每 tick -1 层数（外部实现）
        if (level instanceof ServerLevel) {
            player.getPersistentData().putLong("frb_void_erosion_last",
                    level.getGameTime());
        }
    }

    /** 永不产生掉落（用 Properties#noLootTable() 即可，不需要重写 shouldDropLoot）。 */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}