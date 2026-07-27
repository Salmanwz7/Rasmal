package com.example.rasmal.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the small subset of markdown LLMs commonly emit — bold, headers,
 * bullet lists, tables — into a CharSequence fit for a plain chat bubble.
 * {@code **bold**} becomes a real bold span; heading/table/list punctuation
 * that a TextView can't render is stripped rather than shown literally.
 * A defensive fallback for a system prompt that already asks for plain text.
 */
public final class MarkdownLite {

    private MarkdownLite() {}

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern HEADER = Pattern.compile("(?m)^#{1,6}\\s*");
    private static final Pattern BULLET = Pattern.compile("(?m)^\\s*[-*]\\s+");
    private static final Pattern TABLE_RULE = Pattern.compile("(?m)^\\s*\\|?[\\s:|-]+\\|[\\s:|-]*$\n?");

    public static CharSequence render(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String text = stripStructure(raw);

        SpannableStringBuilder out = new SpannableStringBuilder();
        Matcher m = BOLD.matcher(text);
        int last = 0;
        while (m.find()) {
            out.append(text, last, m.start());
            int boldStart = out.length();
            out.append(m.group(1));
            out.setSpan(new StyleSpan(Typeface.BOLD), boldStart, out.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = m.end();
        }
        out.append(text, last, text.length());
        return out;
    }

    /** Drops table separator rows, headers, and bullet markers a bubble can't render. */
    private static String stripStructure(String raw) {
        String s = TABLE_RULE.matcher(raw).replaceAll("");
        s = s.replaceAll("\\s*\\|\\s*", "  ");
        s = HEADER.matcher(s).replaceAll("");
        s = BULLET.matcher(s).replaceAll("• ");
        return s.trim();
    }
}
