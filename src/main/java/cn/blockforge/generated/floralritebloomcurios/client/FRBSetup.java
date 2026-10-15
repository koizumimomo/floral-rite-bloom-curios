package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * MOD 总线上的客户端注册：按键（可在 设置→按键绑定 修改）、实体渲染器、花瓣粒子。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FRBSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 生之焰附层：单面薄片 → translucent（若贴图含 alpha）/ cutout（无 alpha）
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks.LIFE_FIRE.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
            // 虚空侵蚀附层：同上
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks.VOID_EROSION.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
            // 永恒之门：cross 面片 + 透明区域，走 cutout
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks.ETERNAL_PORTAL.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
            for (CharmType type : CharmType.ALL) {
                ItemProperties.register(FRBItems.charmOf(type),
                        new ResourceLocation(FloralRiteBloom.MOD_ID, "sealed"),
                        (stack, level, entity, seed) -> stack.hasTag()
                                && stack.getTag().getBoolean(
                                cn.blockforge.generated.floralritebloomcurios.util.FRBState.TAG_SEALED)
                                ? 1.0F : 0.0F);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        // 资源包重载后丢弃冲击波着色器，下次渲染重新编译
        event.registerReloadListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(net.minecraft.server.packs.resources.ResourceManager manager,
                                   net.minecraft.util.profiling.ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void unused, net.minecraft.server.packs.resources.ResourceManager manager,
                                 net.minecraft.util.profiling.ProfilerFiller profiler) {
                FRBBossFxClient.invalidateShader();
                // 世界空间后处理（shockwave / wormhole），资源重载时重新编译
                cn.blockforge.generated.floralritebloomcurios.client.shader.FRBShaderEffects.invalidateShaders();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(FRBClient.WREATH_KEY);
        event.register(FRBClient.BLINK_KEY);
        event.register(FRBClient.FROST_RECALL_KEY);
        event.register(FRBClient.HUD_EDIT_KEY);
    }

    /** r77：HUD overlay 注册（3 个技能冷却槽）。 */
    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("frb_ability_hud", new AbilityHudOverlay());
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                cn.blockforge.generated.floralritebloomcurios.registry.FRBBlockEntities.FLORAL_ALTAR.get(),
                FloralAltarRenderer::new);
        event.registerEntityRenderer(FRBEntities.FRILIAS.get(), FriliasRenderer::new);
        event.registerEntityRenderer(FRBEntities.SPIKE.get(), SpikeRenderer::new);
        event.registerEntityRenderer(FRBEntities.SPIKE_VOLLEY.get(), SpikeVolleyRenderer::new);
        event.registerEntityRenderer(FRBEntities.VOID_RIFT.get(), VoidRiftRenderer::new);
        event.registerEntityRenderer(FRBEntities.DEATH_FLOWER.get(), DeathFlowerRenderer::new);
        event.registerEntityRenderer(FRBEntities.PETAL_VOLLEY.get(), PetalVolleyRenderer::new);
        event.registerEntityRenderer(FRBEntities.AOE_WARN_SIGIL.get(), AoeWarnSigilRenderer::new);
        event.registerEntityRenderer(FRBEntities.FLOWER_CRADLE_SIGIL.get(), FlowerCradleSigilRenderer::new);
        event.registerEntityRenderer(FRBEntities.FLOWER_CRADLE_SIGIL_FLOWER.get(), FlowerCradleSigilFlowerRenderer::new);
        event.registerEntityRenderer(FRBEntities.RAIN_CLOUD.get(), RainCloudRenderer::new);
        event.registerEntityRenderer(FRBEntities.WATER_RIPPLE.get(), WaterRippleRenderer::new);
        event.registerEntityRenderer(FRBEntities.WIND_BLADE.get(), WindBladeRenderer::new);
        event.registerEntityRenderer(FRBEntities.MILLE_FLORUM_BLACK_HOLE.get(), MilleFlorumBlackHoleRenderer::new);
        event.registerEntityRenderer(FRBEntities.GAZE_EYE.get(), FriliasGazeEyeRenderer::new);
        event.registerEntityRenderer(FRBEntities.FEATHER_SWORD.get(), FeatherSwordRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterDimensionEffects(
            net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        // 永恒花园异色天空（dimension_type JSON 的 effects 字段指向此 id）
        event.register(EternalGardenSky.EFFECTS_ID, new EternalGardenSky());
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new PlayerBackRingLayer(renderer));
                // r93：移除 PlayerFrontRingLayer——geo 模型双面渲染后前环已可见，不需要额外前层。
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FRBParticles.CONVERGING_PETAL.get(), ConvergingPetalParticle::provider);
        // 永恒花园环境花瓣（白染色保留 sprite 原色，garden_petal.json 已随机 4 种花瓣贴图）
        event.registerSpriteSet(FRBParticles.GARDEN_PETAL.get(),
                sprites -> PetalParticle.provider(sprites, 0xFFFFFF));
        // 丰饶尾迹：花瓣贴图 + 无重力 glow 渲染（原地淡出，不做下落）
        event.registerSpriteSet(FRBParticles.BOUNTIFUL_TRAIL.get(),
                s -> EldenParticle.glow(s, 0.20F, 10, -0.010F));
        for (CharmType type : CharmType.ALL) {
            event.registerSpriteSet(FRBParticles.PETALS.get(type).get(),
                    sprites -> PetalParticle.provider(sprites, type.color));
        }
        // Boss 演出粒子：开场三幕 / 脉冲环 / 主题微粒
        event.registerSpriteSet(FRBParticles.INTRO_SPIRAL.get(), s -> EldenParticle.glow(s, 0.5F, 40, -0.003F));
        event.registerSpriteSet(FRBParticles.INTRO_STAR.get(), s -> EldenParticle.glow(s, 0.4F, 45, -0.002F));
        event.registerSpriteSet(FRBParticles.INTRO_BLOOM.get(), s -> EldenParticle.glow(s, 1.3F, 26, 0.012F));
        event.registerSpriteSet(FRBParticles.INTRO_WISP.get(), s -> EldenParticle.glow(s, 1.8F, 70, 0.005F));
        event.registerSpriteSet(FRBParticles.PULSE_RING.get(), s -> EldenParticle.ring(s, 2.0F, 0.10F));
        event.registerSpriteSet(FRBParticles.ECLIPSE_RING.get(), s -> EldenParticle.ring(s, 3.0F, 0.13F));
        for (var mote : FRBParticles.THEME_MOTES) {
            event.registerSpriteSet(mote.get(), s -> EldenParticle.glow(s, 0.32F, 32, -0.002F));
        }
        // Boss 新机制粒子（用户提供的贴图）：落雷标记 / 灼热之种 / 烈焰击退 / 复活 / 虚空飞溅
        event.registerSpriteSet(FRBParticles.LIGHTNING_MARK.get(), LightningMarkParticle::provider);
        event.registerSpriteSet(FRBParticles.BURNING_SEED_MARK.get(), s -> EldenParticle.glow(s, 0.7F, 25, 0.02F));
        event.registerSpriteSet(FRBParticles.FLAME_KNOCKBACK_CHARGE.get(), s -> EldenParticle.glow(s, 0.5F, 24, -0.002F));
        event.registerSpriteSet(FRBParticles.REVIVE_WISP.get(), s -> EldenParticle.glow(s, 1.0F, 40, 0.01F));
        event.registerSpriteSet(FRBParticles.VOID_SLASH.get(), s -> EldenParticle.glow(s, 0.8F, 22, -0.003F));
        // 潮汐结算星芒：虚空（蓝紫星芒）/ 时间（金圣星），大尺寸慢收缩，保证伤害瞬间清晰可见
        event.registerSpriteSet(FRBParticles.TIDE_VOID_STAR.get(), s -> EldenParticle.glow(s, 0.7F, 28, -0.006F));
        event.registerSpriteSet(FRBParticles.TIDE_TIME_STAR.get(), s -> EldenParticle.glow(s, 0.7F, 28, -0.006F));
        // 霜凝之眼（boss 蓄力时眼前单颗定位，缓动 glow）
        event.registerSpriteSet(FRBParticles.FROST_EYE.get(), s -> EldenParticle.glow(s, 0.6F, 35, 0.0F));
        // 霜凝印记（r66：boss 头顶 gaze.png，用户提供的贴图；短生命周期，客户端每 2 tick 补一颗）
        event.registerSpriteSet(FRBParticles.GAZE_MARK.get(), s -> EldenParticle.glow(s, 0.9F, 16, 0.0F));
        // 时间阶段粒子（6 主题 + 沙漏/表盘/指针/刻度环）
        event.registerSpriteSet(FRBParticles.TIME_PHASE_01.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_PHASE_02.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_PHASE_03.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_PHASE_04.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_PHASE_05.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_PHASE_06.get(), s -> EldenParticle.glow(s, 0.5F, 36, -0.001F));
        event.registerSpriteSet(FRBParticles.TIME_HOURGLASS.get(), s -> EldenParticle.glow(s, 0.7F, 50, 0.0F));
        event.registerSpriteSet(FRBParticles.TIME_CLOCK_FACE.get(), s -> EldenParticle.glow(s, 0.9F, 60, 0.0F));
        event.registerSpriteSet(FRBParticles.TIME_CLOCK_HANDS.get(), s -> EldenParticle.glow(s, 0.6F, 24, 0.005F));
        event.registerSpriteSet(FRBParticles.TIME_TICK_RING.get(), s -> EldenParticle.ring(s, 1.6F, 0.08F));
    }

    private FRBSetup() {}
}
