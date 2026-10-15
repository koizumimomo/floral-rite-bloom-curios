package top.theillusivec4.curios.api;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/**
 * 编译期桩：与真实 Curios 5.14.1+1.20.1 API 的 CuriosApi 中本模组用到的
 * 成员签名一致，打包时会被排除，运行时由真正的 Curios 模组提供。
 */
public final class CuriosApi {
    public static final String MODID = "curios";

    public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity entity) {
        return LazyOptional.empty();
    }

    /** 真实 API：注册槽位校验谓词（槽位 JSON 的 validators 引用该 id）。 */
    public static void registerCurioPredicate(ResourceLocation id, Predicate<SlotResult> predicate) {
    }
}
