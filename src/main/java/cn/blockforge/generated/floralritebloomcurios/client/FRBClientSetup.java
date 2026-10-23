package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专属初始化入口。
 * <p>本类只能由客户端加载：主类通过 DistExecutor 间接引用，
 * 避免 Screen / Minecraft 等客户端类型出现在主类的方法签名中——专用服务器在构造 mod 时
 * 会反射解析主类的全部方法签名，一旦触碰客户端类型就会以
 * "Attempted to load class ... for invalid dist DEDICATED_SERVER" 崩溃。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FRBClientSetup {

    private FRBClientSetup() {}

    /** 构造期注册「模组列表 → 配置」入口（游戏内配置界面）。 */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new FRBConfigScreen(parent)));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        DummyDamageColorInjector.injectAtClientSetup();
    }
}
