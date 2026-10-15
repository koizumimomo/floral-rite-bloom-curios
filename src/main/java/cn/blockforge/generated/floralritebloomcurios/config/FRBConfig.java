package cn.blockforge.generated.floralritebloomcurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * r72：Boss 机制配置（config/frb_boss.toml，COMMON 类型）。
 *
 * 架构：8 个阶段选项卡（自然/焚尽/霜华/云雨/天空/虚空/枯萎/时间），
 * 每个选项卡下展开当前阶段所有机制的 cd / damage 倍率。
 *
 * - cd 倍率：0.5x ~ 2.0x（默认 1.0x；改小 = 更频繁，改大 = 更慢）
 * - damage 倍率：0.1x ~ 10.0x（即 10% ~ 1000%，默认 1.0x；改大 = 更疼）
 *
 * 集成包作者可按包内平衡随意调整数值；文件在首次启动世界时生成。
 *
 * 说明：Common 类型配置在世界加载时读取、修改后自动重载（ModConfigEvent.Reloading），
 * ConfigValue#get() 直读缓存值，tick 内调用开销可忽略。
 */
public final class FRBConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- 战斗通用 ----
    private static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue MULTIPLAYER_MECHANICS;

    // ---- r74：神环渲染样式（玩家背后"万花万草之灵"双层光环，boss 不受影响）----
    public enum HaloStyle { PIXELATED, HIGH_RES }
    private static final ForgeConfigSpec.EnumValue<HaloStyle> HALO_STYLE;

    // ---- r78：神环前面渲染 + HUD 位置（从 FRBClientConfig 合并到此处，便于 mods 列表配置入口直接调）----
    private static final ForgeConfigSpec.BooleanValue SHOW_FRONT_RING;
    private static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    private static final ForgeConfigSpec.IntValue HUD_X;
    private static final ForgeConfigSpec.IntValue HUD_Y;

    // ---- 防御（锁血 / 连击保护）----
    private static final ForgeConfigSpec.DoubleValue MAX_HIT_FRACTION;
    /** r90：时间阶段单次伤害上限（占最大生命比例，0.08 = 单击最多扣 8%）。 */
    private static final ForgeConfigSpec.DoubleValue TIME_MAX_HIT_FRACTION;
    private static final ForgeConfigSpec.BooleanValue COMBO_PROTECT_ENABLED;
    private static final ForgeConfigSpec.IntValue COMBO_WINDOW_TICKS;
    private static final ForgeConfigSpec.DoubleValue COMBO_FACTOR;

    // ---- 基础 cd（tick）— 在 mechanics 节定义，最终 cd = base * CD_xxx倍率（最小 1 tick）----
    private static final ForgeConfigSpec.IntValue THORN_VOLLEY;
    private static final ForgeConfigSpec.IntValue NATURE_BEAM;
    private static final ForgeConfigSpec.IntValue FLOWER_CRADLE;
    private static final ForgeConfigSpec.IntValue THORN_SYMBIOSIS;
    private static final ForgeConfigSpec.IntValue BURN_LIFE_FIRE;
    private static final ForgeConfigSpec.IntValue BURN_FIRE_SPOUT;
    private static final ForgeConfigSpec.IntValue BURNING_SEED;
    private static final ForgeConfigSpec.IntValue BURNING_SEED_FUSE;
    private static final ForgeConfigSpec.IntValue FLAME_KNOCKBACK;
    /** r96：光辉炎柱（焚尽阶段随机落点炎柱 AOE）基础 cd（240t = 12s）。 */
    private static final ForgeConfigSpec.IntValue FLAME_PILLAR;
    private static final ForgeConfigSpec.IntValue FROST_ICE_SPIKE;
    private static final ForgeConfigSpec.IntValue FROST_BEAM;
    private static final ForgeConfigSpec.IntValue FROST_GAZE;
    private static final ForgeConfigSpec.IntValue FROST_SIGIL;
    /** r96：钢铁月环（霜华阶段圆形/环形 AOE）基础 cd（140t = 7s）。 */
    private static final ForgeConfigSpec.IntValue FROST_MOON_RING;
    private static final ForgeConfigSpec.IntValue RAIN_BLOOM;
    private static final ForgeConfigSpec.IntValue LIGHTNING;
    private static final ForgeConfigSpec.IntValue RAIN_CLOUD;
    /** r96：云雨全屏 AOE 基础 cd（480t = 24s）。 */
    private static final ForgeConfigSpec.IntValue RAIN_AOE;
    private static final ForgeConfigSpec.IntValue WIND_BLADE;
    private static final ForgeConfigSpec.IntValue SKY_SHOCKWAVE;
    private static final ForgeConfigSpec.IntValue WIND_EYE;
    private static final ForgeConfigSpec.IntValue FEATHER_SWORD;
    private static final ForgeConfigSpec.IntValue VOID_RIFT;
    private static final ForgeConfigSpec.IntValue VOID_EROSION;
    /** r90：虚空潮汐 基础 cd（240t = 12s）。 */
    private static final ForgeConfigSpec.IntValue VOID_TIDE;
    /** r90：时间潮汐 基础 cd（240t = 12s）。 */
    private static final ForgeConfigSpec.IntValue TIME_TIDE;
    private static final ForgeConfigSpec.IntValue DEATH_FLOWER;
    private static final ForgeConfigSpec.IntValue TIME_ROTATE;
    private static final ForgeConfigSpec.IntValue LIFE_DRAIN_CD;
    private static final ForgeConfigSpec.IntValue TIME_STOP_CD;
    private static final ForgeConfigSpec.IntValue REWIND_CD;

    // ---- r72：八阶段机制 cd 倍率（0.5x ~ 2.0x）----
    // 自然
    private static final ForgeConfigSpec.DoubleValue CD_NATURE_THORN_VOLLEY;
    private static final ForgeConfigSpec.DoubleValue CD_NATURE_BEAM;
    private static final ForgeConfigSpec.DoubleValue CD_NATURE_FLOWER_CRADLE;
    private static final ForgeConfigSpec.DoubleValue CD_NATURE_THORN_SYMBIOSIS;
    // 焚尽
    private static final ForgeConfigSpec.DoubleValue CD_BURN_LIFE_FIRE;
    private static final ForgeConfigSpec.DoubleValue CD_BURN_FIRE_SPOUT;
    private static final ForgeConfigSpec.DoubleValue CD_BURNING_SEED;
    private static final ForgeConfigSpec.DoubleValue CD_BURNING_SEED_FUSE;
    private static final ForgeConfigSpec.DoubleValue CD_FLAME_KNOCKBACK;
    /** r96：光辉炎柱 cd 倍率。 */
    private static final ForgeConfigSpec.DoubleValue CD_FLAME_PILLAR;
    // 霜华
    private static final ForgeConfigSpec.DoubleValue CD_FROST_ICE_SPIKE;
    private static final ForgeConfigSpec.DoubleValue CD_FROST_BEAM;
    private static final ForgeConfigSpec.DoubleValue CD_FROST_GAZE;
    private static final ForgeConfigSpec.DoubleValue CD_FROST_SIGIL;
    /** r96：钢铁月环 cd 倍率。 */
    private static final ForgeConfigSpec.DoubleValue CD_FROST_MOON_RING;
    // 云雨
    private static final ForgeConfigSpec.DoubleValue CD_RAIN_BLOOM;
    private static final ForgeConfigSpec.DoubleValue CD_LIGHTNING;
    private static final ForgeConfigSpec.DoubleValue CD_RAIN_CLOUD;
    /** r96：云雨全屏 AOE cd 倍率。 */
    private static final ForgeConfigSpec.DoubleValue CD_RAIN_AOE;
    // 天空
    private static final ForgeConfigSpec.DoubleValue CD_WIND_BLADE;
    private static final ForgeConfigSpec.DoubleValue CD_SKY_SHOCKWAVE;
    private static final ForgeConfigSpec.DoubleValue CD_WIND_EYE;
    private static final ForgeConfigSpec.DoubleValue CD_FEATHER_SWORD;
    private static final ForgeConfigSpec.DoubleValue DMG_WIND_EYE;
    private static final ForgeConfigSpec.DoubleValue DMG_FEATHER_SWORD;
    // 虚空
    private static final ForgeConfigSpec.DoubleValue CD_VOID_RIFT;
    private static final ForgeConfigSpec.DoubleValue CD_VOID_EROSION;
    // 虚空伤害倍率
    private static final ForgeConfigSpec.DoubleValue DMG_VOID_EROSION;
    /** r90：虚空潮汐 cd / 伤害倍率。 */
    private static final ForgeConfigSpec.DoubleValue CD_VOID_TIDE;
    private static final ForgeConfigSpec.DoubleValue DMG_VOID_TIDE;
    // 枯萎
    private static final ForgeConfigSpec.DoubleValue CD_DEATH_FLOWER;
    // 时间
    private static final ForgeConfigSpec.DoubleValue CD_TIME_ROTATE;
    private static final ForgeConfigSpec.DoubleValue CD_LIFE_DRAIN;
    private static final ForgeConfigSpec.DoubleValue CD_TIME_STOP;
    private static final ForgeConfigSpec.DoubleValue CD_REWIND;
    /** r90：时间潮汐 cd / 伤害倍率。 */
    private static final ForgeConfigSpec.DoubleValue CD_TIME_TIDE;
    private static final ForgeConfigSpec.DoubleValue DMG_TIME_TIDE;

    // ---- r72：八阶段机制伤害倍率（0.1x ~ 10.0x）----
    // 自然
    private static final ForgeConfigSpec.DoubleValue DMG_NATURE_THORN_VOLLEY;
    private static final ForgeConfigSpec.DoubleValue DMG_NATURE_BEAM;
    private static final ForgeConfigSpec.DoubleValue DMG_NATURE_THORN_SYMBIOSIS;
    // 焚尽
    private static final ForgeConfigSpec.DoubleValue DMG_BURNING_SEED;
    private static final ForgeConfigSpec.DoubleValue DMG_FLAME_KNOCKBACK;
    /** r96：光辉炎柱 伤害倍率。 */
    private static final ForgeConfigSpec.DoubleValue DMG_FLAME_PILLAR;
    // 霜华
    private static final ForgeConfigSpec.DoubleValue DMG_FROST_ICE_SPIKE;
    private static final ForgeConfigSpec.DoubleValue DMG_FROST_BEAM;
    private static final ForgeConfigSpec.DoubleValue DMG_FROST_GAZE;
    /** r96：钢铁月环 伤害倍率。 */
    private static final ForgeConfigSpec.DoubleValue DMG_FROST_MOON_RING;
    // 云雨
    private static final ForgeConfigSpec.DoubleValue DMG_LIGHTNING;
    /** r96：云雨全屏 AOE 伤害倍率。 */
    private static final ForgeConfigSpec.DoubleValue DMG_RAIN_AOE;
    // 天空
    private static final ForgeConfigSpec.DoubleValue DMG_WIND_BLADE;
    private static final ForgeConfigSpec.DoubleValue DMG_SKY_SHOCKWAVE;
    // 虚空
    private static final ForgeConfigSpec.DoubleValue DMG_VOID_RIFT;
    // 时间
    private static final ForgeConfigSpec.DoubleValue DMG_LIFE_DRAIN;
    private static final ForgeConfigSpec.DoubleValue DMG_REWIND;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general").comment("战斗通用");
        DAMAGE_MULTIPLIER = b.comment("全局伤害倍率：乘在 boss 所有直接伤害上（1.0 = 原版数值）",
                "整合包觉得 boss 伤害不够看就调高，例如 1.5")
                .defineInRange("damageMultiplier", 1.0D, 0.05D, 100.0D);
        MULTIPLAYER_MECHANICS = b.comment("多人机制检测开关：false = 关闭检测，",
                "荆棘共生等\"多人专属\"机制单人也会释放")
                .define("multiplayerMechanics", true);
        HALO_STYLE = b.comment("玩家\"万花万草之灵\"神环（r91 起两套都是 GeckoLib geo 模型）：",
                "PIXELATED=像素版 geo/item/armor/frilias_back_ring_pix.geo.json + textures/entity/frilias_back_ring.png（默认），",
                "HIGH_RES=高清版 geo/item/armor/frilias_back_ring.geo.json + textures/entity/back_ring_3.png（1024×1024），",
                "仅影响玩家身上的神环渲染，boss 自身光环不受影响。")
                .defineEnum("haloStyle", HaloStyle.PIXELATED);
        SHOW_FRONT_RING = b.comment("神环前面镜像：true = 玩家前面也渲染一层（第一人称可见），false = 仅背面",
                "默认 true。")
                .define("showFrontRing", true);
        SHOW_HUD = b.comment("技能冷却 HUD 显示开关",
                "true = 显示（默认），false = 隐藏")
                .define("showHud", true);
        HUD_X = b.comment("HUD 相对屏幕水平中心的像素偏移（右为正）",
                "在编辑模式（按住 Shift + H 进入）下拖动也会改这里")
                .defineInRange("hudX", 0, -1000, 1000);
        HUD_Y = b.comment("HUD 距离屏幕底部的像素偏移（向上为正）",
                "默认 80")
                .defineInRange("hudY", 80, 0, 1000);
        b.pop();

        b.push("defense").comment("Boss 防御（锁血限伤）");
        MAX_HIT_FRACTION = b.comment("单次伤害上限：占 boss 最大生命的比例（0.15 = 单击最多扣 15% 最大生命）",
                "调到 1.0 = 关闭锁血（不再限伤）")
                .defineInRange("maxHitFraction", 0.15D, 0.01D, 1.0D);
        TIME_MAX_HIT_FRACTION = b.comment("时间阶段单次伤害上限：占 boss 最大生命的比例（0.08 = 单击最多扣 8%，比其它阶段更肉）")
                .defineInRange("timeMaxHitFraction", 0.08D, 0.01D, 1.0D);
        COMBO_PROTECT_ENABLED = b.comment("连击保护（无敌刻类衰减）开关：高频多源攻击伤害打折扣")
                .define("comboProtectionEnabled", true);
        COMBO_WINDOW_TICKS = b.comment("连击保护窗口（tick）：距上次受击不足该 tick 数视为连击")
                .defineInRange("comboWindowTicks", 5, 1, 100);
        COMBO_FACTOR = b.comment("连击窗口内的伤害折算系数（0.4 = 打 4 折；1.0 = 无衰减）")
                .defineInRange("comboFactor", 0.4D, 0.01D, 1.0D);
        b.pop();

        // ---- 基础 cd 值（tick，1 tick = 1/20 秒）----
        // 与下方 phase.*.cd.* 倍率叠加：最终触发间隔 = base × scale（最小 1 tick）
        // 一般玩家不用动这里；想直接调间隔而不改倍率，可改这些默认值。
        b.push("mechanics").comment("Boss 机制基础 cd（tick）。最终间隔 = base × 阶段倍率，最小 1 tick。");
        THORN_VOLLEY = b.comment("荆棘双弹 基础 cd").defineInRange("thornVolley", 200, 20, 6000);
        NATURE_BEAM = b.comment("花瓣光束（自然）基础 cd").defineInRange("natureBeam", 100, 20, 6000);
        FLOWER_CRADLE = b.comment("花之摇篮 基础 cd").defineInRange("flowerCradle", 240, 20, 6000);
        THORN_SYMBIOSIS = b.comment("荆棘共生 基础 cd").defineInRange("thornSymbiosis", 360, 20, 6000);
        BURN_LIFE_FIRE = b.comment("点燃附近花草 基础 cd").defineInRange("burnLifeFire", 200, 20, 6000);
        BURN_FIRE_SPOUT = b.comment("玩家脚下生焰 基础 cd").defineInRange("burnFireSpout", 80, 20, 6000);
        BURNING_SEED = b.comment("灼热之种 基础 cd").defineInRange("burningSeed", 200, 20, 6000);
        BURNING_SEED_FUSE = b.comment("灼热之种引信 基础 tick").defineInRange("burningSeedFuse", 60, 20, 6000);
        FLAME_KNOCKBACK = b.comment("烈焰击退 基础 cd").defineInRange("flameKnockback", 100, 20, 6000);
        FLAME_PILLAR = b.comment("光辉炎柱（焚尽阶段随机落点炎柱 AOE）基础 cd").defineInRange("flamePillar", 240, 20, 6000);
        FROST_ICE_SPIKE = b.comment("冰刺双弹 基础 cd").defineInRange("frostIceSpike", 200, 20, 6000);
        FROST_BEAM = b.comment("花瓣光束（霜华）基础 cd").defineInRange("frostBeam", 100, 20, 6000);
        FROST_GAZE = b.comment("霜凝之视 基础 cd").defineInRange("frostGaze", 240, 20, 6000);
        FROST_SIGIL = b.comment("冰晶符文环绕 基础 cd").defineInRange("frostSigil", 240, 20, 6000);
        FROST_MOON_RING = b.comment("钢铁月环（霜华阶段圆形/环形 AOE）基础 cd").defineInRange("frostMoonRing", 140, 20, 6000);
        RAIN_BLOOM = b.comment("天之泪雨滴 基础 cd").defineInRange("rainBloom", 200, 20, 6000);
        LIGHTNING = b.comment("落雷点名 基础 cd").defineInRange("lightning", 80, 20, 6000);
        RAIN_CLOUD = b.comment("雨云 基础 cd").defineInRange("rainCloud", 240, 20, 6000);
        RAIN_AOE = b.comment("云雨全屏 AOE 基础 cd").defineInRange("rainAoe", 480, 20, 6000);
        WIND_BLADE = b.comment("风刃 基础 cd").defineInRange("windBlade", 200, 20, 6000);
        SKY_SHOCKWAVE = b.comment("盘旋冲击波 基础 cd").defineInRange("skyShockwave", 80, 20, 6000);
        WIND_EYE = b.comment("风眼月环 基础 cd（偶尔独立释放时使用；击退后必接月环不受此 cd 限制）").defineInRange("windEye", 320, 20, 6000);
        FEATHER_SWORD = b.comment("羽剑冲锋 基础 cd").defineInRange("featherSword", 240, 20, 6000);
        VOID_RIFT = b.comment("虚空裂隙 基础 cd").defineInRange("voidRift", 100, 20, 6000);
        VOID_EROSION = b.comment("虚空侵蚀 基础 cd").defineInRange("voidErosion", 60, 20, 6000);
        VOID_TIDE = b.comment("虚空潮汐（环形/圆形地裂 AOE）基础 cd").defineInRange("voidTide", 240, 20, 6000);
        TIME_TIDE = b.comment("时间潮汐（环形/圆形地裂 AOE）基础 cd").defineInRange("timeTide", 240, 20, 6000);
        DEATH_FLOWER = b.comment("死亡之花复活 基础 cd").defineInRange("deathFlower", 240, 20, 6000);
        TIME_ROTATE = b.comment("时间阶段技能轮换 基础 cd").defineInRange("timeRotate", 240, 20, 6000);
        LIFE_DRAIN_CD = b.comment("生命汲取 基础 cd").defineInRange("lifeDrainCd", 200, 20, 6000);
        TIME_STOP_CD = b.comment("时间停止 基础 cd").defineInRange("timeStopCd", 240, 20, 6000);
        REWIND_CD = b.comment("时光倒流 基础 cd").defineInRange("rewindCd", 360, 20, 6000);
        b.pop();

        // ---- r72：Boss 机制配置（八阶段选项卡）----
        // 每个选项卡（phase.nature / phase.burn / ... / phase.time）下：
        //   cd  = 内置 cd 倍率（0.5x ~ 2.0x）
        //   dmg = 伤害倍率（0.1x ~ 10.0x）

        // ---- 自然阶段 ----
        b.push("phase.nature").comment("自然阶段配置 — 调整该阶段所有机制的 cd 与伤害倍率");
        CD_NATURE_THORN_VOLLEY = scaleCd(b, "cd.thornVolley", "荆棘双弹 cd 倍率");
        DMG_NATURE_THORN_VOLLEY = scaleDmg(b, "dmg.thornVolley", "荆棘双弹 伤害倍率（命中基础 5 点）");
        CD_NATURE_BEAM = scaleCd(b, "cd.beam", "花瓣光束 cd 倍率");
        DMG_NATURE_BEAM = scaleDmg(b, "dmg.beam", "花瓣光束 伤害倍率（基础 3 点）");
        CD_NATURE_FLOWER_CRADLE = scaleCd(b, "cd.flowerCradle", "花之摇篮 cd 倍率");
        CD_NATURE_THORN_SYMBIOSIS = scaleCd(b, "cd.thornSymbiosis", "荆棘共生 cd 倍率");
        DMG_NATURE_THORN_SYMBIOSIS = scaleDmg(b, "dmg.thornSymbiosis", "荆棘共生 伤害倍率（基础 16 点平分）");
        b.pop();

        // ---- 焚尽阶段 ----
        b.push("phase.burn").comment("焚尽阶段配置");
        CD_BURN_LIFE_FIRE = scaleCd(b, "cd.lifeFire", "点燃附近花草 cd 倍率");
        CD_BURN_FIRE_SPOUT = scaleCd(b, "cd.fireSpout", "玩家脚下生焰 cd 倍率");
        CD_BURNING_SEED = scaleCd(b, "cd.burningSeed", "灼热之种 cd 倍率");
        CD_BURNING_SEED_FUSE = scaleCd(b, "cd.burningSeedFuse", "灼热之种引信倍率");
        DMG_BURNING_SEED = scaleDmg(b, "dmg.burningSeed", "灼热之种 伤害倍率（基础 8 点）");
        CD_FLAME_KNOCKBACK = scaleCd(b, "cd.flameKnockback", "烈焰击退 cd 倍率");
        DMG_FLAME_KNOCKBACK = scaleDmg(b, "dmg.flameKnockback", "烈焰击退 伤害倍率（基础 6 点）");
        CD_FLAME_PILLAR = scaleCd(b, "cd.flamePillar", "光辉炎柱 cd 倍率");
        DMG_FLAME_PILLAR = scaleDmg(b, "dmg.flamePillar", "光辉炎柱 伤害倍率（基础 12 花灵 + 6 爆炸）");
        b.pop();

        // ---- 霜华阶段 ----
        b.push("phase.frost").comment("霜华阶段配置");
        CD_FROST_ICE_SPIKE = scaleCd(b, "cd.iceSpike", "冰刺双弹 cd 倍率");
        DMG_FROST_ICE_SPIKE = scaleDmg(b, "dmg.iceSpike", "冰刺双弹 伤害倍率（基础 6 点）");
        CD_FROST_BEAM = scaleCd(b, "cd.beam", "花瓣光束 cd 倍率");
        DMG_FROST_BEAM = scaleDmg(b, "dmg.beam", "花瓣光束 伤害倍率（基础 4 点）");
        CD_FROST_GAZE = scaleCd(b, "cd.gaze", "霜凝之视 cd 倍率");
        DMG_FROST_GAZE = scaleDmg(b, "dmg.gaze", "霜凝之视 伤害倍率（基础 8 点冰伤）");
        CD_FROST_SIGIL = scaleCd(b, "cd.sigil", "冰晶符文环绕 cd 倍率");
        CD_FROST_MOON_RING = scaleCd(b, "cd.moonRing", "钢铁月环 cd 倍率");
        DMG_FROST_MOON_RING = scaleDmg(b, "dmg.moonRing", "钢铁月环 伤害倍率（基础 8 点）");
        b.pop();

        // ---- 云雨阶段 ----
        b.push("phase.rain").comment("云雨阶段配置");
        CD_RAIN_BLOOM = scaleCd(b, "cd.rainBloom", "天之泪雨滴 cd 倍率");
        CD_LIGHTNING = scaleCd(b, "cd.lightning", "落雷点名 cd 倍率");
        DMG_LIGHTNING = scaleDmg(b, "dmg.lightning", "落雷 伤害倍率（基础 6 / 翻倍 12）");
        CD_RAIN_CLOUD = scaleCd(b, "cd.rainCloud", "雨云 cd 倍率");
        CD_RAIN_AOE = scaleCd(b, "cd.rainAoe", "云雨全屏 AOE cd 倍率");
        DMG_RAIN_AOE = scaleDmg(b, "dmg.rainAoe", "云雨全屏 AOE 伤害倍率（基础 6 花灵 + 2 格击退）");
        b.pop();

        // ---- 天空阶段 ----
        b.push("phase.sky").comment("天空阶段配置");
        CD_WIND_BLADE = scaleCd(b, "cd.windBlade", "风刃 cd 倍率");
        DMG_WIND_BLADE = scaleDmg(b, "dmg.windBlade", "风刃 伤害倍率（基础 6 点）");
        CD_SKY_SHOCKWAVE = scaleCd(b, "cd.shockwave", "盘旋冲击波 cd 倍率");
        DMG_SKY_SHOCKWAVE = scaleDmg(b, "dmg.shockwave", "盘旋冲击波 伤害倍率（基础 3 点）");
        CD_WIND_EYE = scaleCd(b, "cd.windEye", "风眼月环 cd 倍率（仅独立释放时；击退后必接月环不受限）");
        DMG_WIND_EYE = scaleDmg(b, "dmg.windEye", "风眼月环 伤害倍率（基础 8 点）");
        CD_FEATHER_SWORD = scaleCd(b, "cd.featherSword", "羽剑冲锋 cd 倍率");
        DMG_FEATHER_SWORD = scaleDmg(b, "dmg.featherSword", "羽剑冲锋 伤害倍率（基础 8 点）");
        b.pop();

        // ---- 虚空阶段 ----
        b.push("phase.void").comment("虚空阶段配置");
        CD_VOID_RIFT = scaleCd(b, "cd.voidRift", "虚空裂隙 cd 倍率");
        DMG_VOID_RIFT = scaleDmg(b, "dmg.voidRift", "虚空裂隙 伤害倍率（基础 10 点）");
        CD_VOID_EROSION = scaleCd(b, "cd.voidErosion", "虚空侵蚀 cd 倍率");
        DMG_VOID_EROSION = scaleDmg(b, "dmg.voidErosion", "虚空侵蚀 伤害倍率（基础 4 点 + 失明）");
        CD_VOID_TIDE = scaleCd(b, "cd.voidTide", "虚空潮汐（环形/圆形地裂）cd 倍率");
        DMG_VOID_TIDE = scaleDmg(b, "dmg.voidTide", "虚空潮汐 伤害倍率（基础 8 点）");
        b.pop();

        // ---- 枯萎阶段 ----
        b.push("phase.decay").comment("枯萎阶段配置");
        CD_DEATH_FLOWER = scaleCd(b, "cd.deathFlower", "死亡之花复活 cd 倍率");
        b.pop();

        // ---- 时间阶段 ----
        b.push("phase.time").comment("时间阶段配置");
        CD_TIME_ROTATE = scaleCd(b, "cd.rotate", "阶段技能轮换 cd 倍率");
        CD_LIFE_DRAIN = scaleCd(b, "cd.lifeDrain", "生命汲取 cd 倍率");
        DMG_LIFE_DRAIN = scaleDmg(b, "dmg.lifeDrain", "生命汲取 倍率（吸血系数 25%）");
        CD_TIME_STOP = scaleCd(b, "cd.timeStop", "时间停止 cd 倍率");
        CD_REWIND = scaleCd(b, "cd.rewind", "时光倒流 cd 倍率");
        DMG_REWIND = scaleDmg(b, "dmg.rewind", "时光倒流 倍率（位置回溯秒数）");
        CD_TIME_TIDE = scaleCd(b, "cd.timeTide", "时间潮汐（环形/圆形地裂）cd 倍率");
        DMG_TIME_TIDE = scaleDmg(b, "dmg.timeTide", "时间潮汐 伤害倍率（基础 8 点）");
        b.pop();

        SPEC = b.build();
    }

    private static ForgeConfigSpec.DoubleValue scaleCd(ForgeConfigSpec.Builder b, String path, String comment) {
        return b.comment(comment, "倍率：0.5 = 触发间隔减半（更频繁），2.0 = 间隔翻倍（更慢）")
                .defineInRange(path, 1.0D, 0.5D, 2.0D);
    }

    private static ForgeConfigSpec.DoubleValue scaleDmg(ForgeConfigSpec.Builder b, String path, String comment) {
        return b.comment(comment, "倍率：0.1 = 10% 伤害（原值的 1/10），10.0 = 1000% 伤害（10 倍）")
                .defineInRange(path, 1.0D, 0.1D, 10.0D);
    }

    // ---- 读取入口（tick 内直读，Forge 缓存后开销可忽略）----

    /** 全局伤害倍率乘算：boss 所有直接伤害统一走这里。 */
    public static float dmg(float base) {
        return base * DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static boolean multiplayerMechanics() {
        return MULTIPLAYER_MECHANICS.get();
    }

    /** r74：玩家神环渲染样式。 */
    public static HaloStyle haloStyle() {
        return HALO_STYLE.get();
    }

    // ---- r78：HUD / 神环前面 配置读取 ----
    public static boolean showFrontRing() {
        return SHOW_FRONT_RING.get();
    }

    public static boolean showHud() {
        return SHOW_HUD.get();
    }

    public static int hudX() {
        return HUD_X.get();
    }

    public static int hudY() {
        return HUD_Y.get();
    }

    public static void setHudX(int v) {
        HUD_X.set(v);
    }

    public static void setHudY(int v) {
        HUD_Y.set(v);
    }

    public static float maxHitFraction() {
        return MAX_HIT_FRACTION.get().floatValue();
    }

    public static boolean comboProtectEnabled() {
        return COMBO_PROTECT_ENABLED.get();
    }

    public static int comboWindowTicks() {
        return COMBO_WINDOW_TICKS.get();
    }

    public static float comboFactor() {
        return COMBO_FACTOR.get().floatValue();
    }

    // ---- r72：cd 倍率读取（基础 cd × 倍率，最小 1 tick）----

    public static int thornVolley() { return Math.max(1, (int) (THORN_VOLLEY.get() * CD_NATURE_THORN_VOLLEY.get())); }
    public static int natureBeam() { return Math.max(1, (int) (NATURE_BEAM.get() * CD_NATURE_BEAM.get())); }
    public static int flowerCradle() { return Math.max(1, (int) (FLOWER_CRADLE.get() * CD_NATURE_FLOWER_CRADLE.get())); }
    public static int thornSymbiosis() { return Math.max(1, (int) (THORN_SYMBIOSIS.get() * CD_NATURE_THORN_SYMBIOSIS.get())); }
    public static int burnLifeFire() { return Math.max(1, (int) (BURN_LIFE_FIRE.get() * CD_BURN_LIFE_FIRE.get())); }
    public static int burnFireSpout() { return Math.max(1, (int) (BURN_FIRE_SPOUT.get() * CD_BURN_FIRE_SPOUT.get())); }
    public static int burningSeed() { return Math.max(1, (int) (BURNING_SEED.get() * CD_BURNING_SEED.get())); }
    public static int burningSeedFuse() { return Math.max(1, (int) (BURNING_SEED_FUSE.get() * CD_BURNING_SEED_FUSE.get())); }
    public static int flameKnockback() { return Math.max(1, (int) (FLAME_KNOCKBACK.get() * CD_FLAME_KNOCKBACK.get())); }
    /** r96：光辉炎柱 最终 cd。 */
    public static int flamePillar() { return Math.max(1, (int) (FLAME_PILLAR.get() * CD_FLAME_PILLAR.get())); }
    public static int frostIceSpike() { return Math.max(1, (int) (FROST_ICE_SPIKE.get() * CD_FROST_ICE_SPIKE.get())); }
    public static int frostBeam() { return Math.max(1, (int) (FROST_BEAM.get() * CD_FROST_BEAM.get())); }
    public static int frostGaze() { return Math.max(1, (int) (FROST_GAZE.get() * CD_FROST_GAZE.get())); }
    public static int frostSigil() { return Math.max(1, (int) (FROST_SIGIL.get() * CD_FROST_SIGIL.get())); }
    /** r96：钢铁月环 最终 cd。 */
    public static int frostMoonRing() { return Math.max(1, (int) (FROST_MOON_RING.get() * CD_FROST_MOON_RING.get())); }
    public static int rainBloom() { return Math.max(1, (int) (RAIN_BLOOM.get() * CD_RAIN_BLOOM.get())); }
    public static int lightning() { return Math.max(1, (int) (LIGHTNING.get() * CD_LIGHTNING.get())); }
    public static int rainCloud() { return Math.max(1, (int) (RAIN_CLOUD.get() * CD_RAIN_CLOUD.get())); }
    /** r96：云雨全屏 AOE 最终 cd。 */
    public static int rainAoe() { return Math.max(1, (int) (RAIN_AOE.get() * CD_RAIN_AOE.get())); }
    public static int windBlade() { return Math.max(1, (int) (WIND_BLADE.get() * CD_WIND_BLADE.get())); }
    public static int skyShockwave() { return Math.max(1, (int) (SKY_SHOCKWAVE.get() * CD_SKY_SHOCKWAVE.get())); }
    public static int windEye() { return Math.max(1, (int) (WIND_EYE.get() * CD_WIND_EYE.get())); }
    public static int featherSword() { return Math.max(1, (int) (FEATHER_SWORD.get() * CD_FEATHER_SWORD.get())); }
    public static int voidRift() { return Math.max(1, (int) (VOID_RIFT.get() * CD_VOID_RIFT.get())); }
    public static int voidErosion() { return Math.max(1, (int) (VOID_EROSION.get() * CD_VOID_EROSION.get())); }
    /** r90：虚空潮汐 最终 cd（基础 × 倍率）。 */
    public static int voidTide() { return Math.max(1, (int) (VOID_TIDE.get() * CD_VOID_TIDE.get())); }
    /** r90：时间潮汐 最终 cd（基础 × 倍率）。 */
    public static int timeTide() { return Math.max(1, (int) (TIME_TIDE.get() * CD_TIME_TIDE.get())); }
    public static float dmgVoidErosion() { return DMG_VOID_EROSION.get().floatValue(); }
    /** r90：时间阶段单次伤害上限比例。r92 修复返回类型——调用方均按 float 使用。 */
    public static float timeMaxHitFraction() { return TIME_MAX_HIT_FRACTION.get().floatValue(); }
    public static int deathFlower() { return Math.max(1, (int) (DEATH_FLOWER.get() * CD_DEATH_FLOWER.get())); }
    public static int timeRotate() { return Math.max(1, (int) (TIME_ROTATE.get() * CD_TIME_ROTATE.get())); }
    public static int lifeDrainCd() { return Math.max(1, (int) (LIFE_DRAIN_CD.get() * CD_LIFE_DRAIN.get())); }
    public static int timeStopCd() { return Math.max(1, (int) (TIME_STOP_CD.get() * CD_TIME_STOP.get())); }
    public static int rewindCd() { return Math.max(1, (int) (REWIND_CD.get() * CD_REWIND.get())); }

    // ---- r72：dmg 倍率读取（基础伤害 × 倍率，再叠加全局 DAMAGE_MULTIPLIER）----

    public static float dmgThornVolley() { return DMG_NATURE_THORN_VOLLEY.get().floatValue(); }
    public static float dmgNatureBeam() { return DMG_NATURE_BEAM.get().floatValue(); }
    public static float dmgThornSymbiosis() { return DMG_NATURE_THORN_SYMBIOSIS.get().floatValue(); }
    public static float dmgBurningSeed() { return DMG_BURNING_SEED.get().floatValue(); }
    public static float dmgFlameKnockback() { return DMG_FLAME_KNOCKBACK.get().floatValue(); }
    /** r96：光辉炎柱 伤害倍率。 */
    public static float dmgFlamePillar() { return DMG_FLAME_PILLAR.get().floatValue(); }
    public static float dmgFrostIceSpike() { return DMG_FROST_ICE_SPIKE.get().floatValue(); }
    public static float dmgFrostBeam() { return DMG_FROST_BEAM.get().floatValue(); }
    public static float dmgFrostGaze() { return DMG_FROST_GAZE.get().floatValue(); }
    /** r96：钢铁月环 伤害倍率。 */
    public static float dmgFrostMoonRing() { return DMG_FROST_MOON_RING.get().floatValue(); }
    public static float dmgLightning() { return DMG_LIGHTNING.get().floatValue(); }
    /** r96：云雨全屏 AOE 伤害倍率。 */
    public static float dmgRainAoe() { return DMG_RAIN_AOE.get().floatValue(); }
    public static float dmgWindBlade() { return DMG_WIND_BLADE.get().floatValue(); }
    public static float dmgSkyShockwave() { return DMG_SKY_SHOCKWAVE.get().floatValue(); }
    public static float dmgWindEye() { return DMG_WIND_EYE.get().floatValue(); }
    public static float dmgFeatherSword() { return DMG_FEATHER_SWORD.get().floatValue(); }
    public static float dmgVoidRift() { return DMG_VOID_RIFT.get().floatValue(); }
    /** r90：虚空潮汐 伤害倍率。 */
    public static float dmgVoidTide() { return DMG_VOID_TIDE.get().floatValue(); }
    /** r90：时间潮汐 伤害倍率。 */
    public static float dmgTimeTide() { return DMG_TIME_TIDE.get().floatValue(); }
    public static float dmgLifeDrain() { return DMG_LIFE_DRAIN.get().floatValue(); }
    public static float dmgRewind() { return DMG_REWIND.get().floatValue(); }

    // ---- r85：游戏内配置界面的条目元数据（client/FRBConfigScreen 消费）----

    /** 单个配置条目的描述：显示名 + 取值/设值 + 类型 + 范围。 */
    public static final class Entry {
        public enum Kind { BOOL, INT, DOUBLE, ENUM }

        public final Kind kind;
        public final String label;
        public final String tip;
        public final ForgeConfigSpec.ConfigValue<?> value;
        public final double min;
        public final double max;

        private Entry(Kind kind, String label, String tip, ForgeConfigSpec.ConfigValue<?> value,
                      double min, double max) {
            this.kind = kind;
            this.label = label;
            this.tip = tip;
            this.value = value;
            this.min = min;
            this.max = max;
        }

        public String getText() {
            Object v = value.get();
            if (v instanceof Double d) return String.format("%.2f", d);
            return String.valueOf(v);
        }

        /** 解析并写入；越界自动钳制。返回是否成功。 */
        @SuppressWarnings("unchecked")
        public boolean setText(String s) {
            try {
                switch (kind) {
                    case BOOL -> ((ForgeConfigSpec.BooleanValue) value).set(Boolean.parseBoolean(s));
                    case INT -> {
                        int v = Integer.parseInt(s.trim());
                        v = (int) Math.max(min, Math.min(max, v));
                        ((ForgeConfigSpec.IntValue) value).set(v);
                    }
                    case DOUBLE -> {
                        double v = Double.parseDouble(s.trim());
                        v = Math.max(min, Math.min(max, v));
                        ((ForgeConfigSpec.DoubleValue) value).set(v);
                    }
                    case ENUM -> {
                        ForgeConfigSpec.EnumValue<HaloStyle> ev = (ForgeConfigSpec.EnumValue<HaloStyle>) value;
                        ev.set(HaloStyle.valueOf(s.trim()));
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** BOOL/ENUM 用：切换到下一个值。 */
        @SuppressWarnings("unchecked")
        public void cycle() {
            if (kind == Kind.BOOL) {
                ((ForgeConfigSpec.BooleanValue) value).set(!((ForgeConfigSpec.BooleanValue) value).get());
            } else if (kind == Kind.ENUM) {
                ForgeConfigSpec.EnumValue<HaloStyle> ev = (ForgeConfigSpec.EnumValue<HaloStyle>) value;
                ev.set(ev.get() == HaloStyle.PIXELATED ? HaloStyle.HIGH_RES : HaloStyle.PIXELATED);
            }
        }
    }

    /** 一个分类页（对应 toml 的一个节）。 */
    public record Category(String name, java.util.List<Entry> entries) {}

    private static Entry bool(String label, String tip, ForgeConfigSpec.BooleanValue v) {
        return new Entry(Entry.Kind.BOOL, label, tip, v, 0, 1);
    }

    private static Entry i(String label, String tip, ForgeConfigSpec.IntValue v, int min, int max) {
        return new Entry(Entry.Kind.INT, label, tip, v, min, max);
    }

    private static Entry d(String label, String tip, ForgeConfigSpec.DoubleValue v, double min, double max) {
        return new Entry(Entry.Kind.DOUBLE, label, tip, v, min, max);
    }

    private static Entry cd(String label, ForgeConfigSpec.DoubleValue v) {
        return d(label, "倍率 0.5~2.0（小=更频繁）", v, 0.5D, 2.0D);
    }

    private static Entry dm(String label, ForgeConfigSpec.DoubleValue v) {
        return d(label, "倍率 0.1~10.0", v, 0.1D, 10.0D);
    }

    /** 游戏内配置界面的全部分类与条目（与上方 static 块里的 push/pop 一一对应）。 */
    public static java.util.List<Category> categories() {
        java.util.List<Category> out = new java.util.ArrayList<>();
        out.add(new Category("通用", java.util.List.of(
                d("全局伤害倍率", "0.05~100，乘在 boss 所有直接伤害上", DAMAGE_MULTIPLIER, 0.05D, 100.0D),
                bool("多人机制检测", "关闭后多人专属机制单人也会释放", MULTIPLAYER_MECHANICS),
                new Entry(Entry.Kind.ENUM, "神环样式", "PIXELATED=像素 / HIGH_RES=高清", HALO_STYLE, 0, 1),
                bool("神环前面镜像", "第一人称可见的前环", SHOW_FRONT_RING),
                bool("技能冷却 HUD", "显示/隐藏", SHOW_HUD),
                i("HUD 水平偏移", "-1000~1000 像素", HUD_X, -1000, 1000),
                i("HUD 底部偏移", "0~1000 像素", HUD_Y, 0, 1000))));
        out.add(new Category("防御", java.util.List.of(
                d("单次伤害上限", "占最大生命比例 0.01~1.0（1.0=关闭锁血）", MAX_HIT_FRACTION, 0.01D, 1.0D),
                bool("连击保护", "高频多源攻击伤害打折", COMBO_PROTECT_ENABLED),
                i("连击窗口(tick)", "1~100", COMBO_WINDOW_TICKS, 1, 100),
                d("连击折算系数", "0.01~1.0（0.4=打4折）", COMBO_FACTOR, 0.01D, 1.0D))));
        out.add(new Category("基础CD", java.util.List.of(
                i("荆棘双弹", "tick，20~6000", THORN_VOLLEY, 20, 6000),
                i("花瓣光束(自然)", "tick", NATURE_BEAM, 20, 6000),
                i("花之摇篮", "tick", FLOWER_CRADLE, 20, 6000),
                i("荆棘共生", "tick", THORN_SYMBIOSIS, 20, 6000),
                i("点燃附近花草", "tick", BURN_LIFE_FIRE, 20, 6000),
                i("玩家脚下生焰", "tick", BURN_FIRE_SPOUT, 20, 6000),
                i("灼热之种", "tick", BURNING_SEED, 20, 6000),
                i("灼热之种引信", "tick", BURNING_SEED_FUSE, 20, 6000),
                i("烈焰击退", "tick", FLAME_KNOCKBACK, 20, 6000),
                i("冰刺双弹", "tick", FROST_ICE_SPIKE, 20, 6000),
                i("花瓣光束(霜华)", "tick", FROST_BEAM, 20, 6000),
                i("霜凝之视", "tick", FROST_GAZE, 20, 6000),
                i("冰晶符文环绕", "tick", FROST_SIGIL, 20, 6000),
                i("天之泪雨滴", "tick", RAIN_BLOOM, 20, 6000),
                i("落雷点名", "tick", LIGHTNING, 20, 6000),
                i("雨云", "tick", RAIN_CLOUD, 20, 6000),
                i("风刃", "tick", WIND_BLADE, 20, 6000),
                i("盘旋冲击波", "tick", SKY_SHOCKWAVE, 20, 6000),
                i("虚空裂隙", "tick", VOID_RIFT, 20, 6000),
                i("虚空侵蚀", "tick", VOID_EROSION, 20, 6000),
                i("死亡之花复活", "tick", DEATH_FLOWER, 20, 6000),
                i("时间技能轮换", "tick", TIME_ROTATE, 20, 6000),
                i("生命汲取", "tick", LIFE_DRAIN_CD, 20, 6000),
                i("时间停止", "tick", TIME_STOP_CD, 20, 6000),
                i("时光倒流", "tick", REWIND_CD, 20, 6000))));
        out.add(new Category("自然", java.util.List.of(
                cd("荆棘双弹 cd", CD_NATURE_THORN_VOLLEY), dm("荆棘双弹 伤害", DMG_NATURE_THORN_VOLLEY),
                cd("花瓣光束 cd", CD_NATURE_BEAM), dm("花瓣光束 伤害", DMG_NATURE_BEAM),
                cd("花之摇篮 cd", CD_NATURE_FLOWER_CRADLE),
                cd("荆棘共生 cd", CD_NATURE_THORN_SYMBIOSIS), dm("荆棘共生 伤害", DMG_NATURE_THORN_SYMBIOSIS))));
        out.add(new Category("焚尽", java.util.List.of(
                cd("点燃花草 cd", CD_BURN_LIFE_FIRE), cd("脚下生焰 cd", CD_BURN_FIRE_SPOUT),
                cd("灼热之种 cd", CD_BURNING_SEED), cd("灼热之种引信", CD_BURNING_SEED_FUSE),
                dm("灼热之种 伤害", DMG_BURNING_SEED),
                cd("烈焰击退 cd", CD_FLAME_KNOCKBACK), dm("烈焰击退 伤害", DMG_FLAME_KNOCKBACK),
                cd("光辉炎柱 cd", CD_FLAME_PILLAR), dm("光辉炎柱 伤害", DMG_FLAME_PILLAR))));
        out.add(new Category("霜华", java.util.List.of(
                cd("冰刺双弹 cd", CD_FROST_ICE_SPIKE), dm("冰刺双弹 伤害", DMG_FROST_ICE_SPIKE),
                cd("花瓣光束 cd", CD_FROST_BEAM), dm("花瓣光束 伤害", DMG_FROST_BEAM),
                cd("霜凝之视 cd", CD_FROST_GAZE), dm("霜凝之视 伤害", DMG_FROST_GAZE),
                cd("冰晶符文 cd", CD_FROST_SIGIL),
                cd("钢铁月环 cd", CD_FROST_MOON_RING), dm("钢铁月环 伤害", DMG_FROST_MOON_RING))));
        out.add(new Category("云雨", java.util.List.of(
                cd("雨之华 cd", CD_RAIN_BLOOM),
                cd("落雷 cd", CD_LIGHTNING), dm("落雷 伤害", DMG_LIGHTNING),
                cd("雨云 cd", CD_RAIN_CLOUD),
                cd("云雨全屏AOE cd", CD_RAIN_AOE), dm("云雨全屏AOE 伤害", DMG_RAIN_AOE))));
        out.add(new Category("天空", java.util.List.of(
                cd("风刃 cd", CD_WIND_BLADE), dm("风刃 伤害", DMG_WIND_BLADE),
                cd("冲击波 cd", CD_SKY_SHOCKWAVE), dm("冲击波 伤害", DMG_SKY_SHOCKWAVE),
                cd("风眼月环 cd", CD_WIND_EYE), dm("风眼月环 伤害", DMG_WIND_EYE),
                cd("羽剑冲锋 cd", CD_FEATHER_SWORD), dm("羽剑冲锋 伤害", DMG_FEATHER_SWORD))));
        out.add(new Category("虚空", java.util.List.of(
                cd("虚空裂隙 cd", CD_VOID_RIFT), dm("虚空裂隙 伤害", DMG_VOID_RIFT),
                cd("虚空侵蚀 cd", CD_VOID_EROSION), dm("虚空侵蚀 伤害", DMG_VOID_EROSION),
                cd("虚空潮汐 cd", CD_VOID_TIDE), dm("虚空潮汐 伤害", DMG_VOID_TIDE))));
        out.add(new Category("枯萎", java.util.List.of(
                cd("死亡之花复活 cd", CD_DEATH_FLOWER))));
        out.add(new Category("时间", java.util.List.of(
                cd("技能轮换 cd", CD_TIME_ROTATE),
                cd("生命汲取 cd", CD_LIFE_DRAIN), dm("生命汲取 倍率", DMG_LIFE_DRAIN),
                cd("时间停止 cd", CD_TIME_STOP),
                cd("时光倒流 cd", CD_REWIND), dm("时光倒流 倍率", DMG_REWIND),
                cd("时间潮汐 cd", CD_TIME_TIDE), dm("时间潮汐 伤害", DMG_TIME_TIDE))));
        return out;
    }

    private FRBConfig() {}
}