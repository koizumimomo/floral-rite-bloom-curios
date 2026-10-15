package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** 成就查询 / 授予助手（1.20.1：直接 PlayerAdvancements.award，无需自定义触发器）。 */
public final class FRBAdvancements {

    public static Advancement get(ServerPlayer player, String path) {
        return player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(FloralRiteBloom.MOD_ID, path));
    }

    /** 玩家是否已完成某成就（成就不存在视为未完成）。 */
    public static boolean hasDone(ServerPlayer player, String path) {
        Advancement adv = get(player, path);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** 授予（逐个完成剩余 criterion；已完成则忽略）。 */
    public static void award(ServerPlayer player, String path) {
        Advancement adv = get(player, path);
        if (adv == null) return;
        AdvancementProgress prog = player.getAdvancements().getOrStartProgress(adv);
        if (prog.isDone()) return;
        for (String crit : prog.getRemainingCriteria()) {
            player.getAdvancements().award(adv, crit);
        }
    }

    /** 给 center 周围 radius 格内的所有玩家授予成就（目睹类演出的成就用）。 */
    public static void awardAround(ServerLevel level, Entity center, double radius, String path) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                center.getBoundingBox().inflate(radius))) {
            award(p, path);
        }
    }

    private FRBAdvancements() {}
}
