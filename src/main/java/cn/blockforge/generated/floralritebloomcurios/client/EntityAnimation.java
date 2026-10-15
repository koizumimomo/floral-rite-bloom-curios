package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.item.MysteriousWineItem;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 一次「果冻 + 旋转」动画的状态（移植自 friendswine 的 EntityAnimation，砍掉了本项目用不到的
 * 音乐同步字段）。
 * <p>
 * <b>时钟不需要额外同步</b>：用「醉酒效果总时长 − 剩余时长」反推已进行的秒数。
 * 效果剩余时长本来就会被原版 {@code ClientboundUpdateMobEffectPacket} 同步到客户端，
 * 所以动画时序是白拿的——不用加包，也不用在实体上存状态。
 *
 * @param seconds  动画已进行的秒数（含 partialTick）
 * @param rotating 是否同时绕 Y 轴旋转
 */
public record EntityAnimation(double seconds, boolean rotating) {

    /** 当前挤压量（0~1）。 */
    public float squash() {
        return JellyAnimation.squash(seconds);
    }

    /** 光环半径（格）。与 friendswine 的酒娃娃一致，10 格。 */
    public static final double AURA_RADIUS = 10.0D;

    /**
     * 任意生物：自己身上有醉酒效果，或半径 {@link #AURA_RADIUS} 内有喝醉的玩家 → 动画。
     *
     * @return 不需要动画时返回 null
     */
    @Nullable
    public static EntityAnimation forEntity(LivingEntity entity, float partialTick) {
        if (!entity.isAlive()) return null;

        // 1) 自己就中招了（拿酒右键生物那条路径）
        MobEffectInstance own = entity.getEffect(FRBEffects.DRUNK.get());
        if (own != null) return new EntityAnimation(elapsedSeconds(own, partialTick), true);

        // 2) 否则看周围有没有喝醉的玩家 —— 喝酒 → 一圈生物一起蠕动
        Player source = nearestDrunkPlayer(entity.level(), entity.position());
        if (source == null) return null;
        MobEffectInstance inst = source.getEffect(FRBEffects.DRUNK.get());
        if (inst == null) return null;
        return new EntityAnimation(elapsedSeconds(inst, partialTick), true);
    }

    /**
     * 相机：用和生物一样的判定，所以喝了酒的玩家镜头会跟着一起被压下去
     * （第一人称下看不到自己的模型，只能靠镜头下沉来"感觉到"变形）。
     */
    @Nullable
    public static EntityAnimation forCamera(Player player, float partialTick) {
        return forEntity(player, partialTick);
    }

    /** 效果已进行秒数 =（总时长 − 剩余时长 + partialTick）/ 20。 */
    private static double elapsedSeconds(MobEffectInstance inst, float partialTick) {
        return Math.max(0.0F, MysteriousWineItem.DRUNK_DURATION - inst.getDuration() + partialTick) / 20.0D;
    }

    /** 半径内最近的「持有醉酒效果且活着」的玩家；没有则 null。 */
    @Nullable
    private static Player nearestDrunkPlayer(Level level, Vec3 pos) {
        Player best = null;
        double bestSq = AURA_RADIUS * AURA_RADIUS;
        for (Player p : level.players()) {
            if (!p.isAlive() || !p.hasEffect(FRBEffects.DRUNK.get())) continue;
            double dSq = p.position().distanceToSqr(pos);
            if (dSq <= bestSq) {
                bestSq = dSq;
                best = p;
            }
        }
        return best;
    }
}
