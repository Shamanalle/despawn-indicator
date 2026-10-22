package com.kasper.despawnindicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The game's log, through SLF4J (Minecraft 1.17 and later). */
final class LogSink implements ModLog.Sink {

    private final Logger log = LoggerFactory.getLogger(DespawnIndicator.NAME);

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }
}
