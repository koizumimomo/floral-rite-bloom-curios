package top.theillusivec4.curios.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * 编译期桩（compile-time stub）：与 Curios 5.14.1+1.20.1 真实 API 的
 * top.theillusivec4.curios.api.SlotContext 保持同样的成员签名，
 * 仅供本模组编译使用，打包时会被排除，运行时由真正的 Curios 模组提供。
 */
public final class SlotContext {
    public SlotContext(String identifier, LivingEntity entity, int index, boolean visible, boolean cosmetic) {
    }

    public String getIdentifier() { return null; }

    public int getIndex() { return 0; }

    public LivingEntity getWearer() { return null; }

    public String identifier() { return null; }

    public LivingEntity entity() { return null; }

    public int index() { return 0; }

    public boolean cosmetic() { return false; }

    public boolean visible() { return false; }
}
