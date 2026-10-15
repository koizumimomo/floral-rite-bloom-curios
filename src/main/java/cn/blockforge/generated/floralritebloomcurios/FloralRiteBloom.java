package cn.blockforge.generated.floralritebloomcurios;

import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBCreativeTab;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import cn.blockforge.generated.floralritebloomcurios.command.FRBCommand;

@Mod(FloralRiteBloom.MOD_ID)
public final class FloralRiteBloom {
    public static final String MOD_ID = "floral_rite_bloom_curios";

    public FloralRiteBloom() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        FRBItems.ITEMS.register(bus);
        FRBBlocks.BLOCKS.register(bus);
        FRBEffects.EFFECTS.register(bus);
        FRBParticles.PARTICLES.register(bus);
        FRBSounds.SOUNDS.register(bus);
        FRBEntities.ENTITY_TYPES.register(bus);
        cn.blockforge.generated.floralritebloomcurios.registry.FRBBlockEntities.BLOCK_ENTITIES.register(bus);
        FRBCreativeTab.TABS.register(bus);
        // r70：Boss 机制配置（config/frb_boss.toml）——冷却/伤害倍率/锁血/连击保护/多人检测
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                cn.blockforge.generated.floralritebloomcurios.config.FRBConfig.SPEC,
                "frb_boss.toml");
        // r78：所有配置整合到 FRBConfig 一个 spec（HUD / 神环前面 之前在 FRBClientConfig 里，已并入 FRBConfig）。
//      现在 mods 列表 → 配置按钮 → 打开的是 frb_boss.toml，里面包含 Boss 数值 + HUD + 神环全部选项。
        // FRBClientConfig 类已被删除；调用点统一改 FRBConfig。
        // r85：游戏内配置界面（模组列表 → 配置按钮直接打开 FRBConfigScreen）。
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                        net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                                (mc, parent) -> new cn.blockforge.generated.floralritebloomcurios.client.FRBConfigScreen(parent))));
        // 永恒花园的超平坦变体生成器：DeferredRegister 由 FML 在注册窗口期写入
        cn.blockforge.generated.floralritebloomcurios.world.EternalGardenChunkGenerator.CHUNK_GENERATORS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        // 训练假人（dummmmmmy）颜色注入：构造期写入其 toml（早于 config 加载，本局生效）
        cn.blockforge.generated.floralritebloomcurios.client.DummyDamageColorInjector.injectEarly();
        // 槽位归属走 Curios 内置 curios:tag 校验器（读物品标签 curios:flower_charm / curios:flower_eternal），
        // 不再自己注册谓词。
        bus.addListener(FRBEntities::registerAttributes);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FRBNetwork::init);
    }

    private void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        cn.blockforge.generated.floralritebloomcurios.client.DummyDamageColorInjector.injectAtClientSetup();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        FRBCommand.register(event.getDispatcher());
    }
}
