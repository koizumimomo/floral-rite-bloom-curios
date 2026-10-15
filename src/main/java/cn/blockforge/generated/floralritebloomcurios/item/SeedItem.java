package cn.blockforge.generated.floralritebloomcurios.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 花种物品：让翻译键走 item.frb.&lt;id&gt;_seed，与方块本体的名字区分开。
 */
public class SeedItem extends BlockItem {
    private final String key;

    public SeedItem(Block block, String itemId, Properties props) {
        super(block, props);
        this.key = "item.frb." + itemId;
    }

    @Override
    public String getDescriptionId() {
        return this.key;
    }

    @Override
    public String getDescriptionId(net.minecraft.world.item.ItemStack stack) {
        return this.key;
    }
}
