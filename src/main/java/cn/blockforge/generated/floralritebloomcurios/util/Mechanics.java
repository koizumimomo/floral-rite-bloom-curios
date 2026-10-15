package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import cn.blockforge.generated.floralritebloomcurios.entity.DeathFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.RainCloudEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.SpikeEntity;
import cn.blockforge.generated.floralritebloomcurios.network.BossFxPacket;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.network.MarkerTargetPacket;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 所有 PhaseMechanic 实现集中处：boss 在每个 tick 里遍历这些类，
 * 按 phase 匹配 + cooldown 取模 + tier 解锁阈值决定是否触发。
 *
 * 命名约定：每阶段新增机制以前缀 [B1]/[C1]/... 对应 BOSS_TODO.md 编号。
 */
public final class Mechanics {

    private Mechanics() {}

    /** Boss 阶段内 24~40 格领域内的玩家集合（用于大部分机制的目标圈）。
     *  r93：过滤创造/旁观——否则创造玩家会被机制点名（用户实测创造模式仍被追踪攻击）。 */
    public static List<ServerPlayer> playersInDomain(ServerLevel level, FriliasEntity boss, double radius) {
        return level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(radius),
                p -> !p.isCreative() && !p.isSpectator());
    }

    /** Boss 当前战斗内人数（领域 40 格内 ServerPlayer 数）。单人=1 / 多人>=2。 */
    public static int fightersInRange(ServerLevel level, FriliasEntity boss) {
        return playersInDomain(level, boss, 40.0D).size();
    }

    // ============================================================
    // B1 花之摇篮（自然阶段，cd 12s=240tick，unlockedAt 0）
    // r75：单人战只生成 1 个法阵（不再多人复数）；落点过滤紫水晶簇 + 各类花。
    // ============================================================
    public static final class FlowerCradleMechanic implements PhaseMechanic {
        @Override public int phase() { return 0; }
        @Override public int cooldown() { return FRBConfig.flowerCradle(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            var targets = playersInDomain(level, boss, 24.0D);
            if (targets.isEmpty()) return;
            // r75：单人战只生成 1 个法阵——避免多次循环刷出 N 个重叠 sigil。
            ServerPlayer p = targets.get(level.random.nextInt(targets.size()));
            BlockPos feet = p.blockPosition();
            BlockPos ground = findGround(level, feet);
            if (ground == null) return;
            FlowerCradleSigilEntity.spawn(level, ground, p);
            // r75：法阵出现瞬间领域内 actionbar 提示（满足截图 #8 文本提示需求）
            for (ServerPlayer watcher : playersInDomain(level, boss, 64.0D)) {
                watcher.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("frb.mechanic.flower_cradle.warn")
                                .withStyle(net.minecraft.ChatFormatting.GREEN),
                        true);
            }
            level.playSound(null, ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.2F);
        }

        /**
         * r75：落点过滤——必须站在"非花 / 非紫水晶簇 / 非巨型蘑菇"等可踩实体方块上方。
         * 紫水晶簇虽然能踩但用户要求排除，避免法阵生成在水晶簇顶端。
         */
        private BlockPos findGround(Level level, BlockPos feet) {
            for (int dy = 1; dy >= -3; dy--) {
                BlockPos p = feet.offset(0, dy, 0);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
                if (!state.isAir() && level.getBlockState(p.above()).isAir() && isValidSigilGround(state)) {
                    return p.above();
                }
            }
            return null;
        }

        private boolean isValidSigilGround(net.minecraft.world.level.block.state.BlockState state) {
            net.minecraft.world.level.block.Block b = state.getBlock();
            if (b == net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER) return false;
            if (b == net.minecraft.world.level.block.Blocks.BUDDING_AMETHYST) return false;
            // 排除小型花 + 高花（tag 覆盖所有普通花变种）
            if (state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)) return false;
            if (state.is(net.minecraft.tags.BlockTags.TALL_FLOWERS)) return false;
            // 排除蘑菇类
            if (b == net.minecraft.world.level.block.Blocks.RED_MUSHROOM
                    || b == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM) return false;
            // 排除蘑菇方块 / 蘑菇茎
            if (b == net.minecraft.world.level.block.Blocks.RED_MUSHROOM_BLOCK
                    || b == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK
                    || b == net.minecraft.world.level.block.Blocks.MUSHROOM_STEM) return false;
            return true;
        }
    }

    // ============================================================
    // B2 荆棘共生（自然阶段，cd 15s=300tick，unlockedAt 0，多人专属）
    // r68：点名一名玩家 4 秒预警（红圈+红标）→ 结算时以被点名者为中心、
    // 3 格内的所有玩家（含被点名者）平分 16 点魔法伤害——落单者独吞全部。
    // ============================================================
    public static final class ThornSymbiosisMechanic implements PhaseMechanic {
        @Override public int phase() { return 0; }
        @Override public int cooldown() { return FRBConfig.thornSymbiosis(); }
        @Override public boolean unlockedInMultiplayer() { return false; } // 需领域内 >= 2 名玩家
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            var targets = playersInDomain(level, boss, 24.0D);
            if (targets.isEmpty()) return;
            ServerPlayer marked = targets.get(level.random.nextInt(targets.size()));
            BlockPos feet = marked.blockPosition();
            // 预警圈广播给领域内所有参战玩家（队友能看到去抱团分担）
            for (ServerPlayer p : targets) {
                FRBNetwork.sendCircle(p, feet, 3.0D, 80, 0);
            }
            FRBNetwork.sendMarker(marked, 80, 0xff5050);
            level.playSound(null, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                    SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.HOSTILE, 1.2F, 0.7F);
            boss.scheduleDelayedTask(80, () -> {
                ServerLevel sl = marked.serverLevel();
                if (sl == null || !marked.isAlive()) return;
                List<ServerPlayer> sharing = sl.getEntitiesOfClass(ServerPlayer.class,
                        marked.getBoundingBox().inflate(3.0D), LivingEntity::isAlive);
                if (sharing.isEmpty()) return;
                // r72：荆棘共生伤害 = base × 全局倍率 × 自然阶段倍率（被分担后才是 each）
                float each = (FRBConfig.dmg(16.0F) * FRBConfig.dmgThornSymbiosis()) / sharing.size();
                for (ServerPlayer p : sharing) {
                    p.hurt(sl.damageSources().indirectMagic(boss, boss), each);
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            p.getX(), p.getY() + 1.0D, p.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.1D);
                }
                sl.playSound(null, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                        SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
            });
        }
    }

    // ============================================================
    // C1 灼热之种（焚尽阶段，cd 12s=240tick，unlockedAt 0）
    // ============================================================
    public static final class BurningSeedMechanic implements PhaseMechanic {
        @Override public int phase() { return 1; }
        @Override public int cooldown() { return FRBConfig.burningSeed(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            var targets = playersInDomain(level, boss, 32.0D);
            if (targets.isEmpty()) return;
            ServerPlayer p = targets.get(level.random.nextInt(targets.size()));
            BlockPos feet = p.blockPosition();
            // 4 秒倒计时红圈 + 头顶标记
            cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork.sendCircle(
                    p, feet, 1.5D, 80, 0);
            cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork.sendMarker(p, 80, 0xff5050);
            // 4 秒后结算：交给 boss 自管的 pendingTask 队列（避免依赖 MinecraftServer.tell(Runnable, int)）
            // r70：引信时长读 config
            boss.scheduleDelayedTask(FRBConfig.burningSeedFuse(), () -> {
                ServerLevel sl = p.serverLevel();
                if (sl == null) return;
                // r71（bug4 续）：sl.explode() 会遍历范围内实体+所有方块 ray-trace + 生成大量粒子，
                // 在原版爆炸 tick 上引发数 tick 卡顿。改用原版爆炸粒子 + 音效手动广播 +
                // 仅对 1.5 格内玩家结算伤害（用户要求"应该只有爆炸，不生成生之焰"）。
                double ex = feet.getX() + 0.5D, ey = feet.getY() + 0.5D, ez = feet.getZ() + 0.5D;
                // 原版爆炸核心粒子（EXPLOSION_EMITTER + EXPLOSION 减半）
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        ex, ey, ez, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        ex, ey, ez, 4, 0.6D, 0.4D, 0.6D, 0.02D);
                // 烟火余韵 + 火焰喷射粒子（粒子减半）
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        ex, ey + 0.4D, ez, 8, 0.5D, 0.3D, 0.5D, 0.04D);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        ex, ey + 0.2D, ez, 6, 0.5D, 0.3D, 0.5D, 0.03D);
                // 原版爆炸音效
                sl.playSound(null, ex, ey, ez,
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 1.0F);
                // 伤害结算（仅 1.5 格内玩家，不破坏地形、不放生之焰）
                for (ServerPlayer victim : sl.getEntitiesOfClass(ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(feet).inflate(1.5D))) {
                    // r72：灼热之种爆炸伤害 = base × 全局倍率 × 焚尽阶段倍率
                    victim.hurt(sl.damageSources().explosion(boss, boss), FRBConfig.dmg(8.0F) * FRBConfig.dmgBurningSeed());
                }
            });
        }
    }

    // ============================================================
    // C2 烈焰击退（焚尽阶段，cd 8s=160tick，unlockedAt 0）
    // ============================================================
    public static final class FlameKnockbackMechanic implements PhaseMechanic {
        @Override public int phase() { return 1; }
        @Override public int cooldown() { return FRBConfig.flameKnockback(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // 蓄力 1.5 秒：30 tick 蓄力 → 结算
            boss.startFlameCharge(level, 30);
        }
    }

    // ============================================================
    // D1 霜凝（霜华阶段，cd 20s=400tick，unlockedAt 0）
    // ============================================================
    public static final class FrostGazeMechanic implements PhaseMechanic {
        @Override public int phase() { return 2; }
        @Override public int cooldown() { return FRBConfig.frostGaze(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            boss.startFrostGaze(level, 30); // 1.5 秒蓄力
        }
    }

    // ============================================================
    // D2 随机冰刺法阵（霜华阶段，cd 10s=200tick，unlockedAt 1）
    // 70% 血量之后 boss 周围 16~24 格随机落 6~8 颗冰刺，2 秒后同时爆发。
    // r64：用户反馈冰刺应"24 格内随机寻找落点，且必须是在方块上面"。
    // 用 MOTION_BLOCKING 高度图直接取地表列高 + 玩家脚下附近的方块，与 boss Y 完全无关。
    // ============================================================
    public static final class FrostSigilSurroundMechanic implements PhaseMechanic {
        @Override public int phase() { return 2; }
        @Override public int cooldown() { return FRBConfig.frostSigil(); }
        @Override public int unlockedAt() { return 1; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            RandomSource r = level.random;
            int count = 6 + r.nextInt(3); // 6..8 颗
            for (int i = 0; i < count; i++) {
                double a = r.nextDouble() * Math.PI * 2.0D;
                double d = r.nextDouble() * 24.0D; // r98：0..24 格整圆（避免贴脸免伤）
                double x = boss.getX() + Math.cos(a) * d;
                double z = boss.getZ() + Math.sin(a) * d;
                int cx = net.minecraft.core.SectionPos.blockToSectionCoord(
                        net.minecraft.util.Mth.floor(x));
                int cz = net.minecraft.core.SectionPos.blockToSectionCoord(
                        net.minecraft.util.Mth.floor(z));
                if (!level.hasChunk(cx, cz)) continue;
                // 取地表（与 boss Y 无关），确保选在方块上方
                net.minecraft.core.BlockPos baseXZ = new net.minecraft.core.BlockPos(
                        net.minecraft.util.Mth.floor(x), 0, net.minecraft.util.Mth.floor(z));
                net.minecraft.core.BlockPos groundTop = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        baseXZ);
                // 寻找最上方的非空气方块表面（高度图下方偶尔有树冠/积雪，按空气查找）
                net.minecraft.core.BlockPos surface = groundTop;
                for (int dy = 0; dy <= 6; dy++) {
                    net.minecraft.core.BlockPos up = groundTop.above(dy);
                    if (level.getBlockState(up).isAir()) {
                        surface = up;
                        break;
                    }
                }
                SpikeEntity.spawn(level, surface.getX() + 0.5D, surface.getY(),
                        surface.getZ() + 0.5D, SpikeEntity.KIND_ICE, 40, boss);
            }
        }
    }

    // ============================================================
    // E1 雨之华 debuff（云雨阶段，受击触发，cd 60tick）
    // ============================================================
    public static final class RainBloomTickMechanic implements PhaseMechanic {
        @Override public int phase() { return 3; }
        @Override public int cooldown() { return FRBConfig.rainBloom(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // 每 3 秒：给领域内全员叠 1 层雨之华（最高 12）
            for (ServerPlayer p : playersInDomain(level, boss, 32.0D)) {
                var inst = p.getEffect(FRBEffects.RAIN_BLOOM.get());
                int next = inst == null ? 0 : Math.min(inst.getAmplifier() + 1, 11);
                p.addEffect(new MobEffectInstance(FRBEffects.RAIN_BLOOM.get(), 120, next,
                        false, true, true));
            }
        }
    }

    // ============================================================
    // E2 落雷（云雨阶段，cd 10s=200tick，unlockedAt 0）
    // ============================================================
    public static final class LightningMechanic implements PhaseMechanic {
        @Override public int phase() { return 3; }
        @Override public int cooldown() { return FRBConfig.lightning(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            var targets = playersInDomain(level, boss, 40.0D);
            if (targets.isEmpty()) return;
            int fighters = fightersInRange(level, boss);
            int n = Math.min(fighters == 1 ? 1 : (fighters == 2 ? 2 : 3), targets.size());
            // 随机点名 n 人
            java.util.Collections.shuffle(targets, new java.util.Random());
            for (int i = 0; i < n; i++) {
                ServerPlayer p = targets.get(i);
                FRBNetwork.sendMarker(p, 50, 0xffff66);
                boss.scheduleLightningStrike(level, p, 50);
            }
        }
    }

    // ============================================================
    // E3 积雨云（云雨阶段，cd 24s=480tick，unlockedAt 1）
    // ============================================================
    public static final class RainCloudMechanic implements PhaseMechanic {
        @Override public int phase() { return 3; }
        @Override public int cooldown() { return FRBConfig.rainCloud(); }
        @Override public int unlockedAt() { return 1; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // r93：积雨云落点只取生存/冒险玩家（getNearestPlayer 可能点到创造玩家）
            Player nearest = null;
            double nd = Double.MAX_VALUE;
            for (ServerPlayer sp : playersInDomain(level, boss, 24.0D)) {
                double d = sp.distanceToSqr(boss);
                if (d < nd) { nd = d; nearest = sp; }
            }
            if (nearest == null) return;
            // r90：积雨云升空 3 格 + 无敌约 1.25 秒
            boss.beginSkillCast(level, 25);
            RainCloudEntity.spawn(level, nearest.getX(), nearest.getY() + 6.0D, nearest.getZ());
        }
    }

    // ============================================================
    // F2 风刃（天空阶段，cd 6s=120tick，unlockedAt 1）
    // ============================================================
    public static final class WindBladeMechanic implements PhaseMechanic {
        @Override public int phase() { return 4; }
        @Override public int cooldown() { return FRBConfig.windBlade(); }
        @Override public int unlockedAt() { return 1; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // 随机方向发两道风刃（非追踪直线弹）
            RandomSource r = level.random;
            double baseAngle = r.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < 2; i++) {
                double a = baseAngle + i * Math.PI; // 相反方向
                Vec3 dir = new Vec3(Math.cos(a), -0.15D, Math.sin(a)).normalize();
                cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(
                        level, boss.position().add(0, 0.5D, 0), dir, boss);
            }
            level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 1.6F);
        }
    }

    // ============================================================
    // G1 虚空裂隙（虚空阶段，cd 默认 100tick = 5s；r75 加密度后 cd 翻倍）
    // r75：每次触发同时生成 2 条裂隙，并广播文本提示。
    // ============================================================
    public static final class VoidRiftReducedMechanic implements PhaseMechanic {
        @Override public int phase() { return 5; }
        @Override public int cooldown() { return FRBConfig.voidRift(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // r75：双裂隙（围绕最近玩家对称落 2 条），并显示"虚空撕裂"的红色提示
            boss.spawnSingleVoidRift(level);
            boss.spawnSingleVoidRift(level);
            for (ServerPlayer p : playersInDomain(level, boss, 48.0D)) {
                p.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("frb.mechanic.void.rift")
                                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                        true);
            }
        }
    }

    // ============================================================
    // r90 虚空潮汐（虚空阶段，cd 240 tick = 12s）：
    // 随机先放环形再放圆形 aoe，或先圆形后环形。预警 60 tick + 伤害 + 粒子。
    // ============================================================
    public static final class VoidTideMechanic implements PhaseMechanic {
        @Override public int phase() { return 5; }
        @Override public int cooldown() { return FRBConfig.voidTide(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            boss.castVoidTide(level);
            for (ServerPlayer p : playersInDomain(level, boss, 48.0D)) {
                p.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("frb.mechanic.void.tide")
                                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                        true);
            }
        }
    }

    // ============================================================
    // H1 死亡之花复活（枯萎阶段，每 80 tick 检查）
    // ============================================================
    public static final class DeathFlowerResurreMechanic implements PhaseMechanic {
        @Override public int phase() { return 6; }
        @Override public int cooldown() { return FRBConfig.deathFlower(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            // boss 维护最近一次死亡的花：超过 60 tick（3 秒）未被消灭则复活
            boss.maybeReviveDeathFlowers(level);
        }
    }

    // ============================================================
    // r90 时间潮汐（时间阶段，cd 240t = 12s）：
    // 同虚空潮汐机制，贴图换为 time_aoe_ring/circle。
    // ============================================================
    public static final class TimeTideMechanic implements PhaseMechanic {
        @Override public int phase() { return 7; }
        @Override public int cooldown() { return FRBConfig.timeTide(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            boss.castTimeTide(level);
        }
    }

    // ============================================================
    // r96 钢铁月环（霜华阶段，cd 140t = 7s）：
    // 圆形/环形 AOE，3 秒读条，贴图 frost_moon_ring/circle。
    // ============================================================
    public static final class FrostMoonRingMechanic implements PhaseMechanic {
        @Override public int phase() { return 2; }
        @Override public int cooldown() { return FRBConfig.frostMoonRing(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            boss.castFrostMoonRing(level);
        }
    }

    // ============================================================
    // r96 光辉炎柱（焚尽阶段，cd 240t = 12s）：
    // 8 个随机落点半径 3 圆形 AOE，2.5s 预警后 12 花灵 + 6 爆炸伤害。
    // ============================================================
    public static final class FlamePillarMechanic implements PhaseMechanic {
        @Override public int phase() { return 1; }
        @Override public int cooldown() { return FRBConfig.flamePillar(); }
        @Override public int unlockedAt() { return 0; }
        @Override
        public void tick(FriliasEntity boss, ServerLevel level, int timer) {
            boss.castFlamePillar(level);
        }
    }

    // ============================================================
    // 工厂：FriliasEntity 在构造时一次性注册所有机制。
    // ============================================================
    public static List<PhaseMechanic> all() {
        return java.util.List.of(
                // 自然
                new FlowerCradleMechanic(),
                new ThornSymbiosisMechanic(),
                // 焚尽
                new BurningSeedMechanic(),
                new FlameKnockbackMechanic(),
                new FlamePillarMechanic(),
                // 霜华
                new FrostGazeMechanic(),
                new FrostSigilSurroundMechanic(),
                new FrostMoonRingMechanic(),
                // 云雨
                new RainBloomTickMechanic(),
                new LightningMechanic(),
                new RainCloudMechanic(),
                // 天空
                new WindBladeMechanic(),
                // 虚空
                new VoidRiftReducedMechanic(),
                new VoidTideMechanic(),
                // 枯萎
                new DeathFlowerResurreMechanic(),
                // 时间（额外阶段）
                new TimeTideMechanic()
        );
    }
}