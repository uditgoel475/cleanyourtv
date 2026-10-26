package com.gathin.tvhome;

import java.util.Locale;

/** greeting_below_clock's title case, kept free of Android for unit tests. */
final class Greeting {
    private Greeting() {
    }

    /** "Good afternoon" as "Good Afternoon": the first letter of each word upper case, the rest as they are. */
    static String titleCase(String text, Locale locale) {
        StringBuilder out = new StringBuilder(text.length());
        boolean start = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(start ? String.valueOf(c).toUpperCase(locale) : String.valueOf(c));
            start = Character.isWhitespace(c);
        }
        return out.toString();
    }

}
