package cn.blockforge.generated.floralritebloomcurios.abilities;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/** 寒风铃主动能力：记录玩家状态，并在三十秒内将玩家回溯到该状态。 */
public final class FrostChimeRecall {
    public static final long WINDOW_TICKS = 30L * 20L;
    public static final long COOLDOWN_TICKS = 30L * 20L;

    private static final String SNAPSHOT = "FrostRecallSnapshot";
    private static final String EXPIRES_AT = "FrostRecallExpiresAt";
    private static final String COOLDOWN_UNTIL = "FrostRecallCooldownUntil";
    private static final String AUTO_RECALL = "FrostRecallAuto";

    /** r96：返回是否成功执行（记录快照 / 触发回溯），供服务端决定是否下发冷却 HUD。 */
    public static boolean use(ServerPlayer player) {
        if (!FRBState.recallAllowed(player)) return false;
        CompoundTag state = state(player);
        long now = player.serverLevel().getGameTime();
        if (state.contains(SNAPSHOT, Tag.TAG_COMPOUND)) {
            restore(player, false);
            // restore 无论成功失败都会进入冷却流程（成功回溯 / 维度失效兜底），算成功触发
            return true;
        }
        long cooldown = Math.max(0L, state.getLong(COOLDOWN_UNTIL) - now);
        if (cooldown > 0L) {
            player.displayClientMessage(Component.translatable("frb.frost_recall.cd", seconds(cooldown))
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        CompoundTag snapshot = capture(player);
        state.put(SNAPSHOT, snapshot);
        state.putLong(EXPIRES_AT, now + WINDOW_TICKS);
        player.displayClientMessage(Component.translatable("frb.frost_recall.saved")
                .withStyle(ChatFormatting.AQUA), true);
        ServerLevel level = player.serverLevel();
        level.sendParticles(FRBParticles.petal(CharmType.FROST_CHIME), player.getX(), player.getY() + 1.0D,
                player.getZ(), 24, 0.45D, 0.8D, 0.45D, 0.04D);
        level.playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    public static void tick(ServerPlayer player) {
        CompoundTag state = state(player);
        if (!state.contains(SNAPSHOT, Tag.TAG_COMPOUND)) return;
        if (!FRBState.recallAllowed(player)) {
            finishWithoutRecall(player, state, "frb.frost_recall.lost");
            return;
        }
        if (player.serverLevel().getGameTime() < state.getLong(EXPIRES_AT)) return;
        if (autoRecall(player)) {
            restore(player, true);
        } else {
            finishWithoutRecall(player, state, "frb.frost_recall.expired");
        }
    }

    public static boolean toggleAutoRecall(ServerPlayer player) {
        CompoundTag state = state(player);
        boolean enabled = !autoRecall(player);
        state.putBoolean(AUTO_RECALL, enabled);
        return enabled;
    }

    public static boolean autoRecall(ServerPlayer player) {
        CompoundTag state = state(player);
        return !state.contains(AUTO_RECALL) || state.getBoolean(AUTO_RECALL);
    }

    private static CompoundTag capture(ServerPlayer player) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Dimension", player.level().dimension().location().toString());
        snapshot.putDouble("X", player.getX());
        snapshot.putDouble("Y", player.getY());
        snapshot.putDouble("Z", player.getZ());
        snapshot.putFloat("Yaw", player.getYRot());
        snapshot.putFloat("Pitch", player.getXRot());
        snapshot.putFloat("Health", player.getHealth());
        snapshot.putFloat("Absorption", player.getAbsorptionAmount());
        snapshot.putInt("Food", player.getFoodData().getFoodLevel());
        snapshot.putFloat("Saturation", player.getFoodData().getSaturationLevel());
        snapshot.putFloat("Exhaustion", player.getFoodData().getExhaustionLevel());
        snapshot.putInt("Air", player.getAirSupply());
        snapshot.putInt("Fire", player.getRemainingFireTicks());
        snapshot.putInt("ExperienceLevel", player.experienceLevel);
        snapshot.putInt("TotalExperience", player.totalExperience);
        snapshot.putFloat("ExperienceProgress", player.experienceProgress);
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.save(new CompoundTag()));
        }
        snapshot.put("Effects", effects);
        return snapshot;
    }

    private static void restore(ServerPlayer player, boolean automatic) {
        CompoundTag state = state(player);
        CompoundTag snapshot = state.getCompound(SNAPSHOT);
        if (snapshot.isEmpty()) {
            clearSession(state);
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(snapshot.getString("Dimension"));
        ResourceKey<Level> dimension = id == null ? player.level().dimension()
                : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel destination = player.server.getLevel(dimension);
        if (destination == null) {
            finishWithoutRecall(player, state, "frb.frost_recall.invalid");
            return;
        }

        ServerLevel origin = player.serverLevel();
        origin.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D,
                player.getZ(), 30, 0.5D, 0.9D, 0.5D, 0.05D);
        player.stopRiding();
        player.teleportTo(destination, snapshot.getDouble("X"), snapshot.getDouble("Y"),
                snapshot.getDouble("Z"), snapshot.getFloat("Yaw"), snapshot.getFloat("Pitch"));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setHealth(Math.min(snapshot.getFloat("Health"), player.getMaxHealth()));
        player.setAbsorptionAmount(Math.max(0.0F, snapshot.getFloat("Absorption")));
        player.getFoodData().setFoodLevel(snapshot.getInt("Food"));
        player.getFoodData().setSaturation(snapshot.getFloat("Saturation"));
        player.getFoodData().setExhaustion(snapshot.getFloat("Exhaustion"));
        player.setAirSupply(snapshot.getInt("Air"));
        player.setRemainingFireTicks(snapshot.getInt("Fire"));
        player.experienceLevel = snapshot.getInt("ExperienceLevel");
        player.totalExperience = snapshot.getInt("TotalExperience");
        player.experienceProgress = snapshot.getFloat("ExperienceProgress");
        player.removeAllEffects();
        ListTag effects = snapshot.getList("Effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = MobEffectInstance.load(effects.getCompound(i));
            if (effect != null) player.addEffect(effect);
        }
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        destination.sendParticles(FRBParticles.petal(CharmType.FROST_CHIME), player.getX(),
                player.getY() + 1.0D, player.getZ(), 42, 0.65D, 1.0D, 0.65D, 0.07D);
        destination.playSound(null, player,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.CHIME_RECALL.get(),
                SoundSource.PLAYERS, 1.0F, 1.5F);
        clearSession(state);
        state.putLong(COOLDOWN_UNTIL, destination.getGameTime() + COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable(automatic
                        ? "frb.frost_recall.auto" : "frb.frost_recall.restored")
                .withStyle(ChatFormatting.AQUA), true);
    }

    private static void finishWithoutRecall(ServerPlayer player, CompoundTag state, String message) {
        clearSession(state);
        state.putLong(COOLDOWN_UNTIL, player.serverLevel().getGameTime() + COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable(message)
                .withStyle(ChatFormatting.GRAY), true);
    }

    private static void clearSession(CompoundTag state) {
        state.remove(SNAPSHOT);
        state.remove(EXPIRES_AT);
    }

    private static long seconds(long ticks) {
        return (ticks + 19L) / 20L;
    }

    private static CompoundTag state(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag state = root.getCompound(FRBState.TAG);
        root.put(FRBState.TAG, state);
        return state;
    }

    private FrostChimeRecall() {}
}
