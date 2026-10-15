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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终极效果【终末花环】：每 180 秒可主动释放（按键 R）或在受到致死伤害时自动触发。
 * 以玩家自身为中心展开半径 32 格的祝福领域，领域内获得：
 * 生命恢复Ⅱ、伤害吸收Ⅴ、抗性提升Ⅳ、攻击伤害 +700%。不消耗任何物品。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class TerminusWreath {
    public static final long COOLDOWN_TICKS = 180L * 20L;
    public static final int DURATION_TICKS = 12 * 20;
    public static final double RADIUS = 32.0D;
    public static final int DOMAIN_COLOR = 0xf2d5ff;

    private static final UUID ATTACK_BONUS_UUID = UUID.nameUUIDFromBytes(
            "floral_rite_bloom_curios:wreath_attack".getBytes(StandardCharsets.UTF_8));

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
        applyAttackBonus(player);

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

    /** 攻击伤害 +700%（临时修饰符，领域结束即收回）。 */
    private static void applyAttackBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(ATTACK_BONUS_UUID);
            attr.addTransientModifier(new AttributeModifier(ATTACK_BONUS_UUID,
                    "frb_wreath_attack", 7.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void removeAttackBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(ATTACK_BONUS_UUID);
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

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        DOMAINS.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                if (level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer owner) {
                    removeAttackBonus(owner);
                }
                return true;
            }
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer owner) || !owner.isAlive()) {
                return true;
            }
            if (owner.tickCount % 40 == 0) {
                grantBuffs(owner);
                pulse(level, owner);
            }
            return false;
        });
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
