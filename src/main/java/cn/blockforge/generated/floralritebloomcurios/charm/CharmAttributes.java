package cn.blockforge.generated.floralritebloomcurios.charm;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 七花饰品的静态属性加成（摘下即失效，由 curios 槽位自动管理）：
 * 寒风铃 +2 护甲 / +2 韧性，森语藤 +4 最大生命，黑灵草 +4 攻击伤害，
 * 天穹花 +0.4 攻击速度 / 免疫击退。终极饰品聚合全部七件。
 *
 * r74：焚生藤/天泪花/星界花原本 default {} 空分支 → Curios 不渲染
 *      "curios.modifiers.flower_charm" 头部（仅在饰品提供属性时才显示）。
 *      补一个数值 placeholder（max_health +0）让 Curios 始终展示头部，
 *      实际的机制效果（焚尽护盾/天泪花耐久回复/星界花附魔+2）走 CharmTickEvents/CharmBuffs 事件。
 */
public final class CharmAttributes {

    /** 把某个花灵的属性写入 map；uuidKey 区分佩戴者来源（花灵本体 / 终极聚合）。 */
    public static void apply(CharmType type, String uuidKey,
                             Multimap<Attribute, AttributeModifier> map) {
        switch (type) {
            case FROST_CHIME -> {
                put(map, uuidKey, "armor", Attributes.ARMOR, 2.0D);
                put(map, uuidKey, "toughness", Attributes.ARMOR_TOUGHNESS, 2.0D);
            }
            case FOREST_WHISPER -> put(map, uuidKey, "health", Attributes.MAX_HEALTH, 4.0D);
            case BLACK_SPIRIT -> put(map, uuidKey, "attack", Attributes.ATTACK_DAMAGE, 4.0D);
            case SKY_PILLAR -> {
                put(map, uuidKey, "attackspeed", Attributes.ATTACK_SPEED, 0.4D);
                put(map, uuidKey, "kbresist", Attributes.KNOCKBACK_RESISTANCE, 1.0D);
                // 天穹花：直接跨越一格高的台阶/方块（Forge step_height_addition 属性）
                put(map, uuidKey, "stepheight", net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.0D);
            }
            // r74：补 placeholder 让 Curios 渲染 "curios.modifiers.flower_charm" 头部；
            //      实际机制效果由 CharmTickEvents/CharmBuffs/CharmEvents 事件链路提供
            case EMBER_VINE, SKY_TEAR, ASTRAL_BLOOM ->
                put(map, uuidKey, "presence", Attributes.MAX_HEALTH, 0.0D);
        }
    }

    private static void put(Multimap<Attribute, AttributeModifier> map, String key,
                            String name, Attribute attr, double value) {
        UUID uuid = UUID.nameUUIDFromBytes(
                ("frb:" + key + ':' + name).getBytes(StandardCharsets.UTF_8));
        map.put(attr, new AttributeModifier(uuid, "frb_" + key + "_" + name, value,
                AttributeModifier.Operation.ADDITION));
    }

    private CharmAttributes() {}
}
