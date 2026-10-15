package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.item.BountifulItem;
import cn.blockforge.generated.floralritebloomcurios.item.FlowerCharmItem;
import cn.blockforge.generated.floralritebloomcurios.item.UltimateBloomItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FRBItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FloralRiteBloom.MOD_ID);

    /** 七件花灵饰品（flower_charm 槽位）。 */
    public static final Map<CharmType, RegistryObject<FlowerCharmItem>> CHARMS = new EnumMap<>(CharmType.class);
    /** 七枚对应花灵的种子（种下后长成可收获的花）。 */
    public static final Map<CharmType, RegistryObject<Item>> SEEDS = new EnumMap<>(CharmType.class);

    static {
        for (CharmType type : CharmType.ALL) {
            CHARMS.put(type, ITEMS.register(type.id,
                    () -> new FlowerCharmItem(type, new Item.Properties().stacksTo(1))));
            SEEDS.put(type, ITEMS.register(type.id + "_seed",
                    () -> new cn.blockforge.generated.floralritebloomcurios.item.SeedItem(
                            FRBBlocks.FLOWERS.get(type).get(), type.id + "_seed", new Item.Properties())));
        }
    }

    // ---- 材料 ----
    public static final RegistryObject<Item> PRIMORDIAL_PETAL = ITEMS.register("primordial_petal",
            () -> new Item(new Item.Properties()));
    /** 世界之花 Boss 掉落的灵魂核心，用于合成终极饰品。 */
    public static final RegistryObject<Item> WORLD_FLOWER_WILL = ITEMS.register("world_flower_will",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> FORMLESS_SEED = ITEMS.register("formless_seed",
            () -> new Item(new Item.Properties()));
    // ---- 终极饰品（flower_eternal 专属槽位） ----
    public static final RegistryObject<UltimateBloomItem> ETERNAL_SPIRIT = ITEMS.register("eternal_spirit",
            () -> new UltimateBloomItem(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** 占位武器「丰饶」（绿色名字，手持属性 + 花瓣散射，后续替换正式模型）。 */
    public static final RegistryObject<BountifulItem> BOUNTIFUL = ITEMS.register("bountiful",
            () -> new BountifulItem(new Item.Properties().stacksTo(1)));

    /** r74：「月之庇护」金色头冠（Curios head 槽：+6 护甲 / +5 韧性 / 最终减伤 20%）。 */
    public static final RegistryObject<cn.blockforge.generated.floralritebloomcurios.item.LunarCrownItem> LUNAR_CROWN = ITEMS.register("lunar_crown",
            () -> new cn.blockforge.generated.floralritebloomcurios.item.LunarCrownItem(new Item.Properties().stacksTo(1)));

    /** r74：月见草作物收获物（仪式材料 / 一般素材）。 */
    public static final RegistryObject<Item> EVENING_PRIMROSE = ITEMS.register("evening_primrose",
            () -> new Item(new Item.Properties()));

    /** r74：月见草种子（右键耕地种植）。 */
    public static final RegistryObject<Item> EVENING_PRIMROSE_SEED = ITEMS.register("evening_primrose_seed",
            () -> new cn.blockforge.generated.floralritebloomcurios.item.SeedItem(
                    FRBBlocks.EVENING_PRIMROSE.get(), "evening_primrose_seed", new Item.Properties()));

    /** r74：月之水晶碎片（仪式产出，紫水晶碎片在祭坛上仪式 5~50% 概率获得）。 */
    public static final RegistryObject<Item> LUNAR_SHARD = ITEMS.register("lunar_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // ---- 方块物品 ----
    public static final RegistryObject<Item> RITUAL_ALTAR = ITEMS.register("ritual_altar",
            () -> new BlockItem(FRBBlocks.RITUAL_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> CREATIVE_RITUAL_ALTAR = ITEMS.register("creative_ritual_altar",
            () -> new BlockItem(FRBBlocks.CREATIVE_RITUAL_ALTAR.get(), new Item.Properties()));
    /** 永恒之门（创造用放置物品；正常途径由世界之花开门演出生成）。 */
    public static final RegistryObject<Item> ETERNAL_PORTAL = ITEMS.register("eternal_portal",
            () -> new BlockItem(FRBBlocks.ETERNAL_PORTAL.get(), new Item.Properties()));
    /** 花仪台座：tooltip 里以灰色斜体展示使用提示（frb.pedestal.hint）。 */
    public static final RegistryObject<Item> FLORAL_ALTAR = ITEMS.register("floral_altar",
            () -> new BlockItem(FRBBlocks.FLORAL_ALTAR.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            List<Component> tips, TooltipFlag flag) {
                    tips.add(Component.translatable("frb.pedestal.hint")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            });
    /** 手动召唤 Boss 的刷怪蛋（生存内正常途径仍是种下世界之花）。
     *  必须用 ForgeSpawnEggItem + 延迟 supplier：Forge 47.4.x 的分注册表 RegisterEvent
     *  里 entity_type 晚于 item 触发，物品注册期直接 FRILIAS.get() 会 NPE
     *  （Registry Object not present: frilias），连带 moonlight 等 mod 一起崩。 */
    public static final RegistryObject<Item> FRILIAS_EGG = ITEMS.register("frilias_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(() -> FRBEntities.FRILIAS.get(),
                    0x2b0f3a, 0xe07fd0, new Item.Properties()));
    public static final RegistryObject<Item> SEED_OF_REALM = ITEMS.register("seed_of_realm",
            () -> new cn.blockforge.generated.floralritebloomcurios.item.SeedItem(
                    FRBBlocks.WORLD_FLOWER.get(), "seed_of_realm", new Item.Properties()));

    /** r87：相位之杖（创造调试工具，右键 boss 强制进入下一阶段）。 */
    public static final RegistryObject<cn.blockforge.generated.floralritebloomcurios.item.PhaseSkipWandItem> PHASE_SKIP_WAND =
            ITEMS.register("frb_phase_wand",
                    () -> new cn.blockforge.generated.floralritebloomcurios.item.PhaseSkipWandItem(
                            new Item.Properties().stacksTo(1)));

    /** r100：不知道哪里来的酒——右键自饮 / 右键生物施加「喝醉了」效果。 */
    public static final RegistryObject<cn.blockforge.generated.floralritebloomcurios.item.MysteriousWineItem> MYSTERIOUS_WINE =
            ITEMS.register("mysterious_wine",
                    () -> new cn.blockforge.generated.floralritebloomcurios.item.MysteriousWineItem(
                            new Item.Properties().stacksTo(16)));

    public static Item charmOf(CharmType type) {
        return CHARMS.get(type).get();
    }

    public static Item seedOf(CharmType type) {
        return SEEDS.get(type).get();
    }

    public static boolean isFormlessSeed(ItemStack stack) {
        return stack.is(FORMLESS_SEED.get());
    }

    private FRBItems() {}
}
