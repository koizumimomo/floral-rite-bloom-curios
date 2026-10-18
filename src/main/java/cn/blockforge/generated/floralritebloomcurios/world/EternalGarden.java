package cn.blockforge.generated.floralritebloomcurios.world;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.block.WorldFlowerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 永恒花园（Boss 挑战维度）：主世界完全盛开的世界之花右键进入，
 * 花园内的世界之花右键召唤芙莉艾丝、潜行右键返回来时的位置。
 * 维度本体由数据包注册：data/floral_rite_bloom_curios/dimension[_type]/eternal_garden.json。
 */
public final class EternalGarden {
    public static final ResourceKey<Level> DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(FloralRiteBloom.MOD_ID, "eternal_garden"));

    /** 花园落点固定在 (0, 地表, 0)，世界之花摆在入口东侧（远离大门结构），召唤过场围绕它进行。 */
    private static final String TAG_RETURN = "FRBGardenReturn";
    /** r76：世界之花与大门结构的最小间距（结构南侧 +4 安全余量），结构更大时自动外推。 */
    private static final int FLOWER_MIN_OFFSET = 12;
    /** r68：回程大门结构（结构方块保存于 <world>/generated/frb/structures/garden_gate.nbt）。 */
    private static final ResourceLocation GATE_STRUCTURE = new ResourceLocation("frb", "garden_gate");
    private static final String GATE_DATA_NAME = "frb_garden_gate";

    /** 主世界世界之花/永恒之门右键：记录回程坐标，传送到花园落点，并备好世界之花与回程之门。 */
    public static void enter(ServerPlayer player) {
        ServerLevel garden = player.server.getLevel(DIMENSION_KEY);
        if (garden == null) {
            // 维度数据包未加载时给出明确提示，便于诊断
            player.displayClientMessage(Component.translatable("frb.garden.missing")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        CompoundTag ret = new CompoundTag();
        ret.putString("Dim", player.level().dimension().location().toString());
        ret.putDouble("X", player.getX());
        ret.putDouble("Y", player.getY());
        ret.putDouble("Z", player.getZ());
        ret.putFloat("Yaw", player.getYRot());
        ret.putFloat("Pitch", player.getXRot());
        player.getPersistentData().put(TAG_RETURN, ret);

        BlockPos landing = surface(garden, 0, 0);
        player.teleportTo(garden, landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        // 命令式跨维度传送不会重发能力/药效，手动补发（否则客户端会丢掉夜视、水下呼吸、飞行）
        cn.blockforge.generated.floralritebloomcurios.util.FRBState.resendClientState(player);
        int gateHalfX = ensureGate(garden, landing);
        ensureFlower(garden, landing, gateHalfX);
        garden.playSound(null, landing,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.GARDEN_GATE.get(),
                SoundSource.PLAYERS, 0.6F, 1.4F);
        garden.sendParticles(ParticleTypes.END_ROD, landing.getX() + 0.5D, landing.getY() + 1.0D,
                landing.getZ() + 0.5D, 40, 1.2D, 1.0D, 1.2D, 0.05D);
    }

    /**
     * r68：回程大门——首次进入时在落点北侧摆放 garden_gate 结构（内部含永恒之门）。
     * <ul>
     *   <li>只生成在永恒花园维度（本方法仅被 enter() 调用）；</li>
     *   <li>只生成一次：DimensionDataStorage 持久标记，已有则不生成；</li>
     *   <li>替换旧"落点北侧悬空单门"逻辑；摆放前清掉旧档残留的两处历史单门；</li>
     *   <li>结构文件缺失（世界没有 generated/frb/structures/garden_gate.nbt）时回退旧行为，
     *       不写标记——补上文件后下次进入仍会摆放结构。</li>
     * </ul>
     *
     * @return 结构 X 方向半宽（世界之花用它避让；回退单门时为 0）
     */
    private static int ensureGate(ServerLevel garden, BlockPos landing) {
        GateData data = garden.getDataStorage().computeIfAbsent(GateData::load, GateData::new, GATE_DATA_NAME);
        if (data.placed) {
            return data.gateHalfX;
        }
        var templateOpt = garden.getStructureManager().get(GATE_STRUCTURE);
        if (templateOpt.isEmpty()) {
            legacyPortal(garden, landing);
            return 0;
        }
        net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = templateOpt.get();
        net.minecraft.core.Vec3i size = template.getSize();
        // 中心对齐旧单门的位置（落点北侧 2 格），底部落在地表；r70：整体向下一格（y-1）修正悬浮高度
        BlockPos ground = surface(garden, landing.getX(), landing.getZ() - 2);
        BlockPos origin = new BlockPos(
                landing.getX() - size.getX() / 2,
                ground.getY() - 1,
                landing.getZ() - 2 - size.getZ() / 2);
        // 预加载结构覆盖到的所有区块到 FULL，防止跨区块放置触发同步生成卡服
        int minCx = origin.getX() >> 4;
        int maxCx = (origin.getX() + size.getX()) >> 4;
        int minCz = origin.getZ() >> 4;
        int maxCz = (origin.getZ() + size.getZ()) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                garden.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
        // 清掉旧档的历史单门（贴地 / 悬空两处），避免与结构内的门重复
        removeLegacyPortals(garden, ground);
        template.placeInWorld(garden, origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                garden.random, 2);
        data.placed = true;
        data.gateHalfX = size.getX() / 2;
        data.setDirty();
        return data.gateHalfX;
    }

    /** 旧版回退：落点北侧 2 格保证有一扇永恒之门（悬在地面 2 格高处）。 */
    private static void legacyPortal(ServerLevel garden, BlockPos landing) {
        BlockPos ground = surface(garden, landing.getX(), landing.getZ() - 2);
        BlockPos portalPos = ground.above(2);
        var portalBlock = cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks
                .ETERNAL_PORTAL.get();
        if (garden.getBlockState(portalPos).is(portalBlock)) {
            return;
        }
        if (garden.getBlockState(ground).getBlock()
                instanceof cn.blockforge.generated.floralritebloomcurios.block.EternalPortalBlock) {
            garden.removeBlock(ground, false);
        }
        garden.setBlockAndUpdate(portalPos, portalBlock.defaultBlockState());
    }

    /** 清掉旧档在落点北侧的悬空单门与贴地单门（r68 结构替换前的遗留）。 */
    private static void removeLegacyPortals(ServerLevel garden, BlockPos ground) {
        var portalBlock = cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks
                .ETERNAL_PORTAL.get();
        BlockPos floating = ground.above(2);
        if (garden.getBlockState(floating).is(portalBlock)) {
            garden.removeBlock(floating, false);
        }
        if (garden.getBlockState(ground).getBlock()
                instanceof cn.blockforge.generated.floralritebloomcurios.block.EternalPortalBlock) {
            garden.removeBlock(ground, false);
        }
    }

    /** r68：花园维度的一次性大门标记（挂在维度存档 data/frb_garden_gate.dat）。 */
    public static class GateData extends net.minecraft.world.level.saveddata.SavedData {
        public boolean placed;
        public int gateHalfX;

        public static GateData load(CompoundTag tag) {
            GateData data = new GateData();
            data.placed = tag.getBoolean("Placed");
            data.gateHalfX = tag.getInt("GateHalfX");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Placed", this.placed);
            tag.putInt("GateHalfX", this.gateHalfX);
            return tag;
        }
    }

    /** 潜行右键花园内的世界之花：回到进入前的位置；没有记录则回主世界共享出生点。 */
    public static void leave(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag ret = root.getCompound(TAG_RETURN);
        ServerLevel dest;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        if (ret.isEmpty()) {
            dest = player.server.overworld();
            BlockPos spawn = dest.getSharedSpawnPos();
            x = spawn.getX() + 0.5D;
            y = surface(dest, spawn.getX(), spawn.getZ()).getY();
            z = spawn.getZ() + 0.5D;
            yaw = player.getYRot();
            pitch = player.getXRot();
        } else {
            ResourceLocation id = ResourceLocation.tryParse(ret.getString("Dim"));
            ResourceKey<Level> key = id == null ? Level.OVERWORLD
                    : ResourceKey.create(Registries.DIMENSION, id);
            dest = player.server.getLevel(key);
            if (dest == null) dest = player.server.overworld();
            x = ret.getDouble("X");
            y = ret.getDouble("Y");
            z = ret.getDouble("Z");
            yaw = ret.getFloat("Yaw");
            pitch = ret.getFloat("Pitch");
        }
        root.remove(TAG_RETURN);
        player.teleportTo(dest, x, y, z, yaw, pitch);
        cn.blockforge.generated.floralritebloomcurios.util.FRBState.resendClientState(player);
        player.fallDistance = 0.0F;
    }

    /**
     * 花园内的世界之花右键：若花园中没有存活的芙莉艾丝，
     * 播放苏醒过场（标题 + 音效 + 粒子 + 全维度广播）并召唤她；花保留，可反复挑战。
     *
     * @return false 表示花园中已有她在绽放
     */
    public static boolean summon(ServerPlayer summoner, ServerLevel garden, BlockPos flowerPos) {
        List<FriliasEntity> existing = garden.getEntitiesOfClass(FriliasEntity.class,
                new AABB(flowerPos).inflate(256.0D, 160.0D, 256.0D), FriliasEntity::isAlive);
        if (!existing.isEmpty()) return false;

        FriliasEntity boss = new FriliasEntity(FRBEntities.FRILIAS.get(), garden);
        boss.setPos(flowerPos.getX() + 0.5D, flowerPos.getY(), flowerPos.getZ() + 0.5D);
        garden.addFreshEntity(boss);
        garden.playSound(null, flowerPos,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_SUMMON.get(),
                SoundSource.MASTER, 4.0F, 0.8F);
        garden.sendParticles(ParticleTypes.END_ROD, flowerPos.getX() + 0.5D, flowerPos.getY() + 1.0D,
                flowerPos.getZ() + 0.5D, 80, 1.5D, 2.0D, 1.5D, 0.1D);
        for (ServerPlayer p : garden.players()) {
            FRBNetwork.sendPhaseTitle(p, -1, 0);
            p.sendSystemMessage(Component.translatable("frb.boss.awaken")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /**
     * 落点正南固定位置保证有一朵完全盛开的世界之花：没有就铺泥土补种。
     * r76：用户截图反馈——花原本在传送门东侧（玩家进入后右前方），改到正南（门正前方），
     *      与"刚进入维度时正前方"对齐；旧档在东侧近处已有的世界之花直接沿用，不重复补种。
     */
    private static void ensureFlower(ServerLevel garden, BlockPos landing, int gateHalfX) {
        int offset = Math.max(FLOWER_MIN_OFFSET, gateHalfX + 4);
        // 旧档兼容：东侧近处（含旧版 4 格位）已有世界之花就沿用
        for (int dx = 3; dx <= offset; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (garden.getBlockState(landing.offset(dx, 0, dz)).getBlock() instanceof WorldFlowerBlock) {
                    return;
                }
            }
        }
        // r76：花改放正南（+Z = 玩家进入维度后正前方），offset 12 保证花不被大门结构覆盖，
        //      X 不再偏移，与落点同列。
        BlockPos flowerPos = surface(garden, landing.getX(), landing.getZ() + offset);
        if (garden.getBlockState(flowerPos).getBlock() instanceof WorldFlowerBlock) return;
        BlockPos below = flowerPos.below();
        if (!garden.getBlockState(below).is(BlockTags.DIRT)) {
            garden.setBlockAndUpdate(below, Blocks.DIRT.defaultBlockState());
        }
        garden.setBlockAndUpdate(flowerPos, FRBBlocks.WORLD_FLOWER.get().defaultBlockState()
                .setValue(WorldFlowerBlock.AGE, WorldFlowerBlock.MAX_AGE));
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        // 落点必须等区块真正生成到 FULL，否则异步加载时 heightmap 返回维度最低高度 → 传到基岩下方
        level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight()) {
            y = level.getMinBuildHeight() + 8;
        }
        return new BlockPos(x, y, z);
    }

    private EternalGarden() {}
}
