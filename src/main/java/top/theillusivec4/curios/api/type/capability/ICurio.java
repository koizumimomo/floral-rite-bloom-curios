package top.theillusivec4.curios.api.type.capability;

/**
 * 编译期桩：与真实 Curios API 的 ICurio 中本模组用到的成员签名一致，打包时排除。
 */
public interface ICurio {
    enum DropRule {
        DEFAULT,
        ALWAYS_DROP,
        ALWAYS_KEEP,
        DESTROY
    }
}
