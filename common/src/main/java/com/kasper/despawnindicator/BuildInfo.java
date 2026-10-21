package com.kasper.despawnindicator;

import java.io.InputStream;
import java.util.Properties;

/** Build information baked into the jar by Gradle ({@code despawnindicator-build.properties}). */
public final class BuildInfo {

    private static final String VERSION = load();

    private BuildInfo() {
    }

    /** The mod version, e.g. {@code 0.1.0}. */
    public static String version() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = BuildInfo.class.getResourceAsStream("/despawnindicator-build.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p.getProperty("version", "unknown");
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
