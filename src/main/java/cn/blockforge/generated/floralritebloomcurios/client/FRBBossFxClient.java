package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.network.BossFxPacket;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 客户端 Boss 战镜头演出：镜头震动 / FOV 脉冲 / 全屏白闪 / 径向扭曲冲击波。
 * 由服务端 BossFxPacket 触发；全部毫秒驱动、帧率无关，自动随时间衰减。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBBossFxClient {

    private static long shakeUntilMs;
    private static long shakeDurMs = 1L;
    private static float shakeAmpDeg;

    private static long flashUntilMs;
    private static long flashDurMs = 1L;
    private static float flashAlpha;

    private static long shockUntilMs;
    private static long shockDurMs = 1L;
    private static float shockAmp = 1.0F;

    private static long grayUntilMs;
    private static float grayStrength;

    /** 转阶段主题色浸染（一次性触发，随时间线性渐退）。 */
    private static long tintUntilMs;
    private static long tintDurMs = 1L;
    private static float tintStrength;
    private static final org.joml.Vector3f TINT_COLOR = new org.joml.Vector3f(1.0F, 1.0F, 1.0F);

    /** 万花凋零黑洞后处理：boss 世界坐标 + 启动时间。 */
    private static long finaleUntilMs;
    private static long finaleDurMs = 1L;
    private static final org.joml.Vector3f FINALE_BOSS_POS = new org.joml.Vector3f(0.0F, 64.0F, 0.0F);

    private static EffectInstance shockShader;
    private static EffectInstance grayShader;
    private static EffectInstance tintShader;
    private static EffectInstance finaleShader;
    private static EffectInstance blitShader;
    private static TextureTarget scratch;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** boss 开场动画的本地检测：首见 intro 实体的时刻，用于镜头扭曲强度随动画进度衰减。 */
    private static long introStartMs = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            introStartMs = -1;
            return;
        }
        boolean anyIntro = false;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FriliasEntity f && f.isAlive() && f.isIntro()) {
                anyIntro = true;
                break;
            }
        }
        if (anyIntro && introStartMs < 0) introStartMs = Util.getMillis();
        if (!anyIntro) introStartMs = -1;
    }

    /** 由 FRBClient 注入的网络回调入口。 */
    public static void handle(BossFxPacket packet) {
        long now = Util.getMillis();
        if ((packet.fx() & BossFxPacket.FX_SHAKE) != 0) {
            shakeDurMs = Math.max(1L, packet.duration() * 50L);
            shakeUntilMs = now + shakeDurMs;
            shakeAmpDeg = 7.0F * packet.strength();
        }
        if ((packet.fx() & BossFxPacket.FX_FLASH) != 0) {
            flashDurMs = Math.max(1L, packet.duration() * 50L);
            flashUntilMs = now + flashDurMs;
            flashAlpha = Math.min(0.95F, 0.85F * packet.strength() + 0.1F);
        }
        if ((packet.fx() & BossFxPacket.FX_SHOCKWAVE) != 0) {
            shockDurMs = Math.max(1L, packet.duration() * 50L);
            shockUntilMs = now + shockDurMs;
            shockAmp = Mth.clamp(packet.strength(), 0.0F, 1.0F);
        }
        if ((packet.fx() & BossFxPacket.FX_GRAY) != 0) {
            grayUntilMs = now + Math.max(1L, packet.duration() * 50L);
            grayStrength = Mth.clamp(packet.strength(), 0.0F, 1.0F);
            // 时停用 FX_GRAY + TIME_PHASE_COLOR(0xE0E0E0)；枯萎阶段用 color=0。
            // 据此区分并设置客户端时停标志，供 LocalPlayerMixin / MinecraftMixin 冻结输入。
            if (packet.color() == FriliasEntity.TIME_PHASE_COLOR) {
                cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.setClientActive(true);
            }
        }
        if ((packet.fx() & BossFxPacket.FX_TINT) != 0) {
            tintDurMs = Math.max(1L, packet.duration() * 50L);
            tintUntilMs = now + tintDurMs;
            tintStrength = Mth.clamp(packet.strength(), 0.0F, 1.0F);
            int c = packet.color();
            TINT_COLOR.set(((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F);
        }
        if ((packet.fx() & BossFxPacket.FX_FINALE) != 0) {
            // 万花凋零 shader 通道启动；uBossPos 由 FRBClient 客户端 tick 持续同步
            finaleDurMs = Math.max(1L, packet.duration() * 50L);
            finaleUntilMs = now + finaleDurMs;
        }
    }

    /** 直接给外部调用（boss 头顶世界坐标 → 启动 shader）：服务端 FinaleStartPacket 间接触发。 */
    public static void startFinale(int durationTicks, double bossX, double bossY, double bossZ) {
        long now = Util.getMillis();
        finaleDurMs = Math.max(1L, durationTicks * 50L);
        finaleUntilMs = now + finaleDurMs;
        FINALE_BOSS_POS.set((float) bossX, (float) bossY, (float) bossZ);
    }

    /** 黑洞坐标跟随 boss 移动（每个 tick 由 FRBClient 调用）。 */
    public static void updateFinaleBossPos(double x, double y, double z) {
        FINALE_BOSS_POS.set((float) x, (float) y, (float) z);
    }

    /** 资源重载后丢弃着色器，下次渲染时重新编译。 */
    public static void invalidateShader() {
        if (shockShader != null) {
            shockShader.close();
            shockShader = null;
        }
        if (grayShader != null) {
            grayShader.close();
            grayShader = null;
        }
        if (tintShader != null) {
            tintShader.close();
            tintShader = null;
        }
        if (finaleShader != null) {
            finaleShader.close();
            finaleShader = null;
        }
        if (blitShader != null) {
            blitShader.close();
            blitShader = null;
        }
    }

    // ---- 镜头震动（转阶段/开场爆发共用） ----

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long rem = shakeUntilMs - Util.getMillis();
        if (rem <= 0) return;
        float frac = Mth.clamp(rem / (float) shakeDurMs, 0.0F, 1.0F);
        float amp = shakeAmpDeg * frac * frac;
        double t = (double) Util.getNanos() / 1.0E9D;
        event.setPitch(event.getPitch() + (float) Math.sin(t * 47.3D) * amp);
        event.setYaw(event.getYaw() + (float) Math.cos(t * 41.7D) * amp);
        event.setRoll((float) Math.sin(t * 35.1D) * amp * 0.7F);
    }

    // ---- FOV 脉冲（跟随镜头震动一起触发） ----

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        long rem = shakeUntilMs - Util.getMillis();
        if (rem <= 0) return;
        float frac = Mth.clamp(rem / (float) shakeDurMs, 0.0F, 1.0F);
        event.setFOV(event.getFOV() + shakeAmpDeg * 1.1D * frac * frac);
    }

    // ---- 白闪渐隐 ----

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long rem = flashUntilMs - Util.getMillis();
        if (rem <= 0) return;
        float frac = Mth.clamp(rem / (float) flashDurMs, 0.0F, 1.0F);
        int alpha = (int) (flashAlpha * frac * frac * 255.0F);
        if (alpha <= 2) return;
        GuiGraphics gui = event.getGuiGraphics();
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (Mth.clamp(alpha, 0, 255) << 24) | 0xFFFFFF);
    }

    // ---- 全屏后处理：灰度褪色（枯萎）→ 径向扭曲冲击波（裂隙 / 开场） ----

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        long now = Util.getMillis();
        boolean gray = now < grayUntilMs;
        // 灰度滤镜过期 → 清除客户端时停标志（LocalPlayerMixin / MinecraftMixin 据此解冻输入）
        if (!gray && cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.isActive(null)) {
            cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.setClientActive(false);
        }
        boolean tint = now < tintUntilMs;
        boolean shock = now < shockUntilMs;
        boolean finale = now < finaleUntilMs;
        // boss 开场动画：镜头扭曲强度随动画进度线性衰减（首帧 0.55 -> 结束 0）
        float introStrength = 0.0F;
        if (introStartMs >= 0) {
            float frac = Mth.clamp(1.0F - (now - introStartMs) / (FriliasEntity.INTRO_TICKS * 50.0F), 0.0F, 1.0F);
            introStrength = 0.55F * frac;
        }
        boolean warp = shock || introStrength > 0.01F;
        if (!gray && !tint && !warp && !finale) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        EffectInstance blit = blitShader(mc);
        if (blit == null || !ensureScratch(main.width, main.height)) return;
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, main.width, main.height, 0.0F, 0.1F, 1000.0F);

        if (gray) { // Pass A：屏幕褪为黑白（恒定强度，枯萎阶段期间持续刷新）
            EffectInstance fx = grayShader(mc);
            if (fx != null) {
                scratch.bindWrite(true);
                fx.safeGetUniform("ProjMat").set(ortho);
                fx.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("Strength").set(grayStrength);
                fx.setSampler("DiffuseSampler", main::getColorTextureId);
                fx.apply();
                drawQuad(main.width, main.height);
                fx.clear();
                copyBack(main, blit, ortho);
            }
        }

        if (tint) { // Pass A2：转阶段主题色浸染（一次性触发，随时间线性渐退）
            EffectInstance fx = tintShader(mc);
            if (fx != null) {
                float frac = Mth.clamp((tintUntilMs - Util.getMillis()) / (float) tintDurMs, 0.0F, 1.0F);
                scratch.bindWrite(true);
                fx.safeGetUniform("ProjMat").set(ortho);
                fx.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("TintColor").set(TINT_COLOR.x, TINT_COLOR.y, TINT_COLOR.z);
                fx.safeGetUniform("Strength").set(tintStrength * frac);
                fx.setSampler("DiffuseSampler", main::getColorTextureId);
                fx.apply();
                drawQuad(main.width, main.height);
                fx.clear();
                copyBack(main, blit, ortho);
            }
        }

        if (warp) { // Pass B：径向扭曲（开场动画叠加 + 冲击波衰减）
            EffectInstance fx = shockShader(mc);
            if (fx != null) {
                float strength = introStrength;
                if (shock) {
                    strength += shockAmp * Mth.clamp((shockUntilMs - Util.getMillis()) / (float) shockDurMs, 0.0F, 1.0F);
                }
                float time = (Util.getMillis() % 1000000L) / 1000.0F;
                scratch.bindWrite(true);
                fx.safeGetUniform("ProjMat").set(ortho);
                fx.safeGetUniform("InSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("Strength").set(strength);
                fx.safeGetUniform("iTime").set(time);
                fx.setSampler("DiffuseSampler", main::getColorTextureId);
                fx.apply();
                drawQuad(main.width, main.height);
                fx.clear();
                copyBack(main, blit, ortho);
            }
        }

        if (finale) { // Pass C：万花凋零引力透镜扭曲（真实矩阵投影 + 深度剔除球体）
            EffectInstance fx = finaleShader(mc);
            if (fx != null) {
                float time = (Util.getMillis() % 1000000L) / 1000.0F;
                float frac = Mth.clamp((finaleUntilMs - Util.getMillis()) / (float) finaleDurMs, 0.0F, 1.0F);

                // 用事件提供的真实投影矩阵 × 自行重建的相机 view 矩阵（含相机平移）。
                // 注意：AFTER_LEVEL 的 event.getPoseStack() 是投影空间矩阵，不能当 view 用。
                Matrix4f projection = event.getProjectionMatrix();
                net.minecraft.world.phys.Vec3 camP = event.getCamera().getPosition();
                Matrix4f view = cn.blockforge.generated.floralritebloomcurios.client.shader.FRBShaderEffects
                        .cameraViewMatrix(event.getCamera())
                        .translate((float) -camP.x, (float) -camP.y, (float) -camP.z);
                Matrix4f viewProj = new Matrix4f(projection).mul(view);
                Matrix4f invViewProj = new Matrix4f(viewProj).invert();

                scratch.bindWrite(true);
                fx.safeGetUniform("ProjMat").set(ortho);
                fx.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("SceneSize").set((float) main.width, (float) main.height);
                fx.safeGetUniform("ViewProjection").set(viewProj);
                fx.safeGetUniform("InverseViewProjection").set(invViewProj);
                fx.safeGetUniform("CenterPos").set(FINALE_BOSS_POS.x, FINALE_BOSS_POS.y, FINALE_BOSS_POS.z);
                fx.safeGetUniform("CameraPos").set(
                        (float) event.getCamera().getPosition().x,
                        (float) event.getCamera().getPosition().y,
                        (float) event.getCamera().getPosition().z);
                fx.safeGetUniform("SphereRadius").set(25.0F);
                fx.safeGetUniform("iTime").set(time);
                fx.safeGetUniform("Visibility").set(frac);
                fx.setSampler("DiffuseSampler", main::getColorTextureId);
                fx.setSampler("DepthSampler", main::getDepthTextureId);
                fx.apply();
                drawQuad(main.width, main.height);
                fx.clear();
                copyBack(main, blit, ortho);
            }
        }
    }

    /** 用原版 blit 把临时目标拷回主目标。 */
    private static void copyBack(RenderTarget main, EffectInstance blit, Matrix4f ortho) {
        main.bindWrite(true);
        blit.safeGetUniform("ProjMat").set(ortho);
        blit.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
        blit.setSampler("DiffuseSampler", scratch::getColorTextureId);
        blit.apply();
        drawQuad(main.width, main.height);
        blit.clear();
    }

    private static void drawQuad(int width, int height) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(0.0D, 0.0D, 500.0D).endVertex();
        bb.vertex(0.0D, height, 500.0D).endVertex();
        bb.vertex(width, height, 500.0D).endVertex();
        bb.vertex(width, 0.0D, 500.0D).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    private static EffectInstance shockShader(Minecraft mc) {
        if (shockShader == null) {
            try {
                shockShader = new EffectInstance(mc.getResourceManager(),
                        FloralRiteBloom.MOD_ID + ":boss_shockwave");
            } catch (Exception e) {
                LOGGER.error("[FRB] boss_shockwave shader load failed, shockwave disabled", e);
                return null;
            }
        }
        return shockShader;
    }

    private static EffectInstance grayShader(Minecraft mc) {
        if (grayShader == null) {
            try {
                grayShader = new EffectInstance(mc.getResourceManager(),
                        FloralRiteBloom.MOD_ID + ":boss_gray");
            } catch (Exception e) {
                LOGGER.error("[FRB] boss_gray shader load failed, grayscale disabled", e);
                return null;
            }
        }
        return grayShader;
    }

    private static EffectInstance tintShader(Minecraft mc) {
        if (tintShader == null) {
            try {
                tintShader = new EffectInstance(mc.getResourceManager(),
                        FloralRiteBloom.MOD_ID + ":boss_tint");
            } catch (Exception e) {
                LOGGER.error("[FRB] boss_tint shader load failed, phase tint disabled", e);
                return null;
            }
        }
        return tintShader;
    }

    private static EffectInstance finaleShader(Minecraft mc) {
        if (finaleShader == null) {
            try {
                finaleShader = new EffectInstance(mc.getResourceManager(),
                        FloralRiteBloom.MOD_ID + ":boss_finale");
            } catch (Exception e) {
                LOGGER.error("[FRB] boss_finale shader load failed, mille florum finale disabled", e);
                return null;
            }
        }
        return finaleShader;
    }

    private static EffectInstance blitShader(Minecraft mc) {
        if (blitShader == null) {
            try {
                blitShader = new EffectInstance(mc.getResourceManager(), "minecraft:blit");
            } catch (Exception e) {
                LOGGER.error("[FRB] minecraft:blit shader load failed", e);
                return null;
            }
        }
        return blitShader;
    }

    private static boolean ensureScratch(int width, int height) {
        if (scratch != null && (scratch.width != width || scratch.height != height)) {
            scratch.destroyBuffers();
            scratch = null;
        }
        if (scratch == null) {
            try {
                scratch = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private FRBBossFxClient() {}
}
