package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.util.TimeStopState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * r76：时间静止期间客户端玩家输入冻结。
 *
 * <p>用户在时停里仍能左键 / 右键 / 移动 / 发射丰饶，说明客户端 input 没被屏蔽。
 * {@code TimeStopInputLock} 只在服务端拦截了攻击 / 用物品事件，但本地 input
 * 已经在客户端触发，传送给服务端才被吞——视觉上仍能看到玩家"动了"。</p>
 *
 * <p>本 mixin 在 LocalPlayer.aiStep 头部清空 input.forwardImpulse / leftImpulse / jumping /
 * shiftKeyDown，并把 deltaMovement 直接清零，让玩家在时停期间看起来"完全冻结"。
 * 服务端的 {@link TimeStopInputLock} 仍然负责：即便绕过客户端，服务端也照常拒绝。</p>
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /**
     * aiStep 头部：时停期间强制清零输入 + 速度，再让 aiStep 正常处理（避免一击触发多种效果）。
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void frb$freezeIfTimeStopped(CallbackInfo ci) {
        if (!TimeStopState.isActive(Minecraft.getInstance().level)) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.isCreative() || self.isSpectator()) return;

        // 1) 清零 movement 输入（Minecraft 1.20.1 LocalPlayer.input 是 public 字段）
        self.input.forwardImpulse = 0.0F;
        self.input.leftImpulse = 0.0F;
        self.input.jumping = false;
        self.input.shiftKeyDown = false;

        // 2) 直接清零水平 / 垂直速度；fallDistance 重置避免跳跃触发持续伤害
        self.setDeltaMovement(Vec3.ZERO);
        self.fallDistance = 0.0F;

        // 3) 阻止按住的左键触发持续 attack 累积（AttackCooldown 等 tick 计数挂起）
        //    Mojang 的 LocalPlayer 没有暴露 attackCooldown 字段，由服务端事件兜底。

        // 4) r101：时停前已按住右键持续使用的物品（如丰饶连发）必须强制停止，
        //    否则 onUseTick 每 tick 仍在生成射弹，时停结束后一次性飞出造成暴毙。
        if (self.isUsingItem()) {
            self.stopUsingItem();
        }
    }
}