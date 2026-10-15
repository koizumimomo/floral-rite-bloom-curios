package cn.blockforge.generated.floralritebloomcurios.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 把带 § 颜色码的字面量解析成带样式的 Component。
 * 1.20.1 的语言文件不解析 § 码，逐字上色的名字（如万花万草之灵）必须在代码里构造。
 */
public final class LegacyText {

    /** 解析 §0-§9/§a-§f 颜色与 §l 粗体 / §o 斜体 / §r 重置；未知码忽略。 */
    public static MutableComponent parse(String text) {
        MutableComponent root = Component.empty();
        ChatFormatting color = null;
        boolean bold = false, italic = false, underlined = false;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (run.length() > 0) {
                    root.append(flush(run.toString(), color, bold, italic, underlined));
                    run.setLength(0);
                }
                ChatFormatting fmt = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(++i)));
                if (fmt == null) continue;
                if (fmt == ChatFormatting.RESET) {
                    color = null; bold = false; italic = false; underlined = false;
                } else if (fmt.isColor()) {
                    color = fmt;
                } else if (fmt == ChatFormatting.BOLD) {
                    bold = true;
                } else if (fmt == ChatFormatting.ITALIC) {
                    italic = true;
                } else if (fmt == ChatFormatting.UNDERLINE) {
                    underlined = true;
                }
            } else {
                run.append(c);
            }
        }
        if (run.length() > 0) {
            root.append(flush(run.toString(), color, bold, italic, underlined));
        }
        return root;
    }

    private static MutableComponent flush(String s, ChatFormatting color,
                                          boolean bold, boolean italic, boolean underlined) {
        MutableComponent c = Component.literal(s);
        if (color != null) c.withStyle(color);
        if (bold) c.withStyle(ChatFormatting.BOLD);
        if (italic) c.withStyle(ChatFormatting.ITALIC);
        if (underlined) c.withStyle(ChatFormatting.UNDERLINE);
        return c;
    }

    private LegacyText() {}
}
