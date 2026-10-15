package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.abilities.FrostChimeRecall;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;

/**
 * 佩戴中的周期性效果：寒风铃生命快照、天泪花（灭火 / 水下呼吸夜视 / 水上行走 / 每2秒修复耐久）、
 * 焚生藤熔岩行走、森语藤静止催熟、黑灵草黑死病光环、冰之华协同、
 * 万花万草之灵的永久增益与飞行。判定一律走 FRBState.active()（含 Boss 封印与终极聚合）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class CharmTickEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        int tc = player.tickCount;
        cn.blockforge.generated.floralritebloomcurios.effect.CharmBuffs.tick(player);

        // 寒风铃：旧的致死复活快照 + 主动状态回溯会话。
        if (tc % 400 == 0 && FRBState.active(player, CharmType.FROST_CHIME)) {
            FRBState.setFloat(player, "Snapshot",
                    Math.min(player.getHealth(), player.getMaxHealth()));
        }
        FrostChimeRecall.tick(player);

        // 天泪花：灭火 / 水下呼吸与夜视 / 水上行走 / 周期耐久修复
        if (FRBState.active(player, CharmType.SKY_TEAR)) {
            if (player.isOnFire()) {
                player.clearFire();
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D,
                        player.getZ(), 6, 0.4D, 0.7D, 0.4D, 0.01D);
            }
            // 水上行走（潜行可入水）
            if (!player.isCrouching() && player.isInWater()
                    && level.getFluidState(player.blockPosition()).is(FluidTags.WATER)) {
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x, Math.max(m.y, 0.08D), m.z);
                player.setSwimming(false);
            }
            // 雨中 / 水中每 3 秒治疗并净化一个负面效果
            if (tc % 60 == 0) {
                boolean aqueous = level.isRaining() || player.isUnderWater()
                        || player.isInWaterRainOrBubble();
                if (aqueous) {
                    player.heal(1.0F);
                    for (MobEffectInstance inst : new ArrayList<>(player.getActiveEffects())) {
                        if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                            player.removeEffect(inst.getEffect());
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(),
                                    player.getY() + 1.4D, player.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.05D);
                            break;
                        }
                    }
                }
            }
            // 每 2 秒为物品栏及盔甲栏的物品回复 (1 + [群花]饰品数量) 点耐久
            if (tc % 40 == 0) {
                int amount = FRBState.repairAmount(player);
                var inv = player.getInventory();
                boolean repaired = false;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
                        stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
                        repaired = true;
                    }
                }
                for (int i = 0; i < inv.armor.size(); i++) {
                    ItemStack stack = inv.armor.get(i);
                    if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
                        stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
                        repaired = true;
                    }
                }
                if (repaired && tc % 80 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.5D,
                            player.getZ(), 4, 0.5D, 0.4D, 0.5D, 0.0D);
                }
            }
        }

        // 焚生藤：熔岩行走（潜行可沉入）+ 脚边火苗
        if (FRBState.active(player, CharmType.EMBER_VINE) && !player.isCrouching()
                && level.getFluidState(player.blockPosition()).is(FluidTags.LAVA)) {
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, Math.max(m.y, 0.12D), m.z);
            if (tc % 10 == 0) {
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.1D,
                        player.getZ(), 2, 0.4D, 0.1D, 0.4D, 0.0D);
            }
        }

        // 天穹花二段跳：落地后允许下一次空中跳跃；离地几 tick 后才接受请求，
        // 避免第一次起跳的同步包被误判为二段跳。
        if (player.onGround()) {
            player.getPersistentData().remove(FRBState.TAG_DOUBLE_JUMP_USED);
            player.getPersistentData().remove(FRBState.TAG_AIR_TICKS);
        } else {
            var data = player.getPersistentData();
            data.putInt(FRBState.TAG_AIR_TICKS, data.getInt(FRBState.TAG_AIR_TICKS) + 1);
        }

        // 森语藤：站立不动且开关开启时催熟自身周围作物（约每秒一轮）
        if (tc % 20 == 0 && FRBState.growthEnabled(player)
                && FRBState.active(player, CharmType.FOREST_WHISPER)) {
            Vec3 vel = player.getDeltaMovement();
            if (vel.horizontalDistanceSqr() < 0.002D && player.onGround()) {
                GrowthBoost.boost(level, player.blockPosition(), 6, 2);
            }
        }

        // 黑灵草：黑死病光环 —— 周围敌对生物持续生命流失
        // r77：shift + 右键切换是否启用（通用 Toggleable 接口实现见 CharmTools.enable_black_spirit_aura）；
        //      黑灵草默认开启，玩家可关。
        if (tc % 30 == 0 && FRBState.active(player, CharmType.BLACK_SPIRIT)
                && cn.blockforge.generated.floralritebloomcurios.charm.CharmTools
                .isAuraEnabled(player, CharmType.BLACK_SPIRIT)) {
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(6.0D), e -> e != player && e.isAlive()
                            && e instanceof Enemy)) {
                near.addEffect(new MobEffectInstance(FRBEffects.BLACK_DEATH.get(), 45, 0, true, false));
            }
            if (tc % 60 == 0) {
                level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 0.2D,
                        player.getZ(), 3, 2.0D, 0.4D, 2.0D, 0.0D);
            }
        }

        // 冰之华协同：储存的伤害每 8 秒爆发为治疗
        if (tc % 160 == 0
                && FRBState.active(player, CharmType.FROST_CHIME)
                && FRBState.active(player, CharmType.SKY_TEAR)) {
            float stored = FRBState.getFloat(player, "IceStored", 0.0F);
            if (stored >= 6.0F) {
                player.heal(Math.min(stored * 0.7F, 10.0F));
                FRBState.setFloat(player, "IceStored", 0.0F);
                level.sendParticles(FRBParticles.petal(CharmType.FROST_CHIME), player.getX(),
                        player.getY() + 1.5D, player.getZ(), 26, 0.6D, 0.8D, 0.6D, 0.05D);
                level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D,
                        player.getZ(), 12, 0.5D, 0.8D, 0.5D, 0.02D);
            }
        }

        // 万花万草之灵：永久夜视、水下呼吸、飞行
        if (tc % 40 == 0 && FRBState.hasUltimate(player)) {
            if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().flying
                    && !player.getAbilities().mayfly) {
                // 仅在权限真的未开放时下发能力更新包，避免每 2 秒一次的无谓同步
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (tc % 40 == 0 && !FRBState.hasUltimate(player)
                && !player.isCreative() && !player.isSpectator()
                && player.getAbilities().mayfly) {
            // 卸下终极饰品后收回飞行权限
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    /**
     * 手持森语藤蹲下右键切换周围催熟；手持寒风铃蹲下右键切换超时是否自动回溯；
     * 终极饰品：主手蹲下右键同样切换催熟（聚合的森语藤），副手蹲下右键切换寒风铃的传送回溯。
     */
    @SubscribeEvent
    public static void onGrowthToggle(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isCrouching()) return;
        ItemStack held = event.getItemStack();
        // r78：先匹配手持有具体功能的"扩展槽位"饰品（黑灵草/森语藤/寒风铃），
        // 否则 ETERNAL_SPIRIT 的"聚合"判断会错误吃中其它分支。
        if (held.is(FRBItems.charmOf(CharmType.BLACK_SPIRIT))) {
            // 黑灵草·shift + 右键切换黑死病光环开关（通用 CharmTools 接口）
            boolean enabled = cn.blockforge.generated.floralritebloomcurios.charm.CharmTools
                    .toggleAura(player, CharmType.BLACK_SPIRIT);
            player.displayClientMessage(Component.translatable(
                    enabled ? "frb.black_spirit.aura_on" : "frb.black_spirit.aura_off"), true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().is(FRBItems.ETERNAL_SPIRIT.get())
                && event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND) {
            boolean enabled = FRBState.toggleUltimateRecall(player);
            player.displayClientMessage(Component.translatable(
                    enabled ? "frb.eternal.recall_on" : "frb.eternal.recall_off"), true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (event.getItemStack().is(FRBItems.charmOf(CharmType.FOREST_WHISPER))
                || event.getItemStack().is(FRBItems.ETERNAL_SPIRIT.get())) {
            boolean enabled = FRBState.toggleGrowth(player);
            player.displayClientMessage(Component.translatable(
                    enabled ? "frb.growth.enabled" : "frb.growth.disabled"), true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (player instanceof ServerPlayer serverPlayer
                && event.getItemStack().is(FRBItems.charmOf(CharmType.FROST_CHIME))) {
            boolean enabled = FrostChimeRecall.toggleAutoRecall(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    enabled ? "frb.frost_recall.auto_enabled" : "frb.frost_recall.auto_disabled"), true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** 森语藤：附近动物诞下幼崽时给予祝福（回血 + 催生作物）。 */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        LivingEntity child = event.getChild();
        ServerLevel level = child.level() instanceof ServerLevel sl ? sl : null;
        if (level == null) return;
        Player nearby = level.getNearestPlayer(child, 16.0D);
        if (nearby != null && FRBState.active(nearby, CharmType.FOREST_WHISPER)) {
            nearby.heal(1.0F);
            if (FRBState.growthEnabled(nearby)) {
                GrowthBoost.boost(level, child.blockPosition(), 6, 3);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, child.getX(), child.getY() + 0.6D,
                    child.getZ(), 16, 0.4D, 0.5D, 0.4D, 0.05D);
        }
    }

    private CharmTickEvents() {}
}
