package top.theillusivec4.curios.api;

import net.minecraft.world.item.ItemStack;

/**
 * 编译期桩：与真实 Curios API 的 SlotResult 同签名，打包时排除。
 */
public record SlotResult(SlotContext slotContext, ItemStack stack) {
}
