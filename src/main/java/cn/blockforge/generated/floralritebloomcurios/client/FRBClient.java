package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.network.BlinkPacket;
import cn.blockforge.generated.floralritebloomcurios.network.CircleTargetPacket;
import cn.blockforge.generated.floralritebloomcurios.network.DomainPacket;
import cn.blockforge.generated.floralritebloomcurios.network.DoubleJumpPacket;
import cn.blockforge.generated.floralritebloomcurios.network.EternalTogglePacket;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.network.FormationPacket;
import cn.blockforge.generated.floralritebloomcurios.network.FrostRecallPacket;
import cn.blockforge.generated.floralritebloomcurios.network.MarkerTargetPacket;
import cn.blockforge.generated.floralritebloomcurios.network.PhaseTitlePacket;
import cn.blockforge.generated.floralritebloomcurios.network.WreathUsePacket;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端（FORGE 总线）：按键（R 终末花环 / G 星界折跃 / 空格二段跳）、阶段标题、
 * 法阵绘制、极寒 / 天之泪环境粒子。注册类事件见 FRBSetup。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBClient {

    public static final KeyMapping WREATH_KEY = new KeyMapping(
            "key.frb.wreath", GLFW.GLFW_KEY_R, "key.categories.frb");
    public static final KeyMapping BLINK_KEY = new KeyMapping(
            "key.frb.blink", GLFW.GLFW_KEY_G, "key.categories.frb");
    public static final KeyMapping FROST_RECALL_KEY = new KeyMapping(
            "key.frb.frost_recall", GLFW.GLFW_KEY_V, "key.categories.frb");
    /** r77：HUD 编辑键（默认未绑，由玩家在控制面板自定义）。 */
    public static final KeyMapping HUD_EDIT_KEY = new KeyMapping(
            "key.frb.hud_edit", GLFW.GLFW_KEY_H, "key.categories.frb");

    private static boolean prevJump;
    private static boolean airJumpReady = true;
    private static boolean pendingAirJump;
    private static int airborneTicks;
    /** -2：无；-1：苏醒公告后；0..6：当前 Boss 阶段。 */
    private static int ambientPhase = -2;
    private static final List<FormationAnim> FORMATIONS = new ArrayList<>();

    private record FormationAnim(double x, double y, double z, int kind, int age) {}

    static {
        FRBNetwork.ON_PHASE = p -> {
            Minecraft mc = Minecraft.getInstance();
            if (p.phase() == -1) {
                mc.gui.setTitle(Component.translatable("frb.boss.awaken.title")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                mc.gui.setSubtitle(Component.translatable("frb.boss.awaken.sub")
                        .withStyle(ChatFormatting.AQUA));
                ambientPhase = 0;
                return;
            }
            int phase = p.phase();
            // r75：PHASE_COUNT=7 不再含 time (7)；改用 PHASE_KEYS.length 允许 time 阶段显示。
            if (phase < 0 || phase >= FriliasEntity.PHASE_KEYS.length) return;
            ambientPhase = phase;
            mc.gui.setSubtitle(Component.empty());
            // r75：外层套 DARK_PURPLE 让 "第 X 阶段 · " 永远是紫色（不被阶段色覆盖），
            //      内层阶段名（如"霜华"）单独按 PHASE_FORMAT 取该阶段色。修复"阶段"二字被染色 bug。
            mc.gui.setTitle(Component.translatable("frb.boss.phase.title",
                            phase + 1, Component.translatable("frb.boss.phase." + FriliasEntity.PHASE_KEYS[phase])
                                    .withStyle(FriliasEntity.PHASE_FORMAT[phase]))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        };
        FRBNetwork.ON_FORMATION = p -> FORMATIONS.add(new FormationAnim(p.x(), p.y(), p.z(), p.kind(), 0));
        FRBNetwork.ON_DOMAIN = FRBClient::renderDomain;
        FRBNetwork.ON_BOSS_FX = FRBBossFxClient::handle;
        FRBNetwork.ON_MARKER = FRBClient::onMarker;
        FRBNetwork.ON_CIRCLE = FRBClient::onCircle;
        FRBNetwork.ON_WIND_EYE = FRBClient::onWindEye;
        FRBNetwork.ON_VOID_TIDE = FRBClient::onVoidTide;
        FRBNetwork.ON_FLAME_BEAM = FRBClient::onFlameBeam;
        FRBNetwork.ON_FINALE_TEXT = FRBClient::onFinaleText;
        // r102：世界空间后处理效果（冲击波等）
        FRBNetwork.ON_SHADER_EFFECT = p -> cn.blockforge.generated.floralritebloomcurios.client.shader.FRBShaderEffects
                .add(p.dimensionKey(), p.name(), p.pos(), p.lifeTime(), p.scale());
        // r96：服务端确认技能触发成功后，客户端才刷新冷却 HUD（避免未触发也进 cd）
        FRBNetwork.ON_ABILITY_COOLDOWN = p -> {
            AbilityHudOverlay.Kind[] kinds = AbilityHudOverlay.Kind.values();
            if (p.kindOrdinal() >= 0 && p.kindOrdinal() < kinds.length) {
                AbilityHudOverlay.markUse(kinds[p.kindOrdinal()]);
            }
        };
        // 服务端权威的终极饰品佩戴状态，兜底 Curios 客户端同步延迟
        FRBNetwork.ON_ULTIMATE_STATE = p -> cn.blockforge.generated.floralritebloomcurios.util.FRBState
                .setClientUltimateWorn(p.worn());
    }

    /** FinaleTextPacket 客户端接收：刷新屏幕咏唱文本 + 同步 boss 位置到 shader uniform。 */
    public static void onFinaleText(cn.blockforge.generated.floralritebloomcurios.network.FinaleTextPacket p) {
        if (p.remainingTicks() <= 0 || p.line1().isEmpty()) {
            activeFinaleLine1 = "";
            activeFinaleLine2 = "";
            activeFinaleRemaining = 0;
            activeFinaleEntityId = -1;
            return;
        }
        activeFinaleLine1 = p.line1();
        activeFinaleLine2 = p.line2();
        activeFinaleRemaining = p.remainingTicks();
        activeFinaleEntityId = p.entityId();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity boss = mc.level.getEntity(p.entityId());
            if (boss != null) {
                FRBBossFxClient.startFinale(p.remainingTicks(),
                        boss.getX(), boss.getY(), boss.getZ());
            }
        }
    }

    // ---- 头顶标记 / 地面预警圈本地驱动 ----

    private static final List<MarkerRecord> ACTIVE_MARKERS = new ArrayList<>();

    // ---- 万花凋零终结技屏幕文本（FinaleTextPacket 客户端本地驱动） ----

    /** 客户端当前显示的万花凋零文本；空表示未激活。 */
    private static String activeFinaleLine1 = "";
    private static String activeFinaleLine2 = "";
    /** 客户端显示剩余 tick（每 tick -1，归零时清空）。 */
    private static int activeFinaleRemaining = 0;
    private static int activeFinaleEntityId = -1;

    /** FRBWorldRender 读取预警圈时使用的不可变快照（避免并发修改）。 */
    public static List<CircleRecord> activeCirclesSnapshot() {
        return java.util.Collections.unmodifiableList(ACTIVE_CIRCLES);
    }

    /** 包内可见（FRBWorldRender 同包内读取），不再 private。r69：携带颜色用于分流粒子。 */
    public record MarkerRecord(int entityId, int duration, int age, int color) {}

    /** 客户端接收 MarkerTargetPacket：把指定 entityId 加入"头顶标记观察列表"。 */
    public static void onMarker(MarkerTargetPacket p) {
        ACTIVE_MARKERS.add(new MarkerRecord(p.entityId(), p.duration(), 0, p.color()));
    }

    private static final List<CircleRecord> ACTIVE_CIRCLES = new ArrayList<>();

    /** 包内可见（FRBWorldRender 同包内读取），不再 private。 */
    public record CircleRecord(int entityId, double x, double y, double z,
                               double radius, int duration, int kind, int age) {}

    public static void onCircle(CircleTargetPacket p) {
        ACTIVE_CIRCLES.add(new CircleRecord(p.entityId(), p.x(), p.y(), p.z(),
                p.radius(), p.duration(), p.kind(), 0));
    }

    // ---- r86 风眼月环贴图预警 ----

    private static final List<WindEyeRecord> ACTIVE_WIND_EYES = new ArrayList<>();
    private static final List<VoidTideRecord> ACTIVE_VOID_TIDES = new ArrayList<>();

    /** 包内可见（FRBWorldRender 读取）。 */
    public record WindEyeRecord(double x, double y, double z,
                                float outerRadius, int duration, int age) {}
    public record VoidTideRecord(double x, double y, double z,
                                 float radius, int duration, int kind, int age) {}

    public static void onWindEye(cn.blockforge.generated.floralritebloomcurios.network.WindEyePacket p) {
        ACTIVE_WIND_EYES.add(new WindEyeRecord(p.x(), p.y(), p.z(),
                p.outerRadius(), p.duration(), 0));
    }

    public static List<WindEyeRecord> activeWindEyesSnapshot() {
        return java.util.Collections.unmodifiableList(ACTIVE_WIND_EYES);
    }

    public static void onVoidTide(cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket p) {
        ACTIVE_VOID_TIDES.add(new VoidTideRecord(p.x(), p.y(), p.z(),
                p.radius(), p.duration(), p.kind(), 0));
    }

    public static List<VoidTideRecord> activeVoidTidesSnapshot() {
        return java.util.Collections.unmodifiableList(ACTIVE_VOID_TIDES);
    }

    // ---- r96 光辉炎柱光柱（信标 beam 渲染） ----

    private static final List<FlameBeamRecord> ACTIVE_FLAME_BEAMS = new ArrayList<>();

    public record FlameBeamRecord(double x, double y, double z,
                                  float radius, float height, int duration, int age) {}

    public static void onFlameBeam(cn.blockforge.generated.floralritebloomcurios.network.FlamePillarBeamPacket p) {
        ACTIVE_FLAME_BEAMS.add(new FlameBeamRecord(p.x(), p.y(), p.z(),
                p.radius(), p.height(), p.duration(), 0));
    }

    public static List<FlameBeamRecord> activeFlameBeamsSnapshot() {
        return java.util.Collections.unmodifiableList(ACTIVE_FLAME_BEAMS);
    }

    /** 每 tick 推进两个列表的寿命；过期自动清理。 */
    private static void tickMarkersAndCircles() {
        for (int i = ACTIVE_MARKERS.size() - 1; i >= 0; i--) {
            MarkerRecord r = ACTIVE_MARKERS.get(i);
            int next = r.age() + 1;
            if (next >= r.duration()) {
                ACTIVE_MARKERS.remove(i);
            } else {
                ACTIVE_MARKERS.set(i, new MarkerRecord(r.entityId(), r.duration(), next, r.color()));
            }
        }
        for (int i = ACTIVE_CIRCLES.size() - 1; i >= 0; i--) {
            CircleRecord r = ACTIVE_CIRCLES.get(i);
            int next = r.age() + 1;
            if (next >= r.duration()) {
                ACTIVE_CIRCLES.remove(i);
            } else {
                ACTIVE_CIRCLES.set(i, new CircleRecord(r.entityId(), r.x(), r.y(), r.z(),
                        r.radius(), r.duration(), r.kind(), next));
            }
        }
        for (int i = ACTIVE_WIND_EYES.size() - 1; i >= 0; i--) {
            WindEyeRecord r = ACTIVE_WIND_EYES.get(i);
            int next = r.age() + 1;
            if (next >= r.duration()) {
                ACTIVE_WIND_EYES.remove(i);
            } else {
                ACTIVE_WIND_EYES.set(i, new WindEyeRecord(r.x(), r.y(), r.z(),
                        r.outerRadius(), r.duration(), next));
            }
        }
        for (int i = ACTIVE_VOID_TIDES.size() - 1; i >= 0; i--) {
            VoidTideRecord r = ACTIVE_VOID_TIDES.get(i);
            int next = r.age() + 1;
            if (next >= r.duration()) {
                ACTIVE_VOID_TIDES.remove(i);
            } else {
                ACTIVE_VOID_TIDES.set(i, new VoidTideRecord(r.x(), r.y(), r.z(),
                        r.radius(), r.duration(), r.kind(), next));
            }
        }
        // r96：光辉炎柱光柱寿命推进
        for (int i = ACTIVE_FLAME_BEAMS.size() - 1; i >= 0; i--) {
            FlameBeamRecord r = ACTIVE_FLAME_BEAMS.get(i);
            int next = r.age() + 1;
            if (next >= r.duration()) {
                ACTIVE_FLAME_BEAMS.remove(i);
            } else {
                ACTIVE_FLAME_BEAMS.set(i, new FlameBeamRecord(r.x(), r.y(), r.z(),
                        r.radius(), r.height(), r.duration(), next));
            }
        }
    }

    private static void renderDomain(DomainPacket p) {
        FRBWorldRender.addWreathDomain(p);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float r = ((p.color() >> 16) & 255) / 255.0F;
        float g = ((p.color() >> 8) & 255) / 255.0F;
        float b = (p.color() & 255) / 255.0F;
        DustParticleOptions col = new DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.5F);
        for (int i = 0; i < 128; i++) {
            double a = i / 128.0D * Math.PI * 2.0D;
            mc.level.addParticle(col, p.x() + Math.cos(a) * 32.0D, p.y(), p.z() + Math.sin(a) * 32.0D,
                    0.0D, 0.08D, 0.0D);
        }
        for (int i = 0; i < 80; i++) {
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, p.x(), p.y() + 1.0D, p.z(),
                    (mc.level.getRandom().nextDouble() - 0.5D) * 60.0D,
                    mc.level.getRandom().nextDouble() * 0.6D,
                    (mc.level.getRandom().nextDouble() - 0.5D) * 60.0D);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        FRBWorldRender.tickWreathDomains();
        Player player = mc.player;
        tickMarkersAndCircles();
        spawnMarkers(level, player);
        if (level == null || player == null) {
            ambientPhase = -2;
            FORMATIONS.clear();
            return;
        }
        var bosses = level.getEntitiesOfClass(FriliasEntity.class, player.getBoundingBox().inflate(96.0D));
        ambientPhase = bosses.isEmpty() ? -2 : bosses.get(0).getFrbPhase();
        for (FriliasEntity boss : bosses) {
            // r66：霜凝蓄力期间 boss 头顶展示 gaze.png 印记；r70 减负：每 4 tick 补一颗（生命 16 tick 不断档）
            if (boss.getFrostGazeTicks() > 0 && player.tickCount % 4 == 0) {
                double bob = Math.sin(player.tickCount * 0.12D) * 0.06D;
                level.addParticle(FRBParticles.GAZE_MARK.get(),
                        boss.getX(), boss.getY() + boss.getBbHeight() + 0.7D + bob, boss.getZ(),
                        0.0D, 0.0D, 0.0D);
            }
            if (boss.getTransitionTicks() <= 0) continue;
            for (int i = 0; i < 3; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double radius = 3.0D + level.random.nextDouble() * 2;
                var petal = mc.particleEngine.createParticle(FRBParticles.CONVERGING_PETAL.get(),
                        boss.getX() + Math.cos(angle) * radius, boss.getY() + level.random.nextDouble() * 3,
                        boss.getZ() + Math.sin(angle) * radius,
                        boss.getX(), boss.getY() + 1.4D, boss.getZ());
                if (petal != null) {
                    int color = FriliasEntity.PHASE_COLORS[boss.getFrbPhase()];
                    petal.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
                }
            }
        }

        while (WREATH_KEY.consumeClick()) {
            FRBNetwork.CHANNEL.sendToServer(WreathUsePacket.INSTANCE);
            // r96：不再本地立即标记冷却——等服务端确认触发成功后由 AbilityCooldownPacket 下发
        }
        while (BLINK_KEY.consumeClick()) {
            FRBNetwork.CHANNEL.sendToServer(BlinkPacket.INSTANCE);
        }
        while (FROST_RECALL_KEY.consumeClick()) {
            FRBNetwork.CHANNEL.sendToServer(FrostRecallPacket.INSTANCE);
        }
        // r77：Shift + H 进入 HUD 编辑屏
        while (HUD_EDIT_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new HudEditScreen());
        }

        // 天穹花：二段跳。第一次起跳后先累计空中时间，再发送一次服务端请求。
        boolean jumpDown = mc.options.keyJump.isDown();
        if (player.onGround()) {
            airJumpReady = true;
            pendingAirJump = false;
            airborneTicks = 0;
        } else {
            airborneTicks++;
            if (jumpDown && !prevJump && airJumpReady
                    && airborneTicks >= 3
                    && !player.isPassenger() && !player.getAbilities().flying
                    && !player.isFallFlying() && !player.isInWater()
                    && wearingClient(CharmType.SKY_PILLAR)) {
                FRBNetwork.CHANNEL.sendToServer(DoubleJumpPacket.INSTANCE);
                pendingAirJump = true;
                airJumpReady = false;
            }
            if (pendingAirJump && player.getDeltaMovement().y > 0.2D) {
                pendingAirJump = false;
                for (int i = 0; i < 8; i++) {
                    level.addParticle(FRBParticles.petal(CharmType.SKY_PILLAR),
                            player.getX(), player.getY() + 0.2D, player.getZ(),
                            (level.getRandom().nextDouble() - 0.5D) * 0.15D,
                            0.05D + level.getRandom().nextDouble() * 0.08D,
                            (level.getRandom().nextDouble() - 0.5D) * 0.15D);
                }
            }
        }
        prevJump = jumpDown;

        // 环境粒子：霜华飘雪 / 云雨落雨 / 枯萎尘灰
        if (ambientPhase >= 0 && player.tickCount % 2 == 0) {
            double x = player.getX() + (level.getRandom().nextDouble() - 0.5D) * 24.0D;
            double y = player.getY() + 8.0D + level.getRandom().nextDouble() * 4.0D;
            double z = player.getZ() + (level.getRandom().nextDouble() - 0.5D) * 24.0D;
            switch (ambientPhase) {
                case 2 -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, -0.08D, 0.0D);
                case 3 -> level.addParticle(ParticleTypes.RAIN, x, y, z, 0.0D, -0.4D, 0.0D);
                case 6 -> level.addParticle(ParticleTypes.ASH, x, y - 4.0D, z, 0.0D, -0.02D, 0.0D);
                default -> {}
            }
        }

        // 万花凋零终结技客户端驱动：
        // 1) 每 tick 同步 boss 位置到 shader uniform（uBossPos）
        // 2) 每 tick 倒计时剩余 tick，归零时清空文本
        if (activeFinaleRemaining > 0) {
            activeFinaleRemaining--;
            if (activeFinaleEntityId >= 0) {
                Entity bossEnt = level.getEntity(activeFinaleEntityId);
                if (bossEnt != null) {
                    FRBBossFxClient.updateFinaleBossPos(bossEnt.getX(), bossEnt.getY(), bossEnt.getZ());
                }
            }
            if (activeFinaleRemaining <= 0) {
                activeFinaleLine1 = "";
                activeFinaleLine2 = "";
            }
        }

        // 法阵动画
        List<FormationAnim> next = new ArrayList<>();
        for (FormationAnim f : FORMATIONS) {
            FormationAnim cur = new FormationAnim(f.x(), f.y(), f.z(), f.kind(), f.age() + 1);
            if (cur.age() >= 44) continue;
            int color = switch (cur.kind()) {
                case 1 -> 0x9fdcff;
                case 2 -> 0x7a2ab0;
                default -> 0x74e88a;
            };
            float r = ((color >> 16) & 255) / 255.0F;
            float g = ((color >> 8) & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            DustParticleOptions opt = new DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.0F);
            double baseRadius = cur.kind() == 2 ? 2.2D : 1.35D;
            // r66：夹住收缩半径（age>34 原来会变负数导致圆环反向放大），最小保持 12% 外圈
            double radius = cur.kind() == 0
                    ? baseRadius * Math.max(0.12D, 1.0D - cur.age() / 34.0D)
                    : baseRadius;
            int points = cur.kind() == 0 ? 20 : 14;
            for (int i = 0; i < points; i++) {
                double a = (i / (double) points) * Math.PI * 2.0D + cur.age() * 0.25D;
                double px = cur.x() + Math.cos(a) * radius;
                double pz = cur.z() + Math.sin(a) * radius;
                level.addParticle(opt, px, cur.y() + 0.05D * Math.sin(a + cur.age()), pz,
                        0.0D, 0.01D, 0.0D);
                // r66：汇聚花瓣改为真正的"四周分点"——固定外圈半径、每个环点都放（原来 7 点 + 后期收缩到
                // 中心附近，看起来像全部从一个点起飞）。花瓣自身沿弧线飞向法阵中心。
                if (cur.kind() == 0 && cur.age() % 2 == 0) {
                    double or = baseRadius;
                    var petal = mc.particleEngine.createParticle(FRBParticles.CONVERGING_PETAL.get(),
                            cur.x() + Math.cos(a) * or, cur.y() + 0.25D, cur.z() + Math.sin(a) * or,
                            cur.x(), cur.y() + 1.15D, cur.z());
                    if (petal != null) petal.setColor(r, g, b);
                }
            }
            if (cur.age() % 6 == 0) {
                level.addParticle(ParticleTypes.END_ROD, cur.x(), cur.y() + 0.4D, cur.z(), 0, 0, 0);
            }
            next.add(cur);
        }
        FORMATIONS.clear();
        FORMATIONS.addAll(next);

        // 若 Boss 已全部离场，恢复环境
        if (ambientPhase >= 0 && player.tickCount % 100 == 0
                && level.getEntitiesOfClass(FriliasEntity.class,
                player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            ambientPhase = -2;
        }
    }

    /** 佩戴终极饰品时空手 Shift 右键：主手切森语藤催熟、副手切寒风铃回溯（服务端校验佩戴）。 */
    @SubscribeEvent
    public static void onRightClickEmpty(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        // 不在客户端做佩戴判定：Curios 的客户端同步在切维度后可能滞后，
        // 客户端一旦误判就会把包拦下来。服务端收到包后会再校验一次。
        FRBNetwork.CHANNEL.sendToServer(new EternalTogglePacket(
                event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND));
    }

    private static boolean wearingClient(CharmType type) {
        return cn.blockforge.generated.floralritebloomcurios.util.FRBState
                .active(Minecraft.getInstance().player, type);
    }

    /** 万花凋零咏唱屏幕文本：在屏幕中下部分双行渲染（行 1 = 咏唱名；行 2 = 倒计时）。 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        // r76：时停期间整屏灰色滤镜（梦幻终焉既视感）。不依赖 shader，纯 GUI fill 半透明灰。
        if (cn.blockforge.generated.floralritebloomcurios.util.TimeStopState
                .isActive(Minecraft.getInstance().level)) {
            GuiGraphics gui = event.getGuiGraphics();
            int w = gui.guiWidth();
            int h = gui.guiHeight();
            // ARGB 0x70707070：alpha 0x70（约 44% 不透明）+ 灰色 0x707070 → 屏幕去色感
            gui.fill(0, 0, w, h, 0x70707070);
            // 屏幕顶端中央放一行提示文字（玩家被锁住）
            var tip = Component.literal("⌛ 时间停滞 ⌛")
                    .withStyle(net.minecraft.ChatFormatting.WHITE);
            gui.drawCenteredString(Minecraft.getInstance().font, tip, w / 2, h / 2 - 80, 0xFFFFFF);
        }
        if (activeFinaleLine1.isEmpty() && activeFinaleLine2.isEmpty()) return;
        GuiGraphics gui = event.getGuiGraphics();
        int w = gui.guiWidth();
        int h = gui.guiHeight();
        int midY = h / 2 + h / 6; // 屏幕偏下 1/6 处
        // 行 1（咏唱名）：白色描边 + 紫色字
        var line1 = Component.literal(activeFinaleLine1)
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        var line2 = Component.literal(activeFinaleLine2)
                .withStyle(net.minecraft.ChatFormatting.WHITE);
        int line1W = Minecraft.getInstance().font.width(line1);
        int line2W = Minecraft.getInstance().font.width(line2);
        gui.drawCenteredString(Minecraft.getInstance().font, line1, w / 2, midY, 0xFFFFFF);
        gui.drawCenteredString(Minecraft.getInstance().font, line2, w / 2, midY + 16, 0xFFFFFF);
    }

    /**
     * 在每个被标记玩家头顶 1.5 格处生成本地标记粒子（r69 按颜色分流，不再统一用雷电）：
     * 0x9fdcff 霜凝 → FROST_EYE 霜眼；0x7ce68b 自然 → GARDEN_PETAL 绿花瓣；
     * 其余（红/黄危险点名，如落雷/灼热之种）保留 LIGHTNING_MARK 雷电。
     */
    private static void spawnMarkers(ClientLevel level, Player self) {
        if (level == null || self == null) return;
        for (MarkerRecord r : ACTIVE_MARKERS) {
            var entity = level.getEntity(r.entityId());
            if (!(entity instanceof Player target) || !target.isAlive()) continue;
            // 玩家自己头顶的标记不需要循环贴脸刷；远距离玩家降频到 5 tick 一颗
            int interval = (target == self) ? 1 : 5;
            if (self.tickCount % interval != 0) continue;
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() + 1.5D;
            double z = target.getZ();
            double jitter = 0.25D;
            net.minecraft.core.particles.ParticleOptions type = switch (r.color()) {
                case 0x9fdcff -> FRBParticles.FROST_EYE.get();
                case 0x7ce68b -> FRBParticles.GARDEN_PETAL.get();
                default -> FRBParticles.LIGHTNING_MARK.get();
            };
            level.addParticle(type,
                    x + (level.random.nextDouble() - 0.5D) * jitter,
                    y + (level.random.nextDouble() - 0.5D) * 0.1D,
                    z + (level.random.nextDouble() - 0.5D) * jitter,
                    0.0D, 0.02D, 0.0D);
        }
    }

    private FRBClient() {}
}
