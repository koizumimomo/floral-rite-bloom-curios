package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r76：时间静止（time stop）期间玩家输入封锁。
 *
 * <p>用户截图反馈：之前能照常左键 / 右键 / 发射丰饶，说明 EntityMixin 只冻结了非玩家实体（玩家被加白名单），
 * 但没有拦截攻击 / 使用物品事件。本类在服务端用 Forge 事件总线拦住所有这些事件，
 * 时停中玩家无法造成伤害 / 使用物品 / 破坏方块 / 触发丰饶的右键持续散射。</p>
 *
 * <p>本类与 {@code LevelMixin}（实体 baseTick 冻结）、{@link TimeStopState}（维度标记）
 * 配套使用；共同保证时停期间画面静止 + 玩家输入无效 + 玩家位置锁回快照点。</p>
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeStopInputLock {

    /**
     * 时停期间玩家攻击直接被吞（不造成伤害）。
     * 用 HIGHEST 优先级拦截，确保原 mod 的其它处理逻辑也走不到。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!TimeStopState.isActive(attacker.level())) return;
        if (attacker.isCreative() || attacker.isSpectator()) return;
        event.setCanceled(true);
    }

    /** 时停期间右键触发 / 用物品事件直接吞（含持续散射的丰饶）。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!TimeStopState.isActive(event.getEntity().level())) return;
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) return;
        event.setCanceled(true);
    }

    /** 时停期间右键实体事件（attack 用）也被吞。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!TimeStopState.isActive(event.getEntity().level())) return;
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) return;
        event.setCanceled(true);
    }

    /** 时停期间右键方块（放物品 / 触发机关）也吞。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!TimeStopState.isActive(event.getEntity().level())) return;
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) return;
        event.setCanceled(true);
    }

    /** 时停期间左键挖方块也吞（默认永恒花园维度禁止挖方块，但其他维度仍生效）。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!TimeStopState.isActive(event.getPlayer().level())) return;
        if (event.getPlayer().isCreative() || event.getPlayer().isSpectator()) return;
        event.setCanceled(true);
    }

    private TimeStopInputLock() {}
}