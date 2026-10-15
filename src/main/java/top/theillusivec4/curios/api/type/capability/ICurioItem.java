package top.theillusivec4.curios.api.type.capability;

import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * 编译期桩：与真实 Curios 5.14.1+1.20.1 API 的 ICurioItem 中本模组覆写的
 * 方法签名完全一致，打包时会被排除，运行时由真正的 Curios 模组接口接管。
 */
public interface ICurioItem {
    default void curioTick(SlotContext slotContext, ItemStack stack) {
    }

    default void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    default void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    default boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    default boolean canEquip(String identifier, LivingEntity livingEntity, ItemStack stack) {
        return true;
    }

    default boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    default Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                         UUID uuid, ItemStack stack) {
        return com.google.common.collect.HashMultimap.create();
    }

    default ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                        int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.DEFAULT;
    }
}
