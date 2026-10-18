package cn.blockforge.generated.floralritebloomcurios.abilities;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终极效果【终末花环】：每 180 秒可主动释放（按键 R）或在受到致死伤害时自动触发。
 * 以玩家自身为中心展开半径 32 格的祝福领域，领域内获得：
 * 生命恢复Ⅱ、伤害吸收Ⅴ、抗性提升Ⅳ、造成的最终伤害 +700%。不消耗任何物品。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class TerminusWreath {
    public static final long COOLDOWN_TICKS = 180L * 20L;
    public static final int DURATION_TICKS = 12 * 20;
    public static final double RADIUS = 32.0D;
    public static final int DOMAIN_COLOR = 0xf2d5ff;
    /** +700% = ×8。作用于玩家造成的全部伤害类型。 */
    public static final float DAMAGE_MULTIPLIER = 8.0F;

    private static final Map<UUID, Long> DOMAINS = new ConcurrentHashMap<>();

    /** 主动释放（R 键）。r96：返回是否真正触发成功（供服务端决定是否下发冷却 HUD）。 */
    public static boolean activate(ServerPlayer player) {
        if (!FRBState.wearing(player, FRBItems.ETERNAL_SPIRIT.get())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!FRBState.useCooldown(player, "WreathCd", level.getGameTime(), COOLDOWN_TICKS)) {
            player.displayClientMessage(Component
                    .translatable("frb.wreath.cd", FRBState.cooldownLeft(player, "WreathCd", level.getGameTime()))
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        trigger(player);
        return true;
    }

    /** 爆发主体：致死伤害自动触发时也会调用（调用方负责冷却与取消死亡）。 */
    public static void trigger(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        DOMAINS.put(player.getUUID(), level.getGameTime() + DURATION_TICKS);
        player.invulnerableTime = Math.max(player.invulnerableTime, 30);

        grantBuffs(player);

        level.playSound(null, player,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.WREATH_BURST.get(),
                SoundSource.MASTER, 3.0F, 0.7F);
        level.playSound(null, player, SoundEvents.BEACON_POWER_SELECT, SoundSource.MASTER, 2.0F, 1.0F);
        FRBNetwork.sendDomain(level, player, DOMAIN_COLOR);
        burstRing(level, player);
        player.displayClientMessage(Component.translatable("frb.wreath.fire")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** 领域内自身增益：生命恢复Ⅱ / 伤害吸收Ⅴ / 抗性提升Ⅳ。 */
    public static void grantBuffs(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 4, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 3, false, true));
    }

    /**
     * 领域内玩家造成的最终伤害 ×8（+700%）。
     * <p>
     * 放在 {@link LivingDamageEvent} 而不是攻击力属性：属性只影响近战/弹射物的物理伤害，
     * 法术（魔法伤害）和花灵的自定义伤害类型都不会经过它。LivingDamageEvent 在护甲、
     * 抗性、伤害吸收全部结算完之后、真正扣血之前触发，此时 amount 就是最终出伤，
     * 在这里乘倍率可以覆盖所有伤害类型。LOWEST 优先级保证它在其它增伤之后执行。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player) return; // 不放大自伤
        if (!isDomainActive(player)) return;
        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
    }

    public static boolean isDomainActive(ServerPlayer player) {
        Long until = DOMAINS.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    private static void burstRing(ServerLevel level, ServerPlayer center) {
        double x = center.getX(), y = center.getY() + 0.1D, z = center.getZ();
        for (int i = 0; i < 96; i++) {
            double a = i / 96.0D * Math.PI * 2.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    x + Math.cos(a) * RADIUS, y + 0.2D, z + Math.sin(a) * RADIUS,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0D, z, 60, 3.0D, 1.5D, 3.0D, 0.2D);
    }

    /**
     * 领域维持：每 tick 遍历在线玩家，而不是遍历维度。
     * <p>
     * 旧实现监听 {@link TickEvent.LevelTickEvent} 并调用 {@code level.getPlayerByUUID}，
     * 这个方法只在该维度内查找玩家：玩家在主世界展开领域后，下界 / 末地 / 永恒花园
     * 任意一个维度 tick 一次，就会因为"这个维度里没有该玩家"把领域条目删掉。
     * 领域实际存活不到 1 秒，{@link #isDomainActive} 一直返回 false，最终伤害倍率也就没生效。
     * 改用 {@link TickEvent.ServerTickEvent} + 全局玩家列表后，领域能正常维持 12 秒，
     * 也不受切换维度影响。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;
        for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
            Long until = DOMAINS.get(owner.getUUID());
            if (until == null) continue;
            long now = owner.serverLevel().getGameTime();
            if (until < now || !owner.isAlive() || owner.isRemoved()) {
                DOMAINS.remove(owner.getUUID());
                continue;
            }
            if (owner.tickCount % 40 == 0) {
                grantBuffs(owner);
                pulse(owner.serverLevel(), owner);
            }
        }
    }

    /** 离线时清掉领域条目，避免 DOMAINS 长期保留离线玩家。 */
    @SubscribeEvent
    public static void onLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        DOMAINS.remove(event.getEntity().getUUID());
    }

    /** 领域维持期的花雨演出。 */
    private static void pulse(ServerLevel level, ServerPlayer owner) {
        for (int i = 0; i < 20; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = 3.0D + level.getRandom().nextDouble() * (RADIUS - 3.0D);
            level.sendParticles(ParticleTypes.COMPOSTER,
                    owner.getX() + Math.cos(a) * r, owner.getY() + 2.0D + level.getRandom().nextDouble() * 2.0D,
                    owner.getZ() + Math.sin(a) * r, 2, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private TerminusWreath() {}
}
