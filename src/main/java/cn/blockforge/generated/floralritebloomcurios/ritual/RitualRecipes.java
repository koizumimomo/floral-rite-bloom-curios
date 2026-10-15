package cn.blockforge.generated.floralritebloomcurios.ritual;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * r84：仪式材料的<b>单一数据源</b>——RitualManager（游戏内校验/消耗）与
 * RitualJeiRecipe（JEI 展示）都从这里的定义读取。
 * 以后增删材料 / 调数量，只改本文件，判定与 JEI 页面同时自动生效，不再两处手写。
 *
 * 注意：所有方法按需构造（FRBItems 的 RegistryObject 在物品注册完成前不能 .get()，
 * 因此这里不用静态常量表，而是每次调用返回新定义——调用频率极低，无性能影响）。
 */
public final class RitualRecipes {

    /** 月相仪式类别（RitualManager 与 RitualJeiRecipe 共用）。 */
    public enum LunarKind { PRIMROSE_SEED, LUNAR_SHARD, LUNAR_CROWN }

    /**
     * 月相仪式定义。
     *
     * @param output      仪式产出
     * @param costs       祭坛台座 / 背包供品（校验 + 消耗共用）
     * @param catalyst    手持引子（null = 无；仪式开始消耗 1 个）
     * @param amethystEnv 是否需要环境：紫水晶簇×2 + 水×1（源）
     * @param ritualTicks 仪式读条时长
     */
    public record LunarDef(LunarKind kind, Item output, List<Rituals.Cost> costs,
                           @Nullable Item catalyst, boolean amethystEnv, int ritualTicks) {}

    /**
     * 丰饶合成（封印丰饶）定义。
     *
     * @param costs     固定供品（校验 + 消耗共用）
     * @param anyOneOf  任意其一（七种花灵饰品任选其一，消耗找到的第一件）
     */
    public record BountyDef(List<Rituals.Cost> costs, List<Item> anyOneOf) {}

    /** 月相仪式定义表入口。 */
    public static LunarDef lunar(LunarKind kind) {
        return switch (kind) {
            case PRIMROSE_SEED -> new LunarDef(kind, FRBItems.EVENING_PRIMROSE_SEED.get(),
                    List.of(new Rituals.Cost(FRBItems.FORMLESS_SEED.get(), 1),
                            new Rituals.Cost(FRBItems.PRIMORDIAL_PETAL.get(), 2),
                            new Rituals.Cost(FRBItems.LUNAR_SHARD.get(), 1)),
                    null, true, 40);
            case LUNAR_SHARD -> new LunarDef(kind, FRBItems.LUNAR_SHARD.get(),
                    List.of(), Items.AMETHYST_SHARD, false, 60);
            case LUNAR_CROWN -> new LunarDef(kind, FRBItems.LUNAR_CROWN.get(),
                    // 需求文本见 lang「frb.ritual.req.lunar_crown」：8 月见草 + 8 月之水晶碎片 + 手持引子
                    List.of(new Rituals.Cost(FRBItems.EVENING_PRIMROSE.get(), 8),
                            new Rituals.Cost(FRBItems.LUNAR_SHARD.get(), 8)),
                    FRBItems.LUNAR_SHARD.get(), true, 120);
        };
    }

    /** 丰饶合成：无相花种×1 + 原初花瓣×4 + 任意花灵饰品×1 → 封印的丰饶。 */
    public static BountyDef bountifulCraft() {
        List<Item> charms = new ArrayList<>();
        for (CharmType t : CharmType.ALL) {
            charms.add(FRBItems.charmOf(t));
        }
        return new BountyDef(
                List.of(new Rituals.Cost(FRBItems.FORMLESS_SEED.get(), 1),
                        new Rituals.Cost(FRBItems.PRIMORDIAL_PETAL.get(), 4)),
                charms);
    }

    /** 丰饶解封：世界之花的意志×1（封印丰饶为手持物，不在此列）。 */
    public static List<Rituals.Cost> unsealCosts() {
        return List.of(new Rituals.Cost(FRBItems.WORLD_FLOWER_WILL.get(), 1));
    }

    /** 七仪公共引子：无相花种×1 + 原初花瓣×2（Rituals.check 与 JEI 七仪条目共用）。 */
    public static List<Rituals.Cost> baseCosts() {
        return List.of(new Rituals.Cost(FRBItems.FORMLESS_SEED.get(), 1),
                new Rituals.Cost(FRBItems.PRIMORDIAL_PETAL.get(), 2));
    }

    private RitualRecipes() {}
}
