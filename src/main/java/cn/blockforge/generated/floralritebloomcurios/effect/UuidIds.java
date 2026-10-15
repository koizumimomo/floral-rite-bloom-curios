package cn.blockforge.generated.floralritebloomcurios.effect;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 1.20.1 的 MobEffect#addAttributeModifier(Attribute, String, ...) 内部会做
 * UUID.fromString(id)，id 必须是合法 UUID 字符串（1.21 才允许 "mod:name" 式可读 id）。
 * 本工具用名称派生稳定 UUID（名字相同则 UUID 恒定，可安全跨存档/重启使用），
 * 既保留可读语义，又满足 1.20.1 的格式要求。
 */
public final class UuidIds {

    private UuidIds() {}

    /** 1.20.1 旧版 MobEffect#addAttributeModifier 用：返回 UUID 字符串。 */
    public static String of(String name) {
        return UUID.nameUUIDFromBytes(
                ("floral_rite_bloom_curios:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** 1.20+ AttributeModifier(UUID, ...) 直接构造用：返回 UUID 实例。 */
    public static UUID uuid(String name) {
        return UUID.fromString(of(name));
    }
}
