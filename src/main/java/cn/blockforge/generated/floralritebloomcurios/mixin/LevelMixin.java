package cn.blockforge.generated.floralritebloomcurios.mixin;

import cn.blockforge.generated.floralritebloomcurios.entity.DeathFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import cn.blockforge.generated.floralritebloomcurios.util.TimeStopState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * r73 · L5 时间静止：暂停非玩家实体的 {@link Entity#baseTick()}。
 * <p>
 * 思路（仿梦幻终焉）：时停期间，领域内除了 boss、玩家、机制实体、死亡之花、boss 弹射物之外的一切实体
 * 都不再走 baseTick（自然衰减、出界伤害、流体伤害、传送门等都暂停）。位置则由
 * {@link FriliasEntity#tickTimeStop(net.minecraft.server.level.ServerLevel)} 每 tick 拉回。
 * <p>
 * 之所以挂在 baseTick 而不是 tick：baseTick 是 Entity 里少数 <b>具体方法</b>，而 tick 在 Entity 是
 * abstract 的；每个子类（LivingEntity / Projectile / ItemEntity / ...）都有自己 override 的 tick，
 * baseTick 是它们 <i>几乎</i> 都会调用的共同前置。这样挂一个 mixin 就能拦截"实体基类到子类 tick"这一段。
 * <p>
 * 副作用：非豁免实体的射速类 tick（{@link Projectile#tick()}）和物品实体 tick（{@code ItemEntity#tick()}）
 * 仍然会跑，但它们调用的 {@code baseTick()} 被拦截，而 aiSkill / 物品物理等具体行为依赖 baseTick 设置的
 * 状态——所以同样会基本冻结（玩家看到的就是「时停中的定格」）。
 *
 * @author r73 L5
 */
@Mixin(Entity.class)
public class LevelMixin {

    /**
     * 拦截 baseTick()：时停期间且不在豁免名单中就清零运动 + 取消方法。
     */
    @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
    private void frb$freezeIfTimeStopped(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (!TimeStopState.isActive(level)) return;

        if (isWhitelistEntity(self)) return;

        // 冻结体处理：清零运动 + 拉回快照点（与 boss tickTimeStop 保持一致）
        if (self instanceof LivingEntity le) {
            le.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            le.fallDistance = 0.0F;
        } else {
            self.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        net.minecraft.world.phys.Vec3 snap = TimeStopState.getFrozenPosition(self.getUUID());
        if (snap != null && self.distanceToSqr(snap) > 1.0E-4D) {
            self.teleportTo(snap.x, snap.y, snap.z);
        }
        ci.cancel();
    }

    /**
     * 时停豁免名单：
     * <ul>
     *   <li>boss 自身（要继续 tick 以维持时停维持 + 连招）</li>
     *   <li>所有玩家（创造/旁观本来就在 boss 的 {@code timeStopFreezable} 里跳过）</li>
     *   <li>机制实体（{@link MechanicEntity}，丰饶相关的世界树机制）</li>
     *   <li>死亡之花（{@link DeathFlowerEntity}）</li>
     *   <li>boss 召唤出来的弹射物（{@code getOwner() == boss}）</li>
     * </ul>
     */
    private static boolean isWhitelistEntity(Entity e) {
        if (e instanceof FriliasEntity) return true;
        if (e instanceof Player) return true;
        if (e instanceof MechanicEntity) return true;
        if (e instanceof DeathFlowerEntity) return true;
        if (e instanceof Projectile proj && proj.getOwner() instanceof FriliasEntity) return true;
        return false;
    }
}
