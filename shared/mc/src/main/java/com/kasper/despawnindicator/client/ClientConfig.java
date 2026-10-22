package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.ConfigFile;
import com.kasper.despawnindicator.server.Platform;

import java.util.Properties;

/** The player's settings (config/despawnindicator-client.properties), read again when the file changes. */
public final class ClientConfig {

    private static ConfigFile file;
    private static Properties parsed;
    private static ClientSettings settings = ClientSettings.DEFAULT;

    private ClientConfig() {
    }

    public static synchronized ClientSettings get() {
        if (file == null) {
            file = new ConfigFile(Platform.configDir().resolve(ClientSettings.FILE), ClientSettings.DEFAULTS);
        }
        Properties p = file.get();
        if (p != parsed) {
            parsed = p;
            settings = ClientSettings.parse(p);
        }
        return settings;
    }
}
