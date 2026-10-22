package com.kasper.despawnindicator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {

    @Test
    void defaults() {
        ClientSettings c = ClientSettings.DEFAULT;
        assertTrue(c.deathHud);
        assertEquals(20, c.warnSeconds);
        assertEquals(ClientSettings.Style.BLINK, c.style);
        assertEquals(ClientSettings.Labels.LOOK_SNEAK, c.labels);
        assertFalse(c.sound);
        assertEquals(ServerSettings.Clocks.ALL, ServerSettings.DEFAULT.clocks);
    }

    @Test
    void parsesAndForgivesMistakes() {
        ClientSettings c = ClientSettings.parse(ConfigFile.parse(
                "death_hud = FALSE\nwarn_seconds=1000\nstyle=Label\nlabels=sneak\nsound=yes\n"));
        assertFalse(c.deathHud);
        assertEquals(ClientSettings.MAX_WARN, c.warnSeconds);
        assertEquals(ClientSettings.Style.LABEL, c.style);
        assertEquals(ClientSettings.Labels.SNEAK, c.labels);
        assertFalse(c.labels.look());
        assertTrue(c.labels.sneak());
        assertFalse(c.sound, "not a boolean: the default");
        assertEquals(20, ClientSettings.parse(ConfigFile.parse("warn_seconds=soon")).warnSeconds);
        assertEquals(ClientSettings.MIN_WARN, ClientSettings.parse(ConfigFile.parse("warn_seconds=-3")).warnSeconds);
        assertEquals(ClientSettings.Style.BLINK, ClientSettings.parse(ConfigFile.parse("style=sparkle")).style);
        assertEquals(ServerSettings.Clocks.OWN, ServerSettings.parse(ConfigFile.parse("clocks= own ")).clocks);
    }

    @Test
    void fileIsWrittenAndReread(@TempDir Path dir) throws IOException {
        Path path = dir.resolve("config").resolve(ClientSettings.FILE);
        ConfigFile file = new ConfigFile(path, ClientSettings.DEFAULTS);
        assertTrue(ClientSettings.parse(file.get()).deathHud);
        assertEquals(ClientSettings.DEFAULTS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        Files.write(path, "death_hud=false\n".getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(path, FileTime.fromMillis(Files.getLastModifiedTime(path).toMillis() + 5000));
        // Looked at again at most once a second
        assertTrue(ClientSettings.parse(file.get()).deathHud);
        ConfigFile fresh = new ConfigFile(path, ClientSettings.DEFAULTS);
        assertFalse(ClientSettings.parse(fresh.get()).deathHud);
    }
}
