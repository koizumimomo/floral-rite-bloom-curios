package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.client.EntityAnimation;
import cn.blockforge.generated.floralritebloomcurios.client.JellyAnimation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ⚠️【已停用 r112】⚠️
 * <p>
 * 与 {@link EntityRenderDispatcherMixin} 同属「喝酒 → 果冻变形」这条线，实测不生效，
 * 一并从 {@code mixins.fl_rite.json} 的 client 列表移除。<b>本类目前是死代码，不会被加载。</b>
 * <p>
 * 重新启用：把 {@code "CameraMixin"} 加回 {@code mixins.fl_rite.json} 的 {@code client} 数组。
 * <hr>
 * 喝酒后的【镜头】下沉（移植自 friendswine 的 CameraMixin）。
 * <p>
 * <b>为什么需要它</b>：第一人称看不到自己的模型，所以光靠
 * {@link EntityRenderDispatcherMixin} 的模型挤压，喝了酒的玩家自己毫无感觉。
 * 这里在相机定位之后按同样的 {@code heightScale} 把镜头往下压，
 * 让"变形"在第一人称下也成立。
 * <p>
 * 注入点是 {@code Camera.setup} 里<b>第一个</b> {@code setPosition(DDD)} 之后，
 * 此时相机位置已算好（含眼高插值），但尚未做射线修正，直接改 y 最干净。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** {@code Camera.setPosition} 是 protected，需要影子方法才能调用。 */
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Inject(method = "setup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setPosition(DDD)V",
                    ordinal = 0, shift = At.Shift.AFTER))
    private void frb$drunkCameraDip(BlockGetter level, Entity entity, boolean detached,
                                    boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        // 只处理本地玩家自己：别人的相机轮不到我们动
        if (!(entity instanceof Player player)) return;
        if (player != Minecraft.getInstance().player) return;
        if (!player.isAlive() || player.isSleeping()) return;

        EntityAnimation anim = EntityAnimation.forCamera(player, partialTick);
        if (anim == null) return;

        Camera self = (Camera) (Object) this;
        Vec3 camPos = self.getPosition();
        // 相机相对"玩家脚底插值位置"的高度（约等于眼高）
        double eyeHeight = camPos.y - Mth.lerp(partialTick, player.yo, player.getY());
        // 高度缩放成 0.5 时，镜头同步下降一半眼高；不压扁时 heightScale=1 → 位移 0
        double offset = eyeHeight
                * (JellyAnimation.heightScale(anim.squash(), JellyAnimation.COMPRESS) - 1.0D);
        setPosition(camPos.add(0.0D, offset, 0.0D));
    }
}
