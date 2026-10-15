package cn.blockforge.generated.floralritebloomcurios.block;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds;
import cn.blockforge.generated.floralritebloomcurios.util.FRBAdvancements;
import cn.blockforge.generated.floralritebloomcurios.world.EternalGarden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 永恒之门：世界之花开门演出 40 tick 后在其正上方生成的传送门。
 * 右键传送（打 boss 不会因走位碰撞误触）：
 * 主世界的门 → 进入永恒花园；花园里的门 → 回到进入前的位置。
 * 展开瞬间对周围玩家发镜头扭曲（冲击波着色器）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public class EternalPortalBlock extends Block {

    /** 开门演出 pending：世界之花位置 → 完成时刻（gameTime）。 */
    private static final Map<BlockPos, Long> GATES = new HashMap<>();
    private static final int GATE_TICKS = 40;
    /** 选框 1.8125 格高（模型面片 1.8 格），保证整扇门都点得到。 */
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 29.0D, 13.0D);

    public EternalPortalBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ---- 世界之花 → 开门演出 ----

    /**
     * 主世界成熟世界之花右键：先验证「万花齐放」门槛，通过则启动开门演出。
     * 演出结束在世界之花正上方生成永恒之门。
     */
    public static void tryOpenGate(ServerLevel level, BlockPos flowerPos, ServerPlayer player) {
        if (!player.isCreative() && !FRBAdvancements.hasDone(player, "charm_collector")) {
            player.displayClientMessage(
                    Component.translatable("frb.garden.locked").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        if (GATES.containsKey(flowerPos)) return;
        GATES.put(flowerPos.immutable(), level.getGameTime() + GATE_TICKS);
        level.playSound(null, flowerPos, FRBSounds.GARDEN_GATE.get(), SoundSource.MASTER, 1.0F, 0.7F);
    }

    /** 演出 tick：双臂粒子从世界之花下方螺旋升起、向花顶汇聚。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide
                || !(event.level instanceof ServerLevel level)) return;
        if (GATES.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = GATES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockPos flower = entry.getKey();
            if (!level.isLoaded(flower)) continue; // 区块未加载时不推进，防同步生成
            long remain = entry.getValue() - now;
            if (remain > 0) {
                double p = 1.0D - remain / (double) GATE_TICKS; // 0→1
                double baseY = flower.getY() - 0.2D;
                double topY = flower.getY() + 2.2D;
                for (int arm = 0; arm < 2; arm++) {
                    double angle = p * Math.PI * 4.0D + arm * Math.PI;
                    double radius = 1.6D * (1.0D - p * 0.75D);
                    double px = flower.getX() + 0.5D + Math.cos(angle) * radius;
                    double pz = flower.getZ() + 0.5D + Math.sin(angle) * radius;
                    double py = baseY + (topY - baseY) * p;
                    // 星系主题：星点 + 星云微粒沿旋臂升腾
                    level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                            .INTRO_STAR.get(), px, py, pz, 2, 0.02D, 0.02D, 0.02D, 0.0D);
                    level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                            .INTRO_WISP.get(), px, py, pz, 1, 0.05D, 0.05D, 0.05D, 0.02D);
                }
                level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                        .INTRO_STAR.get(), flower.getX() + 0.5D, topY + 0.2D,
                        flower.getZ() + 0.5D, 1, 0.1D, 0.05D, 0.1D, 0.01D);
                continue;
            }
            it.remove();
            // 汇聚完成：正上方生成永恒之门（星环爆发 + 星点洒落）
            BlockPos portalPos = flower.above();
            level.setBlockAndUpdate(portalPos, cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks
                    .ETERNAL_PORTAL.get().defaultBlockState());
            level.playSound(null, portalPos, FRBSounds.GARDEN_GATE.get(), SoundSource.MASTER, 1.4F, 1.3F);
            level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                    .ECLIPSE_RING.get(), portalPos.getX() + 0.5D, portalPos.getY() + 0.9D,
                    portalPos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                    .INTRO_STAR.get(), portalPos.getX() + 0.5D, portalPos.getY() + 0.9D,
                    portalPos.getZ() + 0.5D, 40, 0.8D, 0.9D, 0.8D, 0.02D);
            level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                    .INTRO_WISP.get(), portalPos.getX() + 0.5D, portalPos.getY() + 0.9D,
                    portalPos.getZ() + 0.5D, 12, 0.4D, 0.6D, 0.4D, 0.01D);
            // 镜头扭曲：门的展开冲击波
            FRBNetwork.sendBossFx(level, portalPos, cn.blockforge.generated.floralritebloomcurios.network.BossFxPacket.FX_SHOCKWAVE,
                    0.5F, 30);
        }
    }

    // ---- 右键传送 ----

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand,
                                 net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() == EternalGarden.DIMENSION_KEY) {
            EternalGarden.leave(sp);
        } else {
            EternalGarden.enter(sp);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- 环境粒子 ----

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 星系主题环境粒子：星点缓慢上浮 + 紫 portal 星云
        double px = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.7D;
        double py = pos.getY() + random.nextDouble() * 1.7D;
        double pz = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.7D;
        level.addParticle(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles
                .INTRO_STAR.get(), px, py, pz, 0.0D, 0.01D, 0.0D);
        if (random.nextFloat() < 0.4F) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, px, py, pz,
                    (random.nextDouble() - 0.5D) * 0.2D, 0.05D, (random.nextDouble() - 0.5D) * 0.2D);
        }
    }
}
