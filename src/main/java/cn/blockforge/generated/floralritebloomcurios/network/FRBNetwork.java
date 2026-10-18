package cn.blockforge.generated.floralritebloomcurios.network;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.abilities.AstralBlink;
import cn.blockforge.generated.floralritebloomcurios.abilities.FrostChimeRecall;
import cn.blockforge.generated.floralritebloomcurios.abilities.TerminusWreath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Consumer;

public final class FRBNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FloralRiteBloom.MOD_ID, "main"),
            () -> "1", "1"::equals, "1"::equals);

    /** 客户端在启动时替换这些处理器，避免通用代码触碰客户端类。 */
    public static Consumer<PhaseTitlePacket> ON_PHASE = p -> {};
    public static Consumer<FormationPacket> ON_FORMATION = p -> {};
    public static Consumer<DomainPacket> ON_DOMAIN = p -> {};
    public static Consumer<BossFxPacket> ON_BOSS_FX = p -> {};
    public static Consumer<MarkerTargetPacket> ON_MARKER = p -> {};
    public static Consumer<CircleTargetPacket> ON_CIRCLE = p -> {};
    public static Consumer<WindEyePacket> ON_WIND_EYE = p -> {};
    public static Consumer<VoidTidePacket> ON_VOID_TIDE = p -> {};
    public static Consumer<FlamePillarBeamPacket> ON_FLAME_BEAM = p -> {};
    public static Consumer<FinaleTextPacket> ON_FINALE_TEXT = p -> {};
    /** r96：S2C 技能触发成功 → 客户端刷新冷却 HUD。 */
    public static Consumer<AbilityCooldownPacket> ON_ABILITY_COOLDOWN = p -> {};
    /** r102：S2C 世界空间后处理效果（冲击波等）。 */
    public static Consumer<ShaderEffectPacket> ON_SHADER_EFFECT = p -> {};
    /** S2C 服务端权威的终极饰品佩戴状态（客户端 Curios 同步兜底）。 */
    public static Consumer<UltimateStatePacket> ON_ULTIMATE_STATE = p -> {};

    private static int index;

    public static void init() {
        CHANNEL.registerMessage(index++, PhaseTitlePacket.class,
                PhaseTitlePacket::encode, PhaseTitlePacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_PHASE.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, FormationPacket.class,
                FormationPacket::encode, FormationPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_FORMATION.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, DomainPacket.class,
                DomainPacket::encode, DomainPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_DOMAIN.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, WreathUsePacket.class,
                (msg, buf) -> {}, buf -> WreathUsePacket.INSTANCE,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender != null && TerminusWreath.activate(sender)) {
                            sendAbilityCooldown(sender, 2); // TERMINUS_WREATH
                        }
                    });
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, BlinkPacket.class,
                (msg, buf) -> {}, buf -> BlinkPacket.INSTANCE,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender != null && AstralBlink.blink(sender)) {
                            sendAbilityCooldown(sender, 1); // ASTRAL_BLINK
                        }
                    });
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, FrostRecallPacket.class,
                (msg, buf) -> {}, buf -> FrostRecallPacket.INSTANCE,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender != null && FrostChimeRecall.use(sender)) {
                            sendAbilityCooldown(sender, 0); // FROST_RECALL
                        }
                    });
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, BossFxPacket.class,
                BossFxPacket::encode, BossFxPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_BOSS_FX.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, MarkerTargetPacket.class,
                MarkerTargetPacket::encode, MarkerTargetPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_MARKER.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, CircleTargetPacket.class,
                CircleTargetPacket::encode, CircleTargetPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_CIRCLE.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, WindEyePacket.class,
                WindEyePacket::encode, WindEyePacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_WIND_EYE.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, VoidTidePacket.class,
                VoidTidePacket::encode, VoidTidePacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_VOID_TIDE.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, FlamePillarBeamPacket.class,
                FlamePillarBeamPacket::encode, FlamePillarBeamPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_FLAME_BEAM.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, FinaleTextPacket.class,
                FinaleTextPacket::encode, FinaleTextPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_FINALE_TEXT.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, EternalTogglePacket.class,
                EternalTogglePacket::encode, EternalTogglePacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender != null
                                && cn.blockforge.generated.floralritebloomcurios.util.FRBState.hasUltimate(sender)) {
                            boolean enabled = msg.offHand()
                                    ? cn.blockforge.generated.floralritebloomcurios.util.FRBState
                                    .toggleUltimateRecall(sender)
                                    : cn.blockforge.generated.floralritebloomcurios.util.FRBState
                                    .toggleGrowth(sender);
                            sender.displayClientMessage(Component.translatable(msg.offHand()
                                    ? (enabled ? "frb.eternal.recall_on" : "frb.eternal.recall_off")
                                    : (enabled ? "frb.growth.enabled" : "frb.growth.disabled")), true);
                        }
                    });
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, LockTargetPacket.class,
                LockTargetPacket::encode, LockTargetPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender == null) return;
                        Entity e = sender.level().getEntity(msg.entityId());
                        if (!(sender.getMainHandItem().getItem()
                                instanceof cn.blockforge.generated.floralritebloomcurios.item.BountifulItem)) {
                            return;
                        }
                        if (!(e instanceof net.minecraft.world.entity.LivingEntity target
                                && target instanceof net.minecraft.world.entity.monster.Enemy)
                                || !target.isAlive()
                                || sender.distanceToSqr(target) > 24.0D * 24.0D) {
                            return;
                        }
                        cn.blockforge.generated.floralritebloomcurios.item.BountifulEvents.lockTarget(sender, target);
                    });
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, DoubleJumpPacket.class,
                (msg, buf) -> {}, buf -> DoubleJumpPacket.INSTANCE,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> {
                        ServerPlayer sender = sup.get().getSender();
                        if (sender != null && !sender.onGround()
                                && sender.getPersistentData().getInt(
                                cn.blockforge.generated.floralritebloomcurios.util.FRBState.TAG_AIR_TICKS) >= 3
                                && !sender.isPassenger()
                                && !sender.getAbilities().flying && !sender.isFallFlying()
                                && !sender.isInWater()
                                && cn.blockforge.generated.floralritebloomcurios.util.FRBState.active(
                                        sender, cn.blockforge.generated.floralritebloomcurios.charm.CharmType.SKY_PILLAR)
                                && !sender.getPersistentData().getBoolean(
                                        cn.blockforge.generated.floralritebloomcurios.util.FRBState.TAG_DOUBLE_JUMP_USED)) {
                            sender.getPersistentData().putBoolean(
                                    cn.blockforge.generated.floralritebloomcurios.util.FRBState.TAG_DOUBLE_JUMP_USED, true);
                            var motion = sender.getDeltaMovement();
                            sender.setDeltaMovement(motion.x, 0.42D, motion.z);
                            sender.hurtMarked = true;
                            sender.serverLevel().sendParticles(
                                    cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles.petal(
                                            cn.blockforge.generated.floralritebloomcurios.charm.CharmType.SKY_PILLAR),
                                    sender.getX(), sender.getY() + 0.2D, sender.getZ(),
                                    8, 0.15D, 0.05D, 0.15D, 0.04D);
                        }
                    });
                    sup.get().setPacketHandled(true);
                });
        // r96：S2C 技能冷却通知（服务端确认触发成功后下发，客户端刷新 HUD）
        CHANNEL.registerMessage(index++, AbilityCooldownPacket.class,
                AbilityCooldownPacket::encode, AbilityCooldownPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_ABILITY_COOLDOWN.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        // r102：S2C 世界空间后处理效果（冲击波等）
        CHANNEL.registerMessage(index++, ShaderEffectPacket.class,
                ShaderEffectPacket::encode, ShaderEffectPacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_SHADER_EFFECT.accept(msg));
                    sup.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(index++, UltimateStatePacket.class,
                UltimateStatePacket::encode, UltimateStatePacket::decode,
                (msg, sup) -> {
                    sup.get().enqueueWork(() -> ON_ULTIMATE_STATE.accept(msg));
                    sup.get().setPacketHandled(true);
                });
    }

    // ---- 发送助手（1.20.1：PacketDistributor + SimpleChannel#send） ----

    public static void sendPhaseTitle(ServerPlayer player, int entityId, int phase) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PhaseTitlePacket(entityId, phase));
    }

    public static void sendPhaseTracking(ServerLevel level, Entity entity, int phase) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new PhaseTitlePacket(entity.getId(), phase));
    }

    public static void sendFormationToTracking(ServerLevel level, BlockPos pos, int kind) {
        FormationPacket packet = new FormationPacket(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D, kind);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64.0D * 64.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
    }

    public static void sendFormation(ServerLevel level, double x, double y, double z, int kind) {
        FormationPacket packet = new FormationPacket(x, y, z, kind);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(x, y, z) < 64.0D * 64.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
    }

    public static void sendDomain(ServerLevel level, Entity center, int color) {
        DomainPacket packet = new DomainPacket(center.getX(), center.getY() + 1.0D, center.getZ(), color);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> center), packet);
    }

    /** 向 Boss 周围 96 格玩家广播镜头演出（震动/白闪/冲击波/染色，color=0 表示无色）。 */
    public static void sendBossFx(ServerLevel level, Entity center, int fx, float strength, int duration) {
        sendBossFx(level, center, fx, strength, duration, 0);
    }

    public static void sendBossFx(ServerLevel level, Entity center, int fx, float strength, int duration, int color) {
        BossFxPacket packet = new BossFxPacket(fx, strength, duration, color);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                center.getBoundingBox().inflate(96.0D))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
        }
    }

    /** 同 {@link #sendBossFx(ServerLevel, Entity, int, float, int)}，以方块位置为中心。 */
    public static void sendBossFx(ServerLevel level, BlockPos pos, int fx, float strength, int duration) {
        sendBossFx(level, pos, fx, strength, duration, 0);
    }

    public static void sendBossFx(ServerLevel level, BlockPos pos, int fx, float strength, int duration, int color) {
        BossFxPacket packet = new BossFxPacket(fx, strength, duration, color);
        var box = new net.minecraft.world.phys.AABB(pos).inflate(96.0D);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
        }
    }

    /** 万花凋零咏唱开始：向 boss 周围玩家发送 FX_FINALE shader pass + 屏幕文本两行。 */
    public static void sendFinaleStart(ServerLevel level, Entity boss, int durationTicks,
                                       String line1Key, String line2Key, int remainingSeconds) {
        // shader 通道（独立位 FX_FINALE=128）
        BossFxPacket fx = new BossFxPacket(BossFxPacket.FX_FINALE, 1.0F, durationTicks, 0);
        // 屏幕文本（独立通道 FinaleTextPacket）
        FinaleTextPacket text = new FinaleTextPacket(boss.getId(),
                Component.translatable(line1Key).getString(),
                Component.translatable(line2Key, remainingSeconds).getString(),
                durationTicks);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                boss.getBoundingBox().inflate(96.0D))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), fx);
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), text);
        }
    }

    /** 万花凋零咏唱倒计时文本更新（不重启 shader pass，只刷文本）。 */
    public static void sendFinaleTextUpdate(ServerLevel level, Entity boss,
                                            String line1Key, String line2Key, int remainingSeconds) {
        FinaleTextPacket text = new FinaleTextPacket(boss.getId(),
                Component.translatable(line1Key).getString(),
                Component.translatable(line2Key, remainingSeconds).getString(),
                remainingSeconds * 20);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                boss.getBoundingBox().inflate(96.0D))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), text);
        }
    }

    /** 万花凋零咏唱结束：清空屏幕文本（remainingTicks=0）。 */
    public static void sendFinaleEnd(ServerLevel level, Entity boss) {
        FinaleTextPacket text = new FinaleTextPacket(boss.getId(), "", "", 0);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                boss.getBoundingBox().inflate(96.0D))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), text);
        }
    }

    /** r96：通知指定玩家技能触发成功，刷新客户端冷却 HUD。kindOrdinal 对应 AbilityHudOverlay.Kind 顺序。 */
    public static void sendAbilityCooldown(ServerPlayer player, int kindOrdinal) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AbilityCooldownPacket(kindOrdinal));
    }

    /**
     * r102：向中心实体附近所有玩家广播世界空间后处理效果。
     * name 支持 "Shockwave"（波纹扩散）/ "Wormhole"（虫洞镜头变形）。lifeTime 单位 tick，scale 为效果缩放。
     */
    public static void sendShaderEffect(ServerLevel level, Entity center, String name, int lifeTime, float scale) {
        ShaderEffectPacket pkt = new ShaderEffectPacket(
                level.dimension().location().toString(), name,
                center.getX(), center.getY(), center.getZ(), lifeTime, scale);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                center.getBoundingBox().inflate(96.0D))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }

    // ---- 通用预警系统（A2 框架） ----

    /** 向指定玩家广播头顶标记 FX（duration 内客户端在目标玩家头顶 1.5 格渲染）。 */
    public static void sendMarker(ServerPlayer target, int duration, int color) {
        MarkerTargetPacket pkt = new MarkerTargetPacket(target.getId(), duration, color);
        for (ServerPlayer p : target.serverLevel().players()) {
            if (p.distanceToSqr(target) < 96.0D * 96.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
    }

    /**
     * 向指定玩家广播地面预警圈 FX（duration 内客户端在目标位置持续渲染半径环）。
     * kind 0=红色警戒，1=冰蓝标记，2=橙色冲击。
     */
    public static void sendCircle(ServerPlayer target, BlockPos ground, double radius, int duration, int kind) {
        CircleTargetPacket pkt = new CircleTargetPacket(target.getId(),
                ground.getX() + 0.5D, ground.getY() + 0.02D, ground.getZ() + 0.5D,
                radius, duration, kind);
        for (ServerPlayer p : target.serverLevel().players()) {
            if (p.distanceToSqr(ground.getX(), ground.getY(), ground.getZ()) < 96.0D * 96.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
    }

    /**
     * r86：向 boss 附近所有玩家广播风眼月环地面贴图预警。
     * 在 (x, y, z) 处显示半径 outerRadius 的 wind_eye_4_18.png 贴图，持续 duration tick。
     */
    public static void sendWindEye(ServerLevel level, double x, double y, double z,
                                    float outerRadius, int duration) {
        WindEyePacket pkt = new WindEyePacket(x, y, z, outerRadius, duration);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(x, y, z) < 96.0D * 96.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
    }

    /** r90：向附近玩家广播虚空潮汐地面预警贴图。kind=0 环形, kind=1 圆形。 */
    public static void sendVoidTide(ServerLevel level, double x, double y, double z,
                                     float radius, int duration, int kind) {
        VoidTidePacket pkt = new VoidTidePacket(x, y, z, radius, duration, kind);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(x, y, z) < 96.0D * 96.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
    }

    /** r96：广播光辉炎柱的信标光柱渲染指令（橙色调 beacon_beam）。 */
    public static void sendFlameBeam(ServerLevel level, double x, double y, double z,
                                     float radius, float height, int duration) {
        FlamePillarBeamPacket pkt = new FlamePillarBeamPacket(x, y, z, radius, height, duration);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(x, y, z) < 96.0D * 96.0D) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            }
        }
    }

    /** S2C：同步服务端权威的终极饰品佩戴状态。 */
    public static void sendUltimateState(ServerPlayer player, boolean worn) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UltimateStatePacket(worn));
    }

    private FRBNetwork() {}
}
