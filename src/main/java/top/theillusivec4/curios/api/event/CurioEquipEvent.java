package top.theillusivec4.curios.api.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import top.theillusivec4.curios.api.SlotContext;

/**
 * 编译桩：真实类见 curios-forge-5.14.1+1.20.1，打包时由 jar exclude 排除。
 */
public class CurioEquipEvent extends LivingEvent {

    private final SlotContext slotContext;
    private final ItemStack stack;

    public CurioEquipEvent(ItemStack stack, SlotContext slotContext) {
        super(slotContext.entity());
        this.stack = stack;
        this.slotContext = slotContext;
    }

    public SlotContext getSlotContext() {
        return this.slotContext;
    }

    public ItemStack getStack() {
        return this.stack;
    }
}
