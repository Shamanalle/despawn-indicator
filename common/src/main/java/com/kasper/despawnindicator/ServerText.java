package com.kasper.despawnindicator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Texts the server sends (command replies), from the same language files as the client's screens: the
 * {@code command.despawnindicator.*} keys of {@code assets/despawnindicator/lang/<language>.json}. A player
 * without the mod has no such keys in their game, so the server picks the player's language and sends the text.
 */
public final class ServerText {

    public static final String PREFIX = "command." + DespawnIndicator.MOD_ID + ".";
    public static final String DEFAULT_LANGUAGE = "en_us";
    /** The languages that have a file. */
    public static final String[] LANGUAGES = {"en_us", "ru_ru", "uk_ua", "de_de", "es_es", "pt_br", "zh_cn",
            "fr_fr", "it_it", "pl_pl", "tr_tr", "ja_jp", "ko_kr"};

    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private ServerText() {
    }

    /**
     * The language file for a player's client language: "ru", "ru_ru", "ru-RU" and "RU" all give "ru_ru",
     * another country's variant of a known language (pt_pt, es_mx, de_at) gives that language, anything
     * unknown gives English.
     */
    public static String language(String code) {
        if (code == null || code.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        String c = code.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (String l : LANGUAGES) {
            if (l.equals(c) || l.startsWith(c + "_")) {
                return l;
            }
        }
        int u = c.indexOf('_');
        String base = u > 0 ? c.substring(0, u) : c;
        for (String l : LANGUAGES) {
            if (l.startsWith(base + "_")) {
                return l;
            }
        }
        return DEFAULT_LANGUAGE;
    }

    /** {@code key} (without the prefix) in {@code language}, with {@code %s} filled in; English when missing. */
    public static String get(String language, String key, Object... args) {
        String text = table(language(language)).get(PREFIX + key);
        if (text == null) {
            text = table(DEFAULT_LANGUAGE).get(PREFIX + key);
        }
        if (text == null) {
            text = PREFIX + key;
        }
        try {
            return args.length == 0 ? text : String.format(Locale.ROOT, text, args);
        } catch (RuntimeException e) {
            return text;
        }
    }

    private static Map<String, String> table(String language) {
        return CACHE.computeIfAbsent(language, ServerText::builtIn);
    }

    private static Map<String, String> builtIn(String language) {
        String path = "/assets/" + DespawnIndicator.MOD_ID + "/lang/" + language + ".json";
        try (InputStream in = ServerText.class.getResourceAsStream(path)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) {
                bytes.write(buffer, 0, n);
            }
            return parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            ModLog.warn("Could not read " + path + ": " + e);
            return Collections.emptyMap();
        }
    }

    /** Reads a flat JSON object of strings ({"key": "value", ...}), which is all a language file is. */
    static Map<String, String> parse(String json) {
        Map<String, String> out = new HashMap<>();
        int[] i = {json.indexOf('{') + 1};
        while (true) {
            skipSpace(json, i);
            if (i[0] >= json.length() || json.charAt(i[0]) == '}') {
                return out;
            }
            String key = string(json, i);
            skipSpace(json, i);
            expect(json, i, ':');
            skipSpace(json, i);
            out.put(key, string(json, i));
            skipSpace(json, i);
            if (i[0] < json.length() && json.charAt(i[0]) == ',') {
                i[0]++;
            }
        }
    }

    private static void skipSpace(String s, int[] i) {
        while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) {
            i[0]++;
        }
    }

    private static void expect(String s, int[] i, char c) {
        if (i[0] >= s.length() || s.charAt(i[0]) != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at " + i[0]);
        }
        i[0]++;
    }

    private static String string(String s, int[] i) {
        expect(s, i, '"');
        StringBuilder b = new StringBuilder();
        while (i[0] < s.length()) {
            char c = s.charAt(i[0]++);
            if (c == '"') {
                return b.toString();
            }
            if (c != '\\') {
                b.append(c);
                continue;
            }
            char e = s.charAt(i[0]++);
            switch (e) {
                case 'n':
                    b.append('\n');
                    break;
                case 't':
                    b.append('\t');
                    break;
                case 'r':
                    b.append('\r');
                    break;
                case 'b':
                    b.append('\b');
                    break;
                case 'f':
                    b.append('\f');
                    break;
                case 'u':
                    b.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16));
                    i[0] += 4;
                    break;
                default:
                    b.append(e);
            }
        }
        throw new IllegalArgumentException("Unterminated string");
    }
}
