package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;

public final class FRBParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<SimpleParticleType> CONVERGING_PETAL =
            PARTICLES.register("converging_petal", () -> new SimpleParticleType(true));

    /** 永恒花园环境花瓣（多色 sprite 随机）。 */
    public static final RegistryObject<SimpleParticleType> GARDEN_PETAL =
            PARTICLES.register("garden_petal", () -> new SimpleParticleType(false));

    /** 每个花灵饰品一个染色花瓣粒子类型（共享花瓣贴图，按类型常量染色）。 */
    public static final Map<CharmType, RegistryObject<SimpleParticleType>> PETALS = new EnumMap<>(CharmType.class);

    // ---- BOSS 新机制粒子（贴图均由用户提供） ----

    /** 落雷头顶标记（贴图 electricity_1~4；客户端每 tick 循环切换 4 张帧）。 */
    public static final RegistryObject<SimpleParticleType> LIGHTNING_MARK =
            PARTICLES.register("lightning_mark", () -> new SimpleParticleType(false));
    /** 灼热之种红圈粒子（贴图 flame_shard，已存在；注册名沿用旧）。 */
    public static final RegistryObject<SimpleParticleType> BURNING_SEED_MARK =
            PARTICLES.register("burning_seed_mark", () -> new SimpleParticleType(false));
    /** 烈焰击退蓄力粒子（贴图 ember_spark）。 */
    public static final RegistryObject<SimpleParticleType> FLAME_KNOCKBACK_CHARGE =
            PARTICLES.register("flame_knockback_charge", () -> new SimpleParticleType(false));
    /** 复活提示（贴图 soul_fire_1~8）。 */
    public static final RegistryObject<SimpleParticleType> REVIVE_WISP =
            PARTICLES.register("revive_wisp", () -> new SimpleParticleType(false));
    /** 虚空飞溅（贴图 ender_slash_1~5；裂隙生成/H1 时使用）。 */
    public static final RegistryObject<SimpleParticleType> VOID_SLASH =
            PARTICLES.register("void_slash", () -> new SimpleParticleType(false));
    /** 虚空潮汐伤害结算星芒（贴图 star_streak，蓝紫星芒拖尾）。 */
    public static final RegistryObject<SimpleParticleType> TIDE_VOID_STAR =
            PARTICLES.register("tide_void_star", () -> new SimpleParticleType(false));
    /** 时间潮汐伤害结算圣星（贴图 holy_star，金色八芒星）。 */
    public static final RegistryObject<SimpleParticleType> TIDE_TIME_STAR =
            PARTICLES.register("tide_time_star", () -> new SimpleParticleType(false));
    /** 霜凝之眼（D1 蓄力时 boss 眼前单颗定位，贴图 frost_eye.png）。 */
    public static final RegistryObject<SimpleParticleType> FROST_EYE =
            PARTICLES.register("frost_eye", () -> new SimpleParticleType(false));
    /** 霜凝印记（r66：boss 头顶展示的 gaze.png 印记，用户提供的贴图）。 */
    public static final RegistryObject<SimpleParticleType> GAZE_MARK =
            PARTICLES.register("gaze_mark", () -> new SimpleParticleType(false));

    /** 时间阶段主题粒子（time_phase_01~06：6 张主题；time_hourglass/time_clock_face/hands/tick_ring：装饰）。 */
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_01 =
            PARTICLES.register("time_phase_01", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_02 =
            PARTICLES.register("time_phase_02", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_03 =
            PARTICLES.register("time_phase_03", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_04 =
            PARTICLES.register("time_phase_04", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_05 =
            PARTICLES.register("time_phase_05", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_PHASE_06 =
            PARTICLES.register("time_phase_06", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_HOURGLASS =
            PARTICLES.register("time_hourglass", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_CLOCK_FACE =
            PARTICLES.register("time_clock_face", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_CLOCK_HANDS =
            PARTICLES.register("time_clock_hands", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_TICK_RING =
            PARTICLES.register("time_tick_ring", () -> new SimpleParticleType(false));

    /** 丰饶花瓣弹的飞行尾迹（无重力、原地淡出的花瓣光点）。 */
    public static final RegistryObject<SimpleParticleType> BOUNTIFUL_TRAIL =
            PARTICLES.register("bountiful_trail", () -> new SimpleParticleType(false));

    static {
        for (CharmType type : CharmType.ALL) {
            PETALS.put(type, PARTICLES.register(type.petalId(), () -> new SimpleParticleType(false)));
        }
    }

    public static SimpleParticleType petal(CharmType type) {
        return PETALS.get(type).get();
    }

    // ---- Boss 演出粒子（贴图来自 Iron-Elden-Ring-Particle-library，MIT, shuimo0413） ----

    /** 开场三幕：星系螺旋 / 星芒拖尾 / 中心光球 / 星云雾气。 */
    public static final RegistryObject<SimpleParticleType> INTRO_SPIRAL =
            PARTICLES.register("intro_spiral", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> INTRO_STAR =
            PARTICLES.register("intro_star", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> INTRO_BLOOM =
            PARTICLES.register("intro_bloom", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> INTRO_WISP =
            PARTICLES.register("intro_wisp", () -> new SimpleParticleType(false));
    /** 转阶段脉冲环 / 开场爆发环。 */
    public static final RegistryObject<SimpleParticleType> PULSE_RING =
            PARTICLES.register("pulse_ring", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ECLIPSE_RING =
            PARTICLES.register("eclipse_ring", () -> new SimpleParticleType(false));

    /** 每阶段一个主题微粒（自然/焚尽/霜华/云雨/天空/虚空/枯萎），下标与 FriliasEntity.phase 对齐。 */
    public static final java.util.List<RegistryObject<SimpleParticleType>> THEME_MOTES = new java.util.ArrayList<>();

    static {
        // r79：粒子注册名必须与 textures/particle/<name>.png 一一对应——之前用 burn_mote/frost_mote 等
        //      注册名，但 textures/particle/ 里没有对应 png，结果 GeckoLib sprite 加载失败，
        //      渲染出紫黑缺图方块。已对齐到包内已有的贴图：
        //      0 焚尽烧火 → 夜晚"霜华阶段"被映射为 nature_petal.png(自然色花瓣)
        //      1 焚尽烧火 → ember_wisp.png(焚生藤火苗)
        //      2 霜华凝晶 → frost_crystal.png(霜晶)
        //      3 云雨冷雾 → violet_shard.png(紫水晶碎片, 与云雨同色系)
        //      4 天空星屑 → star_cluster.png(星团)
        //      5 虚空裂芒 → void_core.png(虚空核)
        //      6 枯萎尘灰 → ash_mote.png(尘灰)
        String[] names = {"nature_petal", "ember_wisp", "frost_crystal",
                "violet_shard", "star_cluster", "void_core", "ash_mote"};
        for (String name : names) {
            THEME_MOTES.add(PARTICLES.register(name, () -> new SimpleParticleType(false)));
        }
    }

    public static SimpleParticleType themeMote(int phase) {
        return THEME_MOTES.get(Math.floorMod(phase, THEME_MOTES.size())).get();
    }

    /** 花灵仪式 → 主题微粒（自然/焚尽/霜华/云雨/天空/虚空/枯萎，与 FriliasEntity.phase 对齐）。 */
    public static SimpleParticleType moteOf(CharmType type) {
        return switch (type) {
            case FOREST_WHISPER -> THEME_MOTES.get(0).get();
            case EMBER_VINE -> THEME_MOTES.get(1).get();
            case FROST_CHIME -> THEME_MOTES.get(2).get();
            case SKY_TEAR -> THEME_MOTES.get(3).get();
            case SKY_PILLAR -> THEME_MOTES.get(4).get();
            case ASTRAL_BLOOM -> THEME_MOTES.get(5).get();
            case BLACK_SPIRIT -> THEME_MOTES.get(6).get();
        };
    }

    private FRBParticles() {}
}
