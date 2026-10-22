package com.kasper.despawnindicator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The game's log, which Minecraft 1.16.5 writes with Log4j: it has no SLF4J. */
final class LogSink implements ModLog.Sink {

    private final Logger log = LogManager.getLogger(DespawnIndicator.NAME);

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }
}
