package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.abilities.TerminusWreath;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import cn.blockforge.generated.floralritebloomcurios.util.TotemTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;

/**
 * 七花饰品与终极饰品的伤害相关效果：
 * 攻击侧（花瓣印记 / 冻结 / 点燃 / 凋零斩杀）、受击侧（闪避 / 快照复活 /
 * 免疫环境伤害 / 七花共鸣 / 终末花环自动触发）、击杀侧（焚生火花 / 焚生回血协同）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class CharmEvents {

    /** 原版 #minecraft:undead 实体标签（1.20.1 的 MobCategory 没有 UNDEAD 分类）。 */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> UNDEAD_TAG =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    new net.minecraft.resources.ResourceLocation("minecraft", "undead"));

    /** 终极饰品的环境伤害免疫清单（伤害消息 id）。 */
    private static final Set<String> ENV_DAMAGE = Set.of(
            "inFire", "onFire", "lava", "hotFloor", "fall", "drown", "inWall", "cactus",
            "sweetBerry", "stalactite", "flyIntoWall", "freeze", "powderSnow", "poison", "wither");

    private static Item charm(CharmType t) {
        return FRBItems.charmOf(t);
    }

    /**
     * 万花万草之灵：免疫所有原版负面效果（中毒、凋零、缓慢、失明、黑暗、挖掘缓慢等）。
     * 通过拦截 MobEffectEvent.Applicable，对 minecraft 命名空间下的 HARMFUL 效果直接 DENY。
     */
    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!FRBState.hasUltimate(player)) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        net.minecraft.world.effect.MobEffect effect = inst.getEffect();
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id == null) return;
        if (!"minecraft".equals(id.getNamespace())) return; // 只拦原版效果，FRB 自身增益不拦
        if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    // ---------------- 攻击侧 ----------------

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) return;

        // 焚生藤：点燃 + 花瓣印记，第 4 层印记后的下一次攻击引爆
        if (FRBState.active(player, CharmType.EMBER_VINE)) {
            MobEffectInstance mark = target.getEffect(FRBEffects.PETAL_MARK.get());
            int stacks = mark == null ? 0 : mark.getAmplifier() + 1;
            if (stacks >= 4) {
                target.removeEffect(FRBEffects.PETAL_MARK.get());
                float bonus = 8.0F + target.getMaxHealth() * 0.04F;
                target.hurt(level.damageSources().playerAttack(player), bonus);
                target.setSecondsOnFire(6);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0D,
                        target.getZ(), 35, 0.5D, 0.8D, 0.5D, 0.08D);
                level.sendParticles(FRBParticles.petal(CharmType.EMBER_VINE), target.getX(),
                        target.getY() + 1.2D, target.getZ(), 25, 0.6D, 0.8D, 0.6D, 0.06D);
                level.playSound(null, target, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.0F, 1.6F);
            } else {
                target.addEffect(new MobEffectInstance(FRBEffects.PETAL_MARK.get(), 200, stacks, false, true));
                target.setSecondsOnFire(3);
            }
        }

        // 寒风铃：命中附加霜蚀
        if (FRBState.active(player, CharmType.FROST_CHIME)) {
            target.addEffect(new MobEffectInstance(FRBEffects.FROSTBITE.get(), 60, 0));
        }

    }

    // ---------------- 受击侧 ----------------

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim instanceof ServerPlayer player)) return;
        DamageSource src = event.getSource();
        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        ServerLevel level = player.serverLevel();

        // 星界花：免疫摔落与虚空伤害
        if (FRBState.active(player, CharmType.ASTRAL_BLOOM)
                && (src.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                    || src.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD))) {
            event.setCanceled(true);
            return;
        }

        // 万花万草之灵：免疫环境伤害
        if (FRBState.hasUltimate(player) && ENV_DAMAGE.contains(src.getMsgId())) {
            event.setCanceled(true);
            return;
        }

        // 冰之华协同（寒风铃 + 天泪花）：保存承受的伤害，稍后爆发治疗
        if (FRBState.active(player, CharmType.FROST_CHIME)
                && FRBState.active(player, CharmType.SKY_TEAR)) {
            float stored = Math.min(40.0F,
                    FRBState.getFloat(player, "IceStored", 0.0F) + amount * 0.5F);
            FRBState.setFloat(player, "IceStored", stored);
        }

        // 致死伤害处理顺序：不死图腾 > 寒风铃快照 → 七花共鸣 → 终末花环
        // r112：LivingAttackEvent 的 amount 是「未扣护甲/抗性/吸收心」的原始值，
        //      直接拿它和当前血量比会把"实际打不死"的伤害误判成致死（例如玩家 5 HP、
        //      原始伤害 6、护甲减掉 4 → 实际只掉 2，本来不会死）。这里按原版公式
        //      估算「实际扣血量」后再判定是否真的会死。
        if (wouldActuallyKill(player, src, amount) && !player.getAbilities().invulnerable) {
            // r112：不死图腾优先——手持图腾时一律放行，交还原版 Player#die → checkTotemDeathProtection
            //      消费图腾。之前这里抢先 cancel 伤害并 setHealth，玩家不会进入死亡流程，
            //      图腾永远不触发（玩家反馈"图腾失效"）。
            if (TotemTools.hasTotem(player)) {
                return;
            }
            float snapshot = FRBState.getFloat(player, "Snapshot", 0.0F);
            if (FRBState.active(player, CharmType.FROST_CHIME) && snapshot > 0.5F
                    && FRBState.useCooldown(player, "ReviveCd", level.getGameTime(), 1200L)) {
                event.setCanceled(true);
                player.setHealth(Math.min(snapshot, player.getMaxHealth()));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 2));
                freezeNearby(level, player);
                reviveFx(level, player, CharmType.FROST_CHIME, "frb.revive.snapshot");
                return;
            }
            if (FRBState.hasAllSeven(player)
                    && FRBState.useCooldown(player, "ResonanceCd", level.getGameTime(), 1200L)) {
                event.setCanceled(true);
                player.setHealth(Math.max(4.0F, player.getMaxHealth() * 0.2F));
                for (MobEffectInstance inst : new ArrayList<>(player.getActiveEffects())) {
                    if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                        player.removeEffect(inst.getEffect());
                    }
                }
                reviveFx(level, player, null, "frb.revive.resonance");
                return;
            }
            if (FRBState.hasUltimate(player)
                    && FRBState.useCooldown(player, "WreathCd", level.getGameTime(),
                    TerminusWreath.COOLDOWN_TICKS)) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                TerminusWreath.trigger(player);
            }
        }
    }

    // ---------------- 伤害数值修正 ----------------

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();

        // 星界花·群星的彼岸：对末影人增伤 50%，受末影人伤害减半
        if (victim instanceof EnderMan && src.getEntity() instanceof ServerPlayer p
                && FRBState.active(p, CharmType.ASTRAL_BLOOM)) {
            event.setAmount(event.getAmount() * 1.5F);
        }
        // 黑灵草·凋亡之气息：对亡灵生物增伤 50%
        if (src.getEntity() instanceof ServerPlayer p && p != victim
                && victim.getType().is(UNDEAD_TAG)
                && FRBState.active(p, CharmType.BLACK_SPIRIT)) {
            event.setAmount(event.getAmount() * 1.5F);
        }
        if (victim instanceof ServerPlayer player) {
            if (src.getEntity() instanceof EnderMan && FRBState.active(player, CharmType.ASTRAL_BLOOM)) {
                event.setAmount(event.getAmount() * 0.5F);
            }
            // 焚生藤·尽燃之烈焰：熔岩与火焰伤害无效（可在熔岩上行走）
            if (FRBState.active(player, CharmType.EMBER_VINE)) {
                String id = src.getMsgId();
                if (id.equals("lava") || id.equals("inFire") || id.equals("onFire")
                        || id.equals("hotFloor")) {
                    event.setAmount(0.0F);
                }
            }
            // E3 潮湿：受到落雷伤害 ×1.5（云雨阶段落雷机制可叠加此效果让命中区更危险）
            if (src.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)
                    && player.hasEffect(FRBEffects.DAMPNESS.get())) {
                event.setAmount(event.getAmount() * 1.5F);
            }
            // r96 浸泡（万变水波）：受到的所有伤害 ×1.25
            if (player.hasEffect(FRBEffects.SOGGY.get())) {
                event.setAmount(event.getAmount() * cn.blockforge.generated.floralritebloomcurios.effect.SoggyEffect.DAMAGE_MULTIPLIER);
            }
        }
    }

    /**
     * r112：按原版伤害公式估算「扣完护甲 / 抗性 / 吸收心后玩家是否真的会死」。
     * 详见 {@link cn.blockforge.generated.floralritebloomcurios.util.TotemTools#wouldActuallyKill}。
     */
    private static boolean wouldActuallyKill(ServerPlayer player, DamageSource src, float rawAmount) {
        return TotemTools.wouldActuallyKill(player, src, rawAmount);
    }

    /** 寒风铃复活时冰封四周：8 格内敌对生物冻结 + 强减速。 */
    private static void freezeNearby(ServerLevel level, ServerPlayer player) {
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8.0D),
                e -> e != player && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Enemy)) {
            near.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, true));
            near.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2, false, true));
            level.sendParticles(ParticleTypes.SNOWFLAKE, near.getX(), near.getY() + 1.0D,
                    near.getZ(), 18, 0.4D, 0.7D, 0.4D, 0.0D);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.2D,
                player.getZ(), 40, 3.0D, 1.0D, 3.0D, 0.0D);
    }

    private static void reviveFx(ServerLevel level, ServerPlayer player, CharmType t, String key) {
        level.playSound(null, player, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6F, 1.1F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D,
                player.getZ(), 50, 0.7D, 1.2D, 0.7D, 0.15D);
        if (t != null) {
            level.sendParticles(FRBParticles.petal(t), player.getX(), player.getY() + 1.4D,
                    player.getZ(), 30, 0.6D, 0.9D, 0.6D, 0.05D);
        }
        player.displayClientMessage(Component.translatable(key)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------- 击杀侧 ----------------

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        LivingEntity dead = event.getEntity();

        // 焚生藤：击杀火花，点燃附近敌人
        if (FRBState.active(player, CharmType.EMBER_VINE)) {
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(4.0D), e -> e != dead && e.isAlive()
                            && e instanceof net.minecraft.world.entity.monster.Enemy)) {
                near.setSecondsOnFire(4);
            }
            level.sendParticles(ParticleTypes.SMOKE, dead.getX(), dead.getY() + 0.5D, dead.getZ(),
                    12, 0.4D, 0.6D, 0.4D, 0.02D);
        }

        // 焚尽 + 自然（焚生藤 + 森语藤）协同：焚生回血并催生附近作物
        if (FRBState.active(player, CharmType.EMBER_VINE)
                && FRBState.active(player, CharmType.FOREST_WHISPER)) {
            player.heal(2.0F);
            if (FRBState.growthEnabled(player)) {
                GrowthBoost.boost(level, player.blockPosition(), 8, 6);
            }
        }
    }

    private CharmEvents() {}
}
