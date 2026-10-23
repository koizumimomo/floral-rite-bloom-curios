package cn.blockforge.generated.floralritebloomcurios.util;

import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.lang.reflect.Method;

/**
 * r112：不死图腾 / 伤害来源归属 / 致死判定的共用工具。
 *
 * <p><b>背景</b>：玩家反馈"装了本模组后不死图腾失效"。根因是本模组有两处"致死保护"
 * 会在原版图腾流程（{@code Player#die → checkTotemDeathProtection}）之前抢先取消伤害 /
 * 拉回血量，导致玩家根本不会进入死亡流程，图腾自然也不会被消耗、不会生效：
 * <ol>
 *   <li>{@code MercyEvents}（仁慈）原本拦截 <b>任何来源</b> 的致命伤害 —— 只要 64 格内有
 *       boss，摔落、岩浆、虚空、其他 mod 的伤害全被仁慈接管，绕开了图腾；</li>
 *   <li>{@code CharmEvents}（寒风铃快照 / 七花共鸣 / 终末花环）的复活分支在
 *       {@code LivingAttackEvent} 阶段抢先 cancel 伤害，同样让死亡流程不再发生。</li>
 * </ol>
 *
 * <p><b>修复</b>：统一优先级 —— <b>不死图腾 &gt; 花灵复活机制 &gt; 仁慈</b>。
 * 手持图腾时一律放行，交还原版流程消费图腾；仁慈只针对 boss 本体造成的伤害生效。
 */
public final class TotemTools {

    private TotemTools() {}

    /**
     * 玩家主手或副手是否持有不死图腾（原版 {@code checkTotemDeathProtection} 的判定口径）。
     * 命中时所有"致死保护"都应放行，让原版消费图腾。
     */
    public static boolean hasTotem(Player player) {
        if (player == null) return false;
        return player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    /**
     * 伤害是否来自芙莉艾丝本体。
     * <p>boss 的各类机制伤害来源统一带 boss 作为 entity / directEntity：
     * {@code mobAttack(this)}、{@code indirectMagic(this, this)}、{@code explosion(this, this)}、
     * 以及自定义的 {@code new DamageSource(holder, this, this)}。
     * <p>刻意 <b>不</b> 覆盖无主伤害（如 {@code lightningBolt()}、摔落、岩浆、虚空、
     * 其他模组的伤害）—— 那类伤害不属于"boss 攻击"，应完全交还原版处理，图腾才能正常生效。
     */
    public static boolean isBossDamage(DamageSource source) {
        if (source == null) return false;
        Entity entity = source.getEntity();
        if (entity instanceof FriliasEntity) return true;
        Entity direct = source.getDirectEntity();
        return direct instanceof FriliasEntity;
    }

    /** 原版 LivingEntity#getDamageAfterArmorAbsorb（protected，反射取用）。 */
    private static final Method M_ARMOR_ABSORB = findMethod("getDamageAfterArmorAbsorb");
    /** 原版 LivingEntity#getDamageAfterMagicAbsorb（protected，反射取用）。 */
    private static final Method M_MAGIC_ABSORB = findMethod("getDamageAfterMagicAbsorb");

    private static Method findMethod(String name) {
        try {
            Method m = LivingEntity.class.getDeclaredMethod(name, DamageSource.class, float.class);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * r112：按原版伤害公式估算「扣完护甲 / 抗性 / 吸收心后玩家是否真的会死」。
     *
     * <p>{@code LivingAttackEvent} 拿到的是原始伤害量，直接与当前血量比较会在高护甲、
     * 高吸收心时误判致死（本来打不死的伤害被当成致命，直接触发复活机制并把血量压到 1）。
     * 这里复刻 {@code LivingEntity#actuallyHurt} 的顺序：护甲 → 抗性 → 吸收心。</p>
     *
     * <p>原版这两个方法是 protected，故用反射调用原版自己的实现；
     * 反射不可用时（极端环境下）回退到"原始伤害 &gt;= 血量"的保守判定。</p>
     */
    public static boolean wouldActuallyKill(LivingEntity entity, DamageSource src, float rawAmount) {
        float effective = rawAmount;
        try {
            if (M_ARMOR_ABSORB != null && !src.is(DamageTypeTags.BYPASSES_ARMOR)) {
                effective = (float) M_ARMOR_ABSORB.invoke(entity, src, effective);
            }
            if (M_MAGIC_ABSORB != null) {
                effective = (float) M_MAGIC_ABSORB.invoke(entity, src, effective);
            }
        } catch (Throwable ignored) {
            // 反射失败 → 用原始值（与旧行为一致）
            effective = rawAmount;
        }
        effective = Math.max(effective - entity.getAbsorptionAmount(), 0.0F);
        return effective >= entity.getHealth();
    }
}
