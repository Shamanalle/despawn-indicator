package com.kasper.despawnindicator;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * A settings file in the game's config folder, as "key=value" lines. Written with the defaults and their
 * explanations when it is missing, and read again whenever it changes, so edits apply without a restart.
 */
public final class ConfigFile {

    /** How often the file's time stamp is looked at, in milliseconds. */
    private static final long CHECK_EVERY = 1000;

    private final Path path;
    private final String defaults;
    private Properties properties;
    private long modified = Long.MIN_VALUE;
    private long checked = Long.MIN_VALUE;
    private boolean warned;

    /** {@code defaults} is the whole file as first written, comments included. */
    public ConfigFile(Path path, String defaults) {
        this.path = path;
        this.defaults = defaults;
        this.properties = parse(defaults);
    }

    /** The settings now; the defaults while the file cannot be read. */
    public synchronized Properties get() {
        long now = System.currentTimeMillis();
        if (now - checked < CHECK_EVERY && checked != Long.MIN_VALUE) {
            return properties;
        }
        checked = now;
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.write(path, defaults.getBytes(StandardCharsets.UTF_8));
            }
            long m = Files.getLastModifiedTime(path).toMillis();
            if (m != modified) {
                modified = m;
                try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Properties p = new Properties();
                    p.load(in);
                    properties = p;
                }
                warned = false;
            }
        } catch (IOException | RuntimeException e) {
            if (!warned) {
                warned = true;
                ModLog.warn("Could not read " + path + ", using the defaults: " + e);
            }
        }
        return properties;
    }

    public Path path() {
        return path;
    }

    static Properties parse(String text) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(text));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }

    /** {@code key} as a boolean; {@code fallback} when missing or neither "true" nor "false". */
    public static boolean bool(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key, "").trim();
        if (v.equalsIgnoreCase("true")) {
            return true;
        }
        return v.equalsIgnoreCase("false") ? false : fallback;
    }

    /** {@code key} as a whole number between {@code min} and {@code max}; {@code fallback} when not a number. */
    public static int integer(Properties p, String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty(key, "").trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** {@code key} as one of {@code values}, written in lower case; {@code fallback} when it is none of them. */
    public static <T extends Enum<T>> T choice(Properties p, String key, T fallback) {
        String v = p.getProperty(key, "").trim();
        for (T t : fallback.getDeclaringClass().getEnumConstants()) {
            if (t.name().equalsIgnoreCase(v)) {
                return t;
            }
        }
        return fallback;
    }
}
