package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.AoeWarnSigilEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.DeathFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FeatherSwordEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilFlowerEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasGazeEyeEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.MilleFlorumBlackHoleEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.PetalVolleyEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.RainCloudEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.SpikeEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.SpikeVolleyEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.VoidRiftEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.WaterRippleEntity;
import cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FRBEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<EntityType<FriliasEntity>> FRILIAS =
            ENTITY_TYPES.register("frilias", () -> EntityType.Builder
                    .of(FriliasEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.5F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("frilias"));

    /** 荆棘 / 冰刺（延迟爆发地刺，建模渲染）。 */
    public static final RegistryObject<EntityType<SpikeEntity>> SPIKE =
            ENTITY_TYPES.register("spike", () -> EntityType.Builder
                    .<SpikeEntity>of(SpikeEntity::new, MobCategory.MISC)
                    .sized(0.9F, 1.6F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("spike"));

    /** 荆棘/冰刺飞弹（r64）：从 boss 左右两侧发射的非追踪子弹。 */
    public static final RegistryObject<EntityType<SpikeVolleyEntity>> SPIKE_VOLLEY =
            ENTITY_TYPES.register("spike_volley", () -> EntityType.Builder
                    .<SpikeVolleyEntity>of(SpikeVolleyEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("spike_volley"));

    /** 虚空裂隙（虚空阶段地面 hazard）。 */
    public static final RegistryObject<EntityType<VoidRiftEntity>> VOID_RIFT =
            ENTITY_TYPES.register("void_rift", () -> EntityType.Builder
                    .<VoidRiftEntity>of(VoidRiftEntity::new, MobCategory.MISC)
                    .sized(1.2F, 2.4F)
                    .clientTrackingRange(10)
                    .updateInterval(4)
                    .build("void_rift"));

    /** 死亡之花（枯萎阶段护命花，100HP / 6 护甲）。 */
    public static final RegistryObject<EntityType<DeathFlowerEntity>> DEATH_FLOWER =
            ENTITY_TYPES.register("death_flower", () -> EntityType.Builder
                    .<DeathFlowerEntity>of(DeathFlowerEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.4F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("death_flower"));

    /** 丰饶的花瓣 / 叶片弹（自动追踪，billboard 渲染）。 */
    public static final RegistryObject<EntityType<PetalVolleyEntity>> PETAL_VOLLEY =
            ENTITY_TYPES.register("petal_volley", () -> EntityType.Builder
                    .<PetalVolleyEntity>of(PetalVolleyEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("petal_volley"));

    /** 通用 AOE 预警法阵（r92）：荆棘/冰刺双弹落点提示，纯展示无结算。 */
    public static final RegistryObject<EntityType<AoeWarnSigilEntity>> AOE_WARN_SIGIL =
            ENTITY_TYPES.register("aoe_warn_sigil", () -> EntityType.Builder
                    .<AoeWarnSigilEntity>of(AoeWarnSigilEntity::new, MobCategory.MISC)
                    .sized(3.0F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("aoe_warn_sigil"));

    /** 花之摇篮法阵（B1 自然阶段，3 秒后落花）。 */
    public static final RegistryObject<EntityType<FlowerCradleSigilEntity>> FLOWER_CRADLE_SIGIL =
            ENTITY_TYPES.register("flower_cradle_sigil", () -> EntityType.Builder
                    .<FlowerCradleSigilEntity>of(FlowerCradleSigilEntity::new, MobCategory.MISC)
                    .sized(3.0F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("flower_cradle_sigil"));

    /** 花之摇篮落花（B1 法阵结算：从天上飘落至法阵）。 */
    public static final RegistryObject<EntityType<FlowerCradleSigilFlowerEntity>> FLOWER_CRADLE_SIGIL_FLOWER =
            ENTITY_TYPES.register("flower_cradle_sigil_flower", () -> EntityType.Builder
                    .<FlowerCradleSigilFlowerEntity>of(FlowerCradleSigilFlowerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("flower_cradle_sigil_flower"));

    /** 积雨云（E3 云雨阶段：缓慢漂移，给下方玩家附加潮湿）。 */
    public static final RegistryObject<EntityType<RainCloudEntity>> RAIN_CLOUD =
            ENTITY_TYPES.register("rain_cloud", () -> EntityType.Builder
                    .<RainCloudEntity>of(RainCloudEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.4F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("rain_cloud"));

    /** 万变水波（r96 云雨阶段：持久地面水面，踏入获得浸泡+雨之华）。 */
    public static final RegistryObject<EntityType<WaterRippleEntity>> WATER_RIPPLE =
            ENTITY_TYPES.register("water_ripple", () -> EntityType.Builder
                    .<WaterRippleEntity>of(WaterRippleEntity::new, MobCategory.MISC)
                    .sized(6.0F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("water_ripple"));

    /** 风刃（F2 天空阶段：非追踪直线弹，6 点物理伤害 + 3 格击退）。 */
    public static final RegistryObject<EntityType<WindBladeEntity>> WIND_BLADE =
            ENTITY_TYPES.register("wind_blade", () -> EntityType.Builder
                    .<WindBladeEntity>of(WindBladeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("wind_blade"));

    /** 万花凋零黑洞（H2 枯萎终招）：悬浮在 boss 头顶的不可见锚点，配合 boss_finale.fsh 后处理。 */
    public static final RegistryObject<EntityType<MilleFlorumBlackHoleEntity>> MILLE_FLORUM_BLACK_HOLE =
            ENTITY_TYPES.register("mille_florum_black_hole", () -> EntityType.Builder
                    .<MilleFlorumBlackHoleEntity>of(MilleFlorumBlackHoleEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("mille_florum_black_hole"));

    /** r75：凝视之眼（霜华阶段 D1 蓄力时挂在 boss 头顶的大眼睛，1.5x，不旋转）。 */
    public static final RegistryObject<EntityType<FriliasGazeEyeEntity>> GAZE_EYE =
            ENTITY_TYPES.register("frilias_gaze_eye", () -> EntityType.Builder
                    .<FriliasGazeEyeEntity>of(FriliasGazeEyeEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("frilias_gaze_eye"));

    /** r86：羽剑冲锋（天空阶段 FF14 机制，60t 预警 + 16t 冲锋，直线红色预警带）。 */
    public static final RegistryObject<EntityType<FeatherSwordEntity>> FEATHER_SWORD =
            ENTITY_TYPES.register("feather_sword", () -> EntityType.Builder
                    .<FeatherSwordEntity>of(FeatherSwordEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("feather_sword"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FRILIAS.get(), FriliasEntity.attributes().build());
        event.put(DEATH_FLOWER.get(), DeathFlowerEntity.attributes().build());
    }

    private FRBEntities() {}
}
