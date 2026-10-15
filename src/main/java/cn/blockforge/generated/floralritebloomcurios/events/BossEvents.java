package cn.blockforge.generated.floralritebloomcurios.events;

import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;

/**
 * 世界之花 Boss 的谢幕：掉落终极合成所需的原始花瓣、世界之花种子、经验与公告。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class BossEvents {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof FriliasEntity boss)) return;
        if (!(boss.level() instanceof ServerLevel level)) return;
        level.playSound(null, boss, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.0F, 0.7F);
        for (int i = 0; i < 40; i++) {
            level.sendParticles(ParticleTypes.WITCH, boss.getX(), boss.getY() + 1.5D, boss.getZ(),
                    2, 1.6D, 1.8D, 1.6D, 0.1D);
            level.sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + 1.2D, boss.getZ(),
                    2, 1.2D, 1.4D, 1.2D, 0.05D);
        }
        // 第七阶段被击败：解除全部封印，由 Boss 亲手交出【万花万草之灵·祝福与永恒相随】
        boss.releaseFight(true);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                boss.getBoundingBox().inflate(96.0D))) {
            p.displayClientMessage(Component.translatable("frb.boss.defeated")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof FriliasEntity boss)) return;
        if (!(boss.level() instanceof ServerLevel level)) return;
        event.getDrops().clear();
        ItemEntity will = new ItemEntity(level, boss.getX(), boss.getY() + 1.4D, boss.getZ(),
                new ItemStack(FRBItems.WORLD_FLOWER_WILL.get(), 1));
        will.setUnlimitedLifetime();
        will.setNoPickUpDelay();
        level.addFreshEntity(will);
        ItemEntity petal = new ItemEntity(level, boss.getX(), boss.getY() + 1.1D, boss.getZ(),
                new ItemStack(FRBItems.PRIMORDIAL_PETAL.get(), 1));
        petal.setNoPickUpDelay();
        level.addFreshEntity(petal);
        level.addFreshEntity(new ItemEntity(level, boss.getX(), boss.getY() + 1.0D, boss.getZ(),
                new ItemStack(FRBItems.SEED_OF_REALM.get(), 1)));
        level.addFreshEntity(new ExperienceOrb(level, boss.getX(), boss.getY() + 1.0D, boss.getZ(), 500));
    }

    private BossEvents() {}

    /**
     * r95：枯萎阶段禁止自然生命恢复——领域内 64 格有处于枯萎阶段的芙莉艾丝时，
     * 取消所有对玩家的回血事件（饱食度回血、药水回血、自然回血等）。
     * 仁慈传送不走 LivingHealEvent，不受影响。
     */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (player.isCreative() || player.isSpectator()) return;
        ServerLevel level = (ServerLevel) player.level();
        boolean blocked = !level.getEntitiesOfClass(FriliasEntity.class,
                player.getBoundingBox().inflate(64.0D),
                b -> b.isAlive() && !b.isRemoved() && b.isDecayPhase()).isEmpty();
        if (blocked) event.setCanceled(true);
    }
}
