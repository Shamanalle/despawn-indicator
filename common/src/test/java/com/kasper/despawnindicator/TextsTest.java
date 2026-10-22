package com.kasper.despawnindicator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextsTest {

    @Test
    void timeLeft() {
        assertEquals("5:00", TimeText.format(6000, false));
        assertEquals("0:42", TimeText.format(840, false));
        assertEquals("0:01", TimeText.format(1, false), "rounded up");
        assertEquals("0:00", TimeText.format(0, false));
        assertEquals("0:00", TimeText.format(-20, false));
        assertEquals("≈4:59", TimeText.format(5980, true));
        assertEquals("1:00:00", TimeText.format(72000, false));
    }

    @Test
    void blinkingSpeedsUp() {
        double warn = 20;
        assertTrue(Blink.shown(30, warn));
        assertEquals(1, Blink.alpha(30, warn));
        assertEquals(1, Blink.scale(30, warn));
        assertEquals(0, Blink.phase(20, warn));
        // Once a second at the start, four times at the end: (1 + 4) / 2 * 20 = 50 blinks in all
        assertEquals(50, Blink.phase(0, warn), 1e-9);
        double early = Blink.phase(19, warn) - Blink.phase(20, warn);
        double late = Blink.phase(0, warn) - Blink.phase(1, warn);
        assertTrue(late > early * 3, "faster at the end");
        int hidden = 0;
        for (int i = 0; i < 2000; i++) {
            if (!Blink.shown(i / 100.0, warn)) {
                hidden++;
            }
            double a = Blink.alpha(i / 100.0, warn);
            assertTrue(a >= 0.25 - 1e-9 && a <= 1 + 1e-9);
            double scale = Blink.scale(i / 100.0, warn);
            assertTrue(scale >= 0.3 - 1e-9 && scale <= 1 + 1e-9);
        }
        // Full size when a blink starts, smallest halfway through it
        assertEquals(1, Blink.scale(0, warn), 1e-9);
        assertEquals(0.3, Blink.scale(-0.125, warn), 1e-9);
        assertTrue(hidden > 600 && hidden < 1000, "hidden about 40% of the time: " + hidden);
    }

    @Test
    void keepsBlinkingWhenTimeRunsOut() {
        double warn = 20;
        assertEquals(54, Blink.phase(-1, warn), 1e-9);
        int shown = 0;
        for (int i = 0; i < 100; i++) {
            if (Blink.shown(-i / 20.0, warn)) {
                shown++;
            }
        }
        assertTrue(shown > 40 && shown < 80, "still blinking: " + shown);
    }

    @Test
    void serverLanguage() {
        assertEquals("ru_ru", ServerText.language("ru"));
        assertEquals("ru_ru", ServerText.language("ru-RU"));
        assertEquals("pt_br", ServerText.language("pt_pt"));
        assertEquals("zh_cn", ServerText.language("zh_tw"));
        assertEquals("en_us", ServerText.language("xx_yy"));
        assertEquals("en_us", ServerText.language(null));
        assertTrue(ServerText.get("ru_ru", "status.title", "1.0").contains("1.0"));
        assertEquals(ServerText.PREFIX + "no_such_key", ServerText.get("en_us", "no_such_key"));
    }
}
