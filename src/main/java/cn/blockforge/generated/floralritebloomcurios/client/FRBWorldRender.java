package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.blockforge.generated.floralritebloomcurios.client.FRBClient;

/**
 * 世界空间渲染：云雨阶段环绕 Boss 的传送符文——剩余几次就画几枚，用尽即全部消失。
 *
 * 注意：RenderLevelStageEvent 给的 PoseStack 是“相机在原点”的空间，
 * 所以世界坐标必须先减去相机位置（此前直接写世界坐标会让符文整体偏出视野）。
 * 焚生藤的花瓣印记不在这里画，改到生物自己的模型空间里，见 {@link PetalMarkOverlay}。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBWorldRender {

    private static final ResourceLocation RUNE =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/rune.png");
    /** r91：八阶段属性边界 emblem，下标与 FriliasEntity#phase 对齐
     *  （自然/焚尽/霜华/云雨/天空/虚空/枯萎/时间）。 */
    private static final ResourceLocation[] BOUNDARY_TEXTURES = {
            boundary("boundary_nature"), boundary("boundary_fire"), boundary("boundary_ice"),
            boundary("boundary_rain"), boundary("boundary_sky"), boundary("boundary_void"),
            boundary("boundary_death"), boundary("boundary_time")
    };
    private static final RenderType[] BOUNDARY_RENDER_TYPES = buildBoundaryRenderTypes();
    /** 万花万草之灵·终末花环领域边界 emblem（替换旧 rune.png）。 */
    private static final ResourceLocation BOUNDARY_END = boundary("boundary_end");
    private static final RenderType BOUNDARY_END_RT = RenderType.entityTranslucent(BOUNDARY_END);
    /** Boss 属性边界环半径与密度（密度与终末花环 64 枚/32 格一致 → 24 格 = 48 枚）。 */
    private static final double BOSS_BOUNDARY_RADIUS = 24.0D;
    private static final int BOSS_BOUNDARY_EMBLEMS = 48;
    private static final int FULL_LIGHT = 0xF000F0;

    private static ResourceLocation boundary(String name) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/particle/" + name + ".png");
    }

    private static RenderType[] buildBoundaryRenderTypes() {
        RenderType[] types = new RenderType[BOUNDARY_TEXTURES.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = RenderType.entityTranslucent(BOUNDARY_TEXTURES[i]);
        }
        return types;
    }
    private static final List<WreathDomain> WREATH_DOMAINS = new ArrayList<>();
    /** kind=0 红圈，kind=1 冰蓝，kind=2 橙色。 */
    private static final int[] CIRCLE_COLORS = {0xff5050, 0x9fdcff, 0xff8a3c};

    private record WreathDomain(double x, double y, double z, int color, int age) {}

    public static void addWreathDomain(cn.blockforge.generated.floralritebloomcurios.network.DomainPacket packet) {
        WREATH_DOMAINS.add(new WreathDomain(packet.x(), packet.y(), packet.z(), packet.color(), 0));
    }

    public static void tickWreathDomains() {
        for (int i = WREATH_DOMAINS.size() - 1; i >= 0; i--) {
            WreathDomain d = WREATH_DOMAINS.get(i);
            if (d.age() >= cn.blockforge.generated.floralritebloomcurios.abilities.TerminusWreath.DURATION_TICKS) {
                WREATH_DOMAINS.remove(i);
            } else {
                WREATH_DOMAINS.set(i, new WreathDomain(d.x(), d.y(), d.z(), d.color(), d.age() + 1));
            }
        }
    }

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !event.getCamera().isInitialized()) return;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        float pt = event.getPartialTick();

        VertexConsumer runeBuf = buffers.getBuffer(RenderType.entityTranslucent(RUNE));
        for (FriliasEntity boss : mc.level.getEntitiesOfClass(FriliasEntity.class,
                mc.player.getBoundingBox().inflate(64.0D))) {
            if (boss.getFrbPhase() != 3) continue;
            int runes = boss.getRunesLeft();
            Vec3 anchor = boss.getPosition(pt);
            double age = boss.tickCount + pt;
            for (int i = 0; i < runes; i++) {
                double a = age * 0.045D + i * Math.PI * 0.5D;
                double rx = anchor.x + Math.cos(a) * 2.6D - camPos.x;
                double ry = anchor.y + 1.25D + Math.sin(age * 0.06D + i) * 0.25D - camPos.y;
                double rz = anchor.z + Math.sin(a) * 2.6D - camPos.z;
                pose.pushPose();
                pose.translate(rx, ry, rz);
                pose.mulPose(cam.rotation());
                float glow = 0.75F + 0.25F * (float) Math.sin(age * 0.12D + i * 1.7D);
                // 云雨阶段使用放大的天蓝色传送标记。
                QuadBills.quadUv(pose, runeBuf, 1.05F, 1.05F, 0.0F, 0.0F, 1.0F, 1.0F,
                        glow * 0.45F, glow * 0.82F, glow * 1.15F, glow, FULL_LIGHT);
                pose.popPose();
            }
        }

        // r91：八阶段属性边界环——永恒花园维度内天气无渲染，改用当前阶段属性 emblem
        //      围住 boss 24 格范围（与服务端 SEAL_RADIUS 一致），开场演出期间不显示。
        for (FriliasEntity boss : mc.level.getEntitiesOfClass(FriliasEntity.class,
                mc.player.getBoundingBox().inflate(64.0D))) {
            if (boss.isIntro()) continue;
            int phase = Math.floorMod(boss.getFrbPhase(), BOUNDARY_RENDER_TYPES.length);
            Vec3 anchor = boss.getPosition(pt);
            double age = boss.tickCount + pt;
            // 永恒花园为平坦超平坦地形，环上高度与中心一致，取一次地表高度即可
            int groundY = mc.level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(anchor.x), Mth.floor(anchor.z));
            VertexConsumer boundaryBuf = buffers.getBuffer(BOUNDARY_RENDER_TYPES[phase]);
            for (int i = 0; i < BOSS_BOUNDARY_EMBLEMS; i++) {
                double a = i * Math.PI * 2.0D / BOSS_BOUNDARY_EMBLEMS + age * 0.012D;
                double rx = anchor.x + Math.cos(a) * BOSS_BOUNDARY_RADIUS - camPos.x;
                double ry = groundY + 0.55D + Math.sin(age * 0.06D + i) * 0.22D - camPos.y;
                double rz = anchor.z + Math.sin(a) * BOSS_BOUNDARY_RADIUS - camPos.z;
                pose.pushPose();
                pose.translate(rx, ry, rz);
                pose.mulPose(cam.rotation());
                float pulse = 0.78F + 0.22F * (float) Math.sin(age * 0.18D + i * 0.7D);
                QuadBills.quadUv(pose, boundaryBuf, 0.9F, 0.9F, 0.0F, 0.0F, 1.0F, 1.0F,
                        pulse, pulse, pulse, pulse, FULL_LIGHT);
                pose.popPose();
            }
        }

        // r91 终末花环：领域边界一圈 boundary_end 圣花环 emblem（替换旧 rune.png）。
        VertexConsumer wreathBuf = buffers.getBuffer(BOUNDARY_END_RT);
        for (WreathDomain domain : WREATH_DOMAINS) {
            double age = domain.age();
            int runes = 64;
            for (int i = 0; i < runes; i++) {
                double a = i * Math.PI * 2.0D / runes + age * 0.012D;
                double rx = domain.x() + Math.cos(a) * 32.0D - camPos.x;
                double ry = domain.y() - camPos.y + 0.15D;
                double rz = domain.z() + Math.sin(a) * 32.0D - camPos.z;
                pose.pushPose();
                pose.translate(rx, ry, rz);
                pose.mulPose(cam.rotation());
                float pulse = 0.74F + 0.26F * (float) Math.sin(age * 0.18D + i * 0.7D);
                QuadBills.quadUv(pose, wreathBuf, 0.8F, 0.8F, 0.0F, 0.0F, 1.0F, 1.0F,
                        pulse, pulse, pulse, 0.94F, FULL_LIGHT);
                pose.popPose();
            }
        }
        // 地面预警圈（来自 FRBClient.ACTIVE_CIRCLES，按 kind 取色，绕 Y 轴旋转 + 渐进脉冲）
        var circles = FRBClient.activeCirclesSnapshot();
        for (var rec : circles) {
            int colorIdx = Math.floorMod(rec.kind(), CIRCLE_COLORS.length);
            int color = CIRCLE_COLORS[colorIdx];
            float r = ((color >> 16) & 255) / 255.0F;
            float g = ((color >> 8) & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            double frac = (double) rec.age() / (double) Math.max(1, rec.duration());
            float pulse = 0.7F + 0.3F * (float) Math.sin(rec.age() * 0.25F);
            float alpha = 0.55F + 0.35F * (1.0F - (float) frac);
            int segments = 48;
            double radius = rec.radius();
            for (int i = 0; i < segments; i++) {
                double a = i * Math.PI * 2.0D / segments + rec.age() * 0.06D;
                double rx = rec.x() + Math.cos(a) * radius - camPos.x;
                double ry = rec.y() - camPos.y + 0.02D;
                double rz = rec.z() + Math.sin(a) * radius - camPos.z;
                pose.pushPose();
                pose.translate(rx, ry, rz);
                pose.mulPose(cam.rotation());
                QuadBills.quadUv(pose, runeBuf, 0.5F, 0.5F, 0.0F, 0.0F, 1.0F, 1.0F,
                        r * pulse, g * pulse, b * pulse, alpha, FULL_LIGHT);
                pose.popPose();
            }
        }
        // r86 风眼月环贴图（wind_eye_4_18.png）：贴地 XZ quad，外半径 = outerRadius
        // 内安全区 = outerRadius * 4/18。预警期渐变 alpha，临近结算更亮。
        var windEyes = FRBClient.activeWindEyesSnapshot();
        for (var rec : windEyes) {
            double frac = (double) rec.age() / (double) Math.max(1, rec.duration());
            float alpha = 0.55F + 0.4F * (1.0F - (float) frac);
            pose.pushPose();
            pose.translate(rec.x() - camPos.x, rec.y() - camPos.y + 0.05D, rec.z() - camPos.z);
            // r90：风眼月环应平铺地面，不乘相机旋转（顶点已是 XZ 水平面）
            WindEyeGroundRenderer.draw(pose, buffers, rec.outerRadius(), alpha);
            pose.popPose();
        }
        // r90 虚空潮汐贴图（void_aoe_ring/circle.png）：贴地 XZ quad
        var voidTides = FRBClient.activeVoidTidesSnapshot();
        for (var rec : voidTides) {
            double frac = (double) rec.age() / (double) Math.max(1, rec.duration());
            float alpha = 0.55F + 0.4F * (1.0F - (float) frac);
            pose.pushPose();
            pose.translate(rec.x() - camPos.x, rec.y() - camPos.y + 0.05D, rec.z() - camPos.z);
            VoidTideGroundRenderer.draw(pose, buffers, rec.radius(), alpha, rec.kind());
            pose.popPose();
        }
        // r96 光辉炎柱光柱：信标 beam 贴图 + 橙色调，两个垂直交叉 quad
        renderFlameBeams(pose, buffers, camPos);
        buffers.endBatch();
    }

    /** r98 光辉炎柱光柱渲染：用户自绘 flame_pillar.png（64x256 竖向火焰），十字交叉 quad。 */
    private static final ResourceLocation BEAM_TEX =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/effect/flame_pillar.png");
    private static final RenderType BEAM_LAYER = RenderType.entityTranslucent(BEAM_TEX);

    private static void renderFlameBeams(PoseStack pose, MultiBufferSource buffers, Vec3 camPos) {
        var beams = FRBClient.activeFlameBeamsSnapshot();
        if (beams.isEmpty()) return;
        VertexConsumer buf = buffers.getBuffer(BEAM_LAYER);
        for (var b : beams) {
            double frac = (double) b.age() / (double) Math.max(1, b.duration());
            // 前半段渐亮，后半段渐暗
            float a = frac < 0.25F ? (float) (frac / 0.25D) : 1.0F - (float) ((frac - 0.25D) / 0.75D);
            a = Mth.clamp(a, 0.0F, 1.0F) * 0.95F;
            float red = 1.0F;
            float green = 0.45F + 0.15F * (1.0F - (float) frac);
            float blue = 0.08F;
            float r = b.radius();
            float h = b.height();
            double bx = b.x() - camPos.x;
            double by = b.y() - camPos.y;
            double bz = b.z() - camPos.z;
            // 两个垂直交叉的竖直 quad（X 向 + Z 向）
            for (int axis = 0; axis < 2; axis++) {
                pose.pushPose();
                pose.translate(bx, by, bz);
                if (axis == 1) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                pose.scale(r, 1.0F, 1.0F);
                drawBeamQuad(pose, buf, h, red, green, blue, a);
                pose.popPose();
            }
        }
    }

    /** 画一个竖直 quad：宽 2（左右各 1）、高 h，UV 纵向平铺 flame_pillar 贴图。 */
    private static void drawBeamQuad(PoseStack pose, VertexConsumer buf, float h,
                                     float r, float g, float b, float a) {
        com.mojang.blaze3d.vertex.PoseStack.Pose m = pose.last();
        // 四个顶点：(-1,0) (-1,h) (1,h) (1,0)，UV 横向 0~1，纵向按高度缩放
        float u0 = 0.0F, u1 = 1.0F;
        float v0 = 0.0F, v1 = h / 8.0F; // 每 8 格重复一次贴图
        int light = 0xF000F0;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        buf.vertex(m.pose(), -1.0F, 0.0F, 0.0F).color(r, g, b, a).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(m.normal(), 0F, 0F, -1F).endVertex();
        buf.vertex(m.pose(), -1.0F, h, 0.0F).color(r, g, b, a).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(m.normal(), 0F, 0F, -1F).endVertex();
        buf.vertex(m.pose(), 1.0F, h, 0.0F).color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(m.normal(), 0F, 0F, -1F).endVertex();
        buf.vertex(m.pose(), 1.0F, 0.0F, 0.0F).color(r, g, b, a).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(m.normal(), 0F, 0F, -1F).endVertex();
    }

    private FRBWorldRender() {}
}
