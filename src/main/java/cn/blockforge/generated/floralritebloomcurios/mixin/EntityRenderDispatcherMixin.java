package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.client.EntityAnimation;
import cn.blockforge.generated.floralritebloomcurios.client.JellyAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ⚠️【已停用 r112】⚠️
 * <p>
 * 酒 → 果冻变形 / 旋转这条线在 r110 实测仍然完全不生效（注入体一次都没执行，
 * 换用了 friendswine 那套已验证可用的注入写法之后依然如此，根因未查明）。
 * 已从 {@code mixins.fl_rite.json} 的 client 列表移除，<b>本类目前是死代码，不会被加载</b>。
 * <p>
 * 重新启用：把 {@code "EntityRenderDispatcherMixin"} 加回 {@code mixins.fl_rite.json}
 * 的 {@code client} 数组即可，代码本身是完整的。
 * <p>
 * 替代品备注：本项目已弃用 {@code LivingEntityRenderer} 注入点，改用
 * {@code EntityRenderDispatcher}（理由见下）。
 * <hr>
 * 果冻回弹 + 旋转：持有「喝醉了」效果（或处在醉酒玩家光环半径内）的生物，
 * 渲染时被挤压回弹并绕 Y 轴旋转。
 * <p>
 * r110：改为移植 friendswine 那套<b>已验证可用</b>的注入方式。与旧写法的三点区别：
 * <ol>
 *   <li>目标是 {@link EntityRenderDispatcher#render}，不是 {@code LivingEntityRenderer#render}。
 *       注入点在实体完成 {@code poseStack.translate(x, y, z)} <b>之后</b>，所以姿态栈原点
 *       已经在实体脚下，缩放/旋转就地生效，且对所有渲染器（含 GeckoLib）一视同仁。</li>
 *   <li>{@code method = "render"} <b>不写描述符</b>，交给 Mixin 按名字解析。</li>
 *   <li>push/pop <b>无条件</b>成对执行，真正的"要不要动画"判定只在中间求值一次。
 *       旧写法在 HEAD 和 RETURN 各判一次 {@code hasEffect}，两帧之间效果过期就会姿态栈失衡。</li>
 * </ol>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    /** 实体在 {@code render} 里完成定位平移之后：压栈 + 施加变形。 */
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 0, shift = At.Shift.AFTER))
    private void frb$jellyBegin(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        poseStack.pushPose();
        if (entity instanceof LivingEntity living) {
            EntityAnimation anim = EntityAnimation.forEntity(living, partialTick);
            if (anim != null) {
                if (anim.rotating()) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(JellyAnimation.angle(anim.seconds())));
                }
                float squash = anim.squash();
                float ws = JellyAnimation.widthScale(squash, JellyAnimation.WIDTH);
                float hs = JellyAnimation.heightScale(squash, JellyAnimation.COMPRESS);
                poseStack.scale(ws, hs, ws);
            }
        }
    }

    /** 实体渲染收尾处（火焰动画之前）：无条件出栈，与 {@link #frb$jellyBegin} 严格配对。 */
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
    private void frb$jellyEnd(Entity entity, double x, double y, double z, float yRot, float partialTick,
                              PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        poseStack.popPose();
    }
}
