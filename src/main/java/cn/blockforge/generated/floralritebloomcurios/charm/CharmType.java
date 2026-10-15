package cn.blockforge.generated.floralritebloomcurios.charm;

import net.minecraft.ChatFormatting;

/**
 * 七件花灵饰品的静态定义：物品 id、代表色（花瓣粒子 / 荧光花 / 法阵 / 血条协同色）、
 * 名字与描述标题使用的文本颜色（§ 码对应）、分段描述的行数。
 */
public enum CharmType {
    FROST_CHIME("frost_chime", 0x9fdcff, ChatFormatting.BLUE, 2),
    EMBER_VINE("ember_vine", 0xff7a3c, ChatFormatting.RED, 3),
    SKY_TEAR("sky_tear", 0x5f8dff, ChatFormatting.DARK_BLUE, 4),
    FOREST_WHISPER("forest_whisper", 0x74e88a, ChatFormatting.GREEN, 4),
    ASTRAL_BLOOM("astral_bloom", 0xc9a2ff, ChatFormatting.GOLD, 6),
    BLACK_SPIRIT("black_spirit", 0x8a5fb0, ChatFormatting.LIGHT_PURPLE, 3),
    SKY_PILLAR("sky_pillar", 0xd8fff2, ChatFormatting.AQUA, 3);

    public static final CharmType[] ALL = values();

    public final String id;
    public final int color;
    /** 名字 / 描述标题的颜色：寒风铃§9、焚生藤§c、天泪花§1、森语藤§a、星界花§6、黑灵草§5、天穹花§b。 */
    public final ChatFormatting textColor;
    /** frb.charm.&lt;id&gt;.line1..N 的行数。 */
    public final int descLines;
    /** r77：CharmTools 用 bit 索引做持久化开关位（按 ALL 序号）。 */
    public final int bitIndex;

    CharmType(String id, int color, ChatFormatting textColor, int descLines) {
        this.id = id;
        this.color = color;
        this.textColor = textColor;
        this.descLines = descLines;
        this.bitIndex = ordinal();
    }

    public String petalId() {
        return id + "_petal";
    }

    public int bit() {
        return 1 << bitIndex;
    }
}
