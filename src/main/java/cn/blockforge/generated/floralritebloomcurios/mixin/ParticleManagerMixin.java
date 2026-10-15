package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.util.TimeStopState;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * r73 · L5 时间静止：暂停粒子系统的 render（即「tick + draw」）。
 * <p>
 * Minecraft 1.20.1 中粒子没有专门的 {@code tick()} 方法，所有粒子的运动更新都在
 * {@link ParticleEngine#render(PoseStack, MultiBufferSource.BufferSource, LightTexture, Camera, float, Frustum)}
 * 中完成（先 {@code particle.tick()} + 死亡判定 + quad 重算，再画出来）。所以挂在 render HEAD 上
 * cancellable，等于「粒子不动 + 粒子不画」双重效果。
 * <p>
 * <b>为什么不挂 @Shadow level</b>：Mojang mappings 下 ParticleEngine 的 level 字段是 protected ClientLevel，
 * 反混淆时名字会变；用 Minecraft.getInstance().level 兜底更跨版本稳定（反正本 mixin 只在 client 加载）。
 * <p>
 * <b>仿梦幻终焉</b>：梦幻终焉的同名 mixin 在 render 上拦截，但他们额外还挂了 reload 上（preload
 * 替换他们自定义的 {@code CustomInLevelRendererDispatcher}）。我们只做 render 这一段。
 *
 * @author r73 L5
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    /** r73：时停期间取消粒子渲染（不画）。 */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void frb$pauseIfTimeStopped(PoseStack poseStack,
                                       MultiBufferSource.BufferSource bufferSource,
                                       LightTexture lightTexture,
                                       Camera camera,
                                       float partialTick,
                                       Frustum frustum,
                                       CallbackInfo ci) {
        net.minecraft.world.level.Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        if (!TimeStopState.isActive(clientLevel)) return;
        ci.cancel();
    }

    /**
     * r101：时停期间还要取消粒子 tick（位置更新）。
     * 1.20.1 中 {@link ParticleEngine#tick()} 独立于 render 存在，
     * 仅 cancel render 会导致粒子仍在位移（只是不画），时停结束后粒子"瞬移"。
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void frb$freezeParticleTick(CallbackInfo ci) {
        net.minecraft.world.level.Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        if (!TimeStopState.isActive(clientLevel)) return;
        ci.cancel();
    }
}