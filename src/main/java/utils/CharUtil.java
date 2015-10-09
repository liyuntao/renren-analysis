package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主要用于人人网昵称中一些影响swing展示的特殊符号的过滤
 * 形如 ♯♭ $ ஐﻬ◎ ﻬஐ の ☆→ あ ぃ ￡ ♥『』Ψ № ∑ ⌒〖〗＠ξζ∮ ▓ ∏ 卐【】√ ¤ ╋等2b字符
 */
public class CharUtil {

    private static final Logger log = LoggerFactory.getLogger(CharUtil.class);

    public static String removeIllegalChars(String input) {
        StringBuilder sb = new StringBuilder();
        char[] c = input.toCharArray();
        for (char cc : c) {
            if(isOk(cc)) sb.append(cc);
        }
        if(sb.length() == 0) {
            log.warn("好友{}经非法字符过滤后name为空白", input);
            return "sb";
        }
        return sb.toString();
    }

    private static boolean isOk(char c) {
        return Character.isLetterOrDigit(c) | isChinese(c);
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }
}
