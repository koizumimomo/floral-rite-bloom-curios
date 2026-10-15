package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.effect.BlackDeathEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.CloudGraceEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.DampnessEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.DrunkEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.FloralSpiritAidEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.FrostBloomEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.FrostbiteEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.PetalMarkEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.RainBloomEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.SoggyEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.TimeFlowerEffect;
import cn.blockforge.generated.floralritebloomcurios.effect.VoidErosionEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FRBEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FloralRiteBloom.MOD_ID);

    /** 霜蚀：每秒受到细雪伤害并附带减速（寒风铃 & 终末花环冰封）。 */
    public static final RegistryObject<FrostbiteEffect> FROSTBITE =
            EFFECTS.register("frostbite", FrostbiteEffect::new);

    /** 黑死病：持续生命流失（黑灵草瘟疫光环）。 */
    public static final RegistryObject<BlackDeathEffect> BLACK_DEATH =
            EFFECTS.register("black_death", BlackDeathEffect::new);

    /** 云之华：Boss 云雨阶段附加，攻击大幅削弱、恢复与流失反转。 */
    public static final RegistryObject<CloudGraceEffect> CLOUD_GRACE =
            EFFECTS.register("cloud_grace", CloudGraceEffect::new);

    /** 花瓣印记：焚生藤攻击叠加，第 5 次命中引爆。 */
    public static final RegistryObject<PetalMarkEffect> PETAL_MARK =
            EFFECTS.register("petal_mark", PetalMarkEffect::new);

    /** 霜之华：Boss 霜华阶段附加，攻击伤害 -50%。 */
    public static final RegistryObject<FrostBloomEffect> FROST_BLOOM =
            EFFECTS.register("frost_bloom", FrostBloomEffect::new);

    /** 雨之华（云雨阶段 debuff）：每层 -5% 最大生命值，最高 12 层。 */
    public static final RegistryObject<RainBloomEffect> RAIN_BLOOM =
            EFFECTS.register("rain_bloom", RainBloomEffect::new);

    /** 潮湿（云雨阶段 debuff）：落雷伤害 ×1.5。 */
    public static final RegistryObject<DampnessEffect> DAMPNESS =
            EFFECTS.register("dampness", DampnessEffect::new);

    /** 虚空侵蚀（G2 地块 debuff）：每层 -1% 最大生命/攻击/移速，最高 20 层；离开后每秒 -1。 */
    public static final RegistryObject<VoidErosionEffect> VOID_EROSION =
            EFFECTS.register("void_erosion", VoidErosionEffect::new);

    /** 花灵之助（B1 法阵 buff）：+20% 攻击伤害。 */
    public static final RegistryObject<FloralSpiritAidEffect> FLORAL_SPIRIT_AID =
            EFFECTS.register("floral_spirit_aid", FloralSpiritAidEffect::new);

    /** 浸泡（云雨阶段万变水波 debuff）：受到的所有伤害 +25%，持续 5 秒。 */
    public static final RegistryObject<SoggyEffect> SOGGY =
            EFFECTS.register("soggy", SoggyEffect::new);

    /** 喝醉了：实体果冻挤压回弹；芙莉艾丝处于此状态时不释放任何技能。 */
    public static final RegistryObject<DrunkEffect> DRUNK =
            EFFECTS.register("drunk", DrunkEffect::new);

    /** 时之华（时间阶段 debuff）：造成伤害 -70%，持续整个阶段。 */
    public static final RegistryObject<TimeFlowerEffect> TIME_FLOWER =
            EFFECTS.register("time_flower", TimeFlowerEffect::new);

    private FRBEffects() {}
}
