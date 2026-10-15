package cn.blockforge.generated.floralritebloomcurios.client.shader;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端活跃的世界空间后处理效果列表。
 * 服务端通过 ShaderEffectPacket 下发；RenderLevelStageEvent.AFTER_LEVEL 逐个应用。
 *
 * <p>实现方式与 {@code FRBBossFxClient} 同源：直接用 {@link EffectInstance} + 临时渲染目标
 * + 原版 blit 回写。**不使用 PostChain**——PostChain 的 passes/尺寸字段在正式环境是 SRG 名
 * （f_110009_ 等），反射开发名会抛 NoSuchFieldException 导致主 shader 取不到、效果静默失效
 * （r102~r105 波纹扩散一直不生效的根因）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class FRBShaderEffects {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static final List<ShaderEffect> EFFECTS = new ArrayList<>();

    private static EffectInstance shockShader;
    private static EffectInstance wormholeShader;
    private static EffectInstance blitShader;
    private static TextureTarget scratch;

    /** 客户端直接添加一个效果（由 ShaderEffectPacket 处理函数调用）。 */
    public static void add(ResourceKey<Level> dimension, String name, Vec3 pos, int lifeTime, float scale) {
        EFFECTS.add(new ShaderEffect(dimension, name, pos, lifeTime, scale));
        LOGGER.info(
                "FRB shader effect added: name={}, pos=({},{},{}), life={}, dim={}",
                name, pos.x, pos.y, pos.z, lifeTime, dimension.location());
    }

    /** 每游戏 tick 推进一次效果计时并清理过期项（iTime 与生命周期均以 tick 为单位）。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EFFECTS.removeIf(e -> e.tickCount++ >= e.lifeTime);
    }

    /** 资源包重载：丢弃已编译 shader，下次渲染时重新编译。 */
    public static void invalidateShaders() {
        if (shockShader != null) {
            shockShader.close();
            shockShader = null;
        }
        if (wormholeShader != null) {
            wormholeShader.close();
            wormholeShader = null;
        }
        if (blitShader != null) {
            blitShader.close();
            blitShader = null;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (EFFECTS.isEmpty()) return;
        applyAll(event.getProjectionMatrix(), event.getPartialTick(), event.getCamera());
    }

    /**
     * 世界渲染后逐个应用所有活跃效果。
     *
     * <p><b>注意</b>：{@code RenderLevelStageEvent.AFTER_LEVEL} 由 {@code GameRenderer} 派发，
     * 其 {@code getPoseStack()} 携带的是 <b>投影空间</b> 矩阵（已经把 projection 乘进去），
     * 并不是相机 view 矩阵——直接拿它当 ViewMatrix 会让 raymarching 射线完全错误、视觉上"毫无扭曲"。
     * 真正的 view 矩阵（相机旋转，不带平移）在 {@code LevelRenderer} 内部的 stage 才会传出来。
     * 这里按 {@code GameRenderer} 的构造顺序自行重建：{@code Rx(xRot) · Ry(yRot + 180)}（roll 视为 0）。
     */
    public static void applyAll(Matrix4f projection, float partialTick, Camera camera) {
        if (EFFECTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        EffectInstance blit = blitShader(mc);
        if (blit == null || !ensureScratch(main.width, main.height)) return;
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, main.width, main.height, 0.0F, 0.1F, 1000.0F);
        Vec3 camPos = camera.getPosition();
        Matrix4f view = cameraViewMatrix(camera);
        RenderSystem.depthMask(false);
        // 快照遍历，避免 add/remove 并发
        for (ShaderEffect t : new ArrayList<>(EFFECTS)) {
            if (!mc.level.dimension().equals(t.dimension)) continue;
            Vec3 pos = t.pos.subtract(camPos);
            // 相机空间（旋转 + 平移到 boss 原点）：view · T(pos)
            Matrix4f viewMat = new Matrix4f(view).translate((float) pos.x, (float) pos.y, (float) pos.z);
            if (t.name.equals("Shockwave")) {
                applyShockwave(mc, main, blit, ortho, projection, viewMat, t.tickCount);
            } else if (t.name.equals("Wormhole")) {
                applyWormhole(mc, main, blit, ortho, projection, viewMat, partialTick);
            }
        }
        RenderSystem.depthMask(true);
    }

    /** 相机 view 矩阵（世界 → 相机空间，仅旋转）。与 GameRenderer#renderLevel 中 view 的构造一致。 */
    public static Matrix4f cameraViewMatrix(Camera camera) {
        return new Matrix4f()
                .rotate(Axis.XP.rotationDegrees(camera.getXRot()))
                .rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
    }

    /** 世界空间波纹扩散冲击波（水母同款 raymarching 球壳）。 */
    private static void applyShockwave(Minecraft mc, RenderTarget main, EffectInstance blit, Matrix4f ortho,
                                       Matrix4f projection, Matrix4f viewMat, int tickCount) {
        if (shockShader == null) {
            shockShader = loadShader(mc, "frb_shockwave");
        }
        EffectInstance fx = shockShader;
        if (fx == null) return;
        scratch.bindWrite(true);
        fx.safeGetUniform("iResolution").set(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        fx.safeGetUniform("InverseTransformMatrix").set(getInverseTransformMatrix(projection, viewMat));
        fx.safeGetUniform("ProjectionMat").set(projection);
        fx.safeGetUniform("ViewMat").set(viewMat);
        fx.safeGetUniform("iTime").set(tickCount / 20.0F);
        fx.setSampler("DiffuseSampler", main::getColorTextureId);
        fx.setSampler("DepthSampler", main::getDepthTextureId);
        fx.apply();
        drawQuad(main.width, main.height);
        fx.clear();
        copyBack(main, blit, ortho);
    }

    /** 虫洞镜头变形（Bosses-Unleashed 移植）：球壳引力透镜 + 体积星光隧道。 */
    private static void applyWormhole(Minecraft mc, RenderTarget main, EffectInstance blit, Matrix4f ortho,
                                      Matrix4f projection, Matrix4f viewMat, float partialTick) {
        if (wormholeShader == null) {
            wormholeShader = loadShader(mc, "frb_wormhole");
        }
        EffectInstance fx = wormholeShader;
        if (fx == null) return;
        scratch.bindWrite(true);
        fx.safeGetUniform("iResolution").set(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        fx.safeGetUniform("InverseTransformMatrix").set(getInverseTransformMatrix(projection, viewMat));
        fx.safeGetUniform("ProjectionMatrix").set(projection);
        fx.safeGetUniform("ViewMatrix").set(viewMat);
        fx.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
        long gameTime = mc.level == null ? 0L : mc.level.getGameTime();
        fx.safeGetUniform("iTime").set((gameTime % 2400000L + partialTick) / 20.0F);
        fx.setSampler("DiffuseSampler", main::getColorTextureId);
        fx.setSampler("DepthSampler", main::getDepthTextureId);
        fx.apply();
        drawQuad(main.width, main.height);
        fx.clear();
        copyBack(main, blit, ortho);
    }

    private static EffectInstance loadShader(Minecraft mc, String name) {
        try {
            return new EffectInstance(mc.getResourceManager(), FloralRiteBloom.MOD_ID + ":" + name);
        } catch (Exception e) {
            LOGGER.error("[FRB] {} shader load failed", name, e);
            return null;
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

    private static Matrix4f getInverseTransformMatrix(Matrix4f projection, Matrix4f modelView) {
        return new Matrix4f(projection).mul(modelView).invert();
    }

    public static class ShaderEffect {
        public final ResourceKey<Level> dimension;
        public final String name;
        public final Vec3 pos;
        public final int lifeTime;
        public final float scale;
        public int tickCount;

        public ShaderEffect(ResourceKey<Level> dimension, String name, Vec3 pos, int lifeTime, float scale) {
            this.dimension = dimension;
            this.name = name;
            this.pos = pos;
            this.lifeTime = lifeTime;
            this.scale = scale;
        }
    }

    private FRBShaderEffects() {}
}