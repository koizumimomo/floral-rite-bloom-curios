package cn.blockforge.generated.floralritebloomcurios.effect;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** 无限饰品增益按来源维护；卸下、封印、离水后恢复原有药水效果。 */
public final class CharmBuffs {
    public static void tick(Player player) {
        boolean forest = FRBState.active(player, CharmType.FOREST_WHISPER);
        boolean water = FRBState.hasUltimate(player)
                || (FRBState.active(player, CharmType.SKY_TEAR) && player.isUnderWater());
        sync(player, MobEffects.REGENERATION, "Regen", forest);
        sync(player, MobEffects.DIG_SPEED, "Haste", forest);
        sync(player, MobEffects.NIGHT_VISION, "Vision", water);
        sync(player, MobEffects.WATER_BREATHING, "Breathing", water);
    }

    private static void sync(Player player, MobEffect effect, String key, boolean active) {
        CompoundTag root = player.getPersistentData();
        String owner = "FRBOwnedBuff_" + key;
        CompoundTag saved = root.getCompound(owner);
        MobEffectInstance current = player.getEffect(effect);
        boolean ours = current != null && current.isInfiniteDuration()
                && current.getAmplifier() == 0 && current.isAmbient() && !current.isVisible();
        if (active) {
            if (root.contains(owner) && ours) return;
            // 更强的效果保留，结束后再恢复饰品提供的一级效果。
            if (current != null && current.getAmplifier() > 0) return;
            if (ours && !root.contains(owner)) return;
            if (!root.contains(owner)) {
                saved.putLong("Since", player.level().getGameTime());
                if (current != null) saved.put("Previous", current.save(new CompoundTag()));
                root.put(owner, saved);
            }
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION,
                    0, true, false, true));
        } else if (root.contains(owner)) {
            if (ours) {
                player.removeEffect(effect);
                MobEffectInstance previous = MobEffectInstance.load(saved.getCompound("Previous"));
                if (previous != null) {
                    long elapsed = Math.max(0, player.level().getGameTime() - saved.getLong("Since"));
                    int duration = previous.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION
                            : (int) Math.max(0, previous.getDuration() - elapsed);
                    if (duration != 0) player.addEffect(new MobEffectInstance(effect, duration,
                            previous.getAmplifier(), previous.isAmbient(), previous.isVisible(), previous.showIcon()));
                }
            } else if (current != null) {
                // 强效药水可能把饰品效果压入 HiddenEffect，卸下时也必须移除该层。
                CompoundTag data = current.save(new CompoundTag());
                if (stripHidden(data)) {
                    MobEffectInstance replacement = MobEffectInstance.load(data);
                    if (replacement != null) {
                        player.removeEffect(effect);
                        player.addEffect(replacement);
                    }
                }
            }
            root.remove(owner);
        }
    }

    private static boolean stripHidden(CompoundTag data) {
        if (!data.contains("HiddenEffect")) return false;
        CompoundTag hidden = data.getCompound("HiddenEffect");
        boolean changed = stripHidden(hidden);
        if (hidden.getInt("Duration") == MobEffectInstance.INFINITE_DURATION
                && hidden.getByte("Amplifier") == 0 && hidden.getBoolean("Ambient")
                && !hidden.getBoolean("ShowParticles")) {
            if (hidden.contains("HiddenEffect")) data.put("HiddenEffect", hidden.getCompound("HiddenEffect"));
            else data.remove("HiddenEffect");
            return true;
        }
        return changed;
    }

    private CharmBuffs() {}
}
