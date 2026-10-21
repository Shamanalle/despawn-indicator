package com.kasper.despawnindicator;

/**
 * The mod's log. Each platform hands it its own logger at startup (slf4j on the loaders, the plugin's
 * logger on Bukkit); until then lines go to standard output.
 */
public final class ModLog {

    /** Where the lines go. */
    public interface Sink {
        void info(String message);

        void warn(String message);
    }

    private static volatile Sink sink = new Sink() {
        @Override
        public void info(String message) {
            System.out.println("[" + DespawnIndicator.NAME + "] " + message);
        }

        @Override
        public void warn(String message) {
            System.out.println("[" + DespawnIndicator.NAME + "] WARN " + message);
        }
    };

    private ModLog() {
    }

    public static void use(Sink s) {
        if (s != null) {
            sink = s;
        }
    }

    public static void info(String message) {
        sink.info(message);
    }

    public static void warn(String message) {
        sink.warn(message);
    }
}
