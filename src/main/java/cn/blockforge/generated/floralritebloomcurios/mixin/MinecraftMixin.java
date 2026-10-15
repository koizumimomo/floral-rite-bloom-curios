package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.util.TimeStopState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间静止期间禁止玩家左右键行为（吃东西 / 攻击 / 用物品）。
 *
 * <p>服务端 {@link TimeStopInputLock} 已吞掉攻击 / 用物品事件，但客户端仍会播放
 * 吃东西 / 抬手动画。本 mixin 在 Minecraft 的左键 / 右键入口直接取消，让客户端
 * 也完全无法触发任何交互动作。</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /** 右键用物品（含吃东西、喝药、发射丰饶等）——时停期间直接取消。 */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void frb$blockUseItemDuringTimeStop(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        if (TimeStopState.isActive(mc.level)) ci.cancel();
    }

    /** 左键攻击起手——时停期间取消。 */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void frb$blockAttackDuringTimeStop(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        if (TimeStopState.isActive(mc.level)) ci.cancel();
    }

    /** 按住左键持续攻击——时停期间取消。 */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void frb$blockContinueAttackDuringTimeStop(boolean bl, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        if (TimeStopState.isActive(mc.level)) ci.cancel();
    }
}
