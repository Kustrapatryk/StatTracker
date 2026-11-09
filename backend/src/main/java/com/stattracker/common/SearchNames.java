package com.stattracker.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalizes player names and search queries so that "jokic" finds "Nikola Jokić". */
public final class SearchNames {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SearchNames() {
    }

    public static String normalize(String text) {
        String withoutMarks = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        StringBuilder result = new StringBuilder(withoutMarks.length());
        for (char c : withoutMarks.toLowerCase(Locale.ROOT).toCharArray()) {
            // Letters that Unicode does not decompose into a base letter and a mark.
            switch (c) {
                case 'ł' -> result.append('l');
                case 'đ' -> result.append('d');
                case 'ø' -> result.append('o');
                case 'ı' -> result.append('i');
                case 'ß' -> result.append("ss");
                case 'æ' -> result.append("ae");
                default -> result.append(c);
            }
        }
        return WHITESPACE.matcher(result).replaceAll(" ").strip();
    }
}
