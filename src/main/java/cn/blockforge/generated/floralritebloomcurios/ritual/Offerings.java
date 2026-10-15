package cn.blockforge.generated.floralritebloomcurios.ritual;

import cn.blockforge.generated.floralritebloomcurios.block.entity.FloralAltarBlockEntity;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 仪式供品来源：
 * · 无相花种与原始花瓣是“引子”，永远从玩家背包扣；
 * · 其余供品优先取祭坛 / 台座 8 格内的花仪台座，附近没有台座时退回玩家背包（兼容旧流程）。
 */
public interface Offerings {

    int count(Item item);

    boolean consume(Item item, int count);

    /** 台座上的供品（含被右键的那个台座）。 */
    final class Pedestals implements Offerings {
        private final List<FloralAltarBlockEntity> pedestals;

        Pedestals(List<FloralAltarBlockEntity> pedestals) {
            this.pedestals = pedestals;
        }

        @Override
        public int count(Item item) {
            int n = 0;
            for (FloralAltarBlockEntity be : pedestals) {
                if (be.getOffering().is(item)) n += be.getOffering().getCount();
            }
            return n;
        }

        @Override
        public boolean consume(Item item, int count) {
            if (count(item) < count) return false;
            int left = count;
            for (FloralAltarBlockEntity be : pedestals) {
                if (left <= 0) break;
                ItemStack stack = be.getOffering();
                if (!stack.is(item)) continue;
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
                be.setOffering(stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
            return left == 0;
        }
    }

    /** 引子（无相花种 / 原始花瓣）走玩家背包，其余供品走台座（没有台座则也走背包）。
     *  r73 修复：丰饶合成在台座列表非空但台座上空着、玩家背包里有花灵饰品时，count/consume
     *  只看台座找不到，导致「使用了但不消耗」——本类改为台座优先吃 + 玩家背包兜底。 */
    final class Combined implements Offerings {
        private final List<FloralAltarBlockEntity> pedestals;
        private final Player player;

        Combined(List<FloralAltarBlockEntity> pedestals, Player player) {
            this.pedestals = pedestals;
            this.player = player;
        }

        private boolean catalyst(Item item) {
            return item == FRBItems.FORMLESS_SEED.get() || item == FRBItems.PRIMORDIAL_PETAL.get();
        }

        @Override
        public int count(Item item) {
            if (catalyst(item)) return player.getInventory().countItem(item);
            // r73：台座 + 背包求和（之前只二选一）
            int total = 0;
            if (!pedestals.isEmpty()) total += new Pedestals(pedestals).count(item);
            total += player.getInventory().countItem(item);
            return total;
        }

        @Override
        public boolean consume(Item item, int count) {
            if (catalyst(item)) return RitualManager.consume(player, item, count);
            int left = count;
            // r73：先吃台座，台座不够再吃背包（之前二选一导致台座空时玩家物品完全不被吃）
            if (!pedestals.isEmpty()) {
                Offerings ped = new Pedestals(pedestals);
                int pedCount = ped.count(item);
                if (pedCount > 0) {
                    int take = Math.min(left, pedCount);
                    if (ped.consume(item, take)) left -= take;
                }
            }
            if (left > 0) {
                if (RitualManager.consume(player, item, left)) left = 0;
            }
            return left == 0;
        }
    }

    /** 收集祭坛 / 台座周围 8 格内的花仪台座。 */
    static List<FloralAltarBlockEntity> nearbyPedestals(ServerLevel level, BlockPos center) {
        List<FloralAltarBlockEntity> list = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (level.getBlockEntity(p) instanceof FloralAltarBlockEntity be) {
                list.add(be);
            }
        }
        return list;
    }

    /** 以玩家背包为唯一来源（/frb 命令与旧流程）。 */
    static Offerings of(Player player) {
        return new Combined(List.of(), player);
    }

    /** 供品来源：附近有花仪台座就吃台座，否则吃背包。 */
    static Offerings at(ServerLevel level, BlockPos center, Player player) {
        return new Combined(nearbyPedestals(level, center), player);
    }
}
