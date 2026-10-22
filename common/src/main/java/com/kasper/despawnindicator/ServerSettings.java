package com.kasper.despawnindicator;

import java.util.Properties;

/** What a server owner can set, in config/despawnindicator-server.properties (plugins: their data folder). */
public final class ServerSettings {

    public static final String FILE = "despawnindicator-server.properties";

    /** Whose items players with the mod get exact times for. */
    public enum Clocks {
        /** Every item they see. */
        ALL,
        /** Only items they dropped themselves; other items show the client's estimate. */
        OWN
    }

    public static final String DEFAULTS = ""
            + "# Despawn Indicator, server settings. Changes apply within a second, no restart needed.\n"
            + "\n"
            + "# Whose items players with the mod see exact despawn times for: all (every item) or own (only\n"
            + "# items they dropped or lost when they died; other items show an estimate, marked with ≈).\n"
            + "clocks=all\n";

    public static final ServerSettings DEFAULT = parse(ConfigFile.parse(DEFAULTS));

    public final Clocks clocks;

    private ServerSettings(Clocks clocks) {
        this.clocks = clocks;
    }

    public static ServerSettings parse(Properties p) {
        return new ServerSettings(ConfigFile.choice(p, "clocks", Clocks.ALL));
    }
}
