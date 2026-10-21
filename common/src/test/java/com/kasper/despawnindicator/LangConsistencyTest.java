package com.kasper.despawnindicator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against untranslated keys showing up in the game: every key the code uses must exist in every
 * language file, all language files must have the same keys and the same placeholders, and every language
 * {@link ServerText} offers must have a file.
 */
public class LangConsistencyTest {

    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path LANG = ROOT.resolve("common/src/main/resources/assets/despawnindicator/lang");

    private static Map<String, String> english;
    /** Every other language, by file name. */
    private static final Map<String, Map<String, String>> others = new TreeMap<>();
    private static String sources;

    @BeforeAll
    static void load() throws IOException {
        english = readLang(LANG.resolve("en_us.json"));
        try (Stream<Path> files = Files.list(LANG)) {
            for (Path f : files.filter(f -> f.toString().endsWith(".json") && !f.endsWith("en_us.json")).collect(Collectors.toList())) {
                others.put(f.getFileName().toString(), readLang(f));
            }
        }
        StringBuilder all = new StringBuilder();
        for (String dir : new String[]{"common/src/main", "shared", "versions"}) {
            Path p = ROOT.resolve(dir);
            if (!Files.isDirectory(p)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(p)) {
                for (Path f : files.filter(f -> f.toString().endsWith(".java") && !generated(p.relativize(f))).collect(Collectors.toList())) {
                    all.append(new String(Files.readAllBytes(f), StandardCharsets.UTF_8)).append('\n');
                }
            }
        }
        sources = all.toString();
    }

    /** Under a module's build folder (the Minecraft sources a loader unpacks, for one). */
    private static boolean generated(Path relative) {
        for (Path part : relative) {
            if (part.toString().equals("build") || part.toString().equals(".gradle")) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("Every language the server offers has a file, and every file is offered")
    void languagesHaveFiles() {
        Set<String> files = new TreeSet<>(others.keySet());
        files.add("en_us.json");
        Set<String> offered = Arrays.stream(ServerText.LANGUAGES).map(l -> l + ".json").collect(Collectors.toCollection(TreeSet::new));
        assertEquals(offered, files);
        assertTrue(files.size() >= 7);
    }

    @Test
    @DisplayName("All language files contain the same keys")
    void sameKeys() {
        assertTrue(others.containsKey("ru_ru.json"), "Russian is required");
        for (Map.Entry<String, Map<String, String>> lang : others.entrySet()) {
            assertEquals(new TreeSet<>(english.keySet()), new TreeSet<>(lang.getValue().keySet()), lang.getKey());
        }
    }

    @Test
    @DisplayName("Placeholders match between languages")
    void samePlaceholders() {
        for (Map.Entry<String, Map<String, String>> lang : others.entrySet()) {
            for (String key : english.keySet()) {
                assertEquals(placeholders(english.get(key)), placeholders(lang.getValue().get(key)),
                        "Placeholders differ for " + key + " in " + lang.getKey());
            }
        }
    }

    @Test
    @DisplayName("Every key used by the code is translated")
    void usedKeysExist() {
        Set<String> used = new TreeSet<>();
        Matcher full = Pattern.compile("\"((?:gui|key|command|text)\\.despawnindicator\\.[\\w.\\-]*)\"").matcher(sources);
        while (full.find()) {
            if (!full.group(1).endsWith(".")) {
                used.add(full.group(1));
            }
        }
        // Server texts are looked up without their prefix
        Matcher server = Pattern.compile("ServerText\\.get\\(\\s*[^,()]+(?:\\([^)]*\\))?,\\s*\"([\\w.]+)\"").matcher(sources);
        while (server.find()) {
            used.add(ServerText.PREFIX + server.group(1));
        }
        assertFalse(used.isEmpty(), "no keys found in the code: is the search still right?");
        List<String> missing = new ArrayList<>();
        for (String k : used) {
            if (!english.containsKey(k)) {
                missing.add(k);
            }
        }
        assertTrue(missing.isEmpty(), "Missing translations: " + missing);
    }

    /** How many placeholders a text has: "%s", "%1$s" (a translation may reorder them), "%d". */
    private static int placeholders(String s) {
        Matcher m = Pattern.compile("%(?:\\d+\\$)?[sd]").matcher(s);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Minimal reader for flat {"key": "value"} JSON files. */
    private static Map<String, String> readLang(Path file) throws IOException {
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Map<String, String> map = new TreeMap<>();
        Matcher m = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        while (m.find()) {
            assertNull(map.put(m.group(1), m.group(2)), "Duplicate key " + m.group(1) + " in " + file.getFileName());
        }
        assertFalse(map.isEmpty(), "No entries in " + file);
        assertEquals(map.keySet(), ServerText.parse(json).keySet(), "the server reads " + file.getFileName() + " the same way");
        return map;
    }
}
