package top.theillusivec4.curios.api.type.capability;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotResult;

/**
 * 编译期桩：与真实 Curios API 的 ICuriosItemHandler 中本模组用到的
 * 成员签名一致，打包时会被排除。
 */
public interface ICuriosItemHandler {
    boolean isEquipped(Item item);

    List<SlotResult> findCurios(String... identifiers);

    Optional<SlotResult> findFirstCurio(Item item);

    Optional<SlotResult> findFirstCurio(Predicate<ItemStack> predicate);
}
