package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public final class FRBCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FLORAL_TAB = TABS.register("floral_rite_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.frb"))
                    .icon(() -> new ItemStack(FRBItems.ETERNAL_SPIRIT.get()))
                    .displayItems((params, output) -> {
                        output.accept(FRBItems.PRIMORDIAL_PETAL.get());
                        output.accept(FRBItems.WORLD_FLOWER_WILL.get());
                        output.accept(FRBItems.FORMLESS_SEED.get());
                        output.accept(FRBItems.RITUAL_ALTAR.get());
                        output.accept(FRBItems.CREATIVE_RITUAL_ALTAR.get());
                        output.accept(FRBItems.FLORAL_ALTAR.get());
                        for (CharmType type : CharmType.ALL) {
                            output.accept(FRBItems.SEEDS.get(type).get());
                        }
                        for (CharmType type : CharmType.ALL) {
                            output.accept(FRBItems.CHARMS.get(type).get());
                        }
                        output.accept(FRBItems.SEED_OF_REALM.get());
                        output.accept(FRBItems.ETERNAL_SPIRIT.get());
                        output.accept(FRBItems.BOUNTIFUL.get());
                        output.accept(FRBItems.FRILIAS_EGG.get());
                        // r87：创造调试工具——右键 boss 强制进入下一阶段
                        output.accept(FRBItems.PHASE_SKIP_WAND.get());
                        // r77：月见草系列必须进创造栏搜索栏——
                        // 之前几个 build 漏了，玩家在游戏里搜不到。
                        output.accept(FRBItems.EVENING_PRIMROSE.get());
                        output.accept(FRBItems.EVENING_PRIMROSE_SEED.get());
                        output.accept(FRBItems.LUNAR_SHARD.get());
                        output.accept(FRBItems.LUNAR_CROWN.get());
                    })
                    .build());

    @SubscribeEvent
    public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        // 本模组使用独立创造栏，无需加入原版栏
    }

    private FRBCreativeTab() {}
}
