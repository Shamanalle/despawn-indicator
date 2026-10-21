package com.kasper.despawnindicator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClockMessageTest {

    @Test
    void roundTrip() {
        List<ItemClock> clocks = Arrays.asList(
                ItemClock.of(1, 0, 6000, false, false),
                ItemClock.of(70000, 5990, 6000, true, true),
                ItemClock.of(3, DespawnIndicator.UNLIMITED_AGE, 6000, false, false),
                // Forge's ItemExpireEvent can let an item live on past its lifespan for a moment
                new ItemClock(4, 0, -5, 6000));
        assertEquals(clocks, ClockMessage.decode(ClockMessage.encode(clocks)));
    }

    @Test
    void ofAge() {
        ItemClock c = ItemClock.of(9, 1200, 6000, false, true);
        assertEquals(4800, c.remaining);
        assertEquals(6000, c.lifespan);
        assertTrue(c.has(ItemClock.MINE));
        assertFalse(c.has(ItemClock.FROZEN));
        assertTrue(ItemClock.of(9, DespawnIndicator.UNLIMITED_AGE, 6000, false, false).has(ItemClock.UNLIMITED));
        assertTrue(ItemClock.of(9, 0, 0, false, false).has(ItemClock.UNLIMITED), "no lifespan: never despawns");
    }

    /** The exact bytes, which the Bukkit plugin and every loader must produce alike. */
    @Test
    void bytes() {
        byte[] b = ClockMessage.encode(Arrays.asList(new ItemClock(300, ItemClock.MINE, 100, 6000),
                new ItemClock(5, ItemClock.UNLIMITED, 0, 0)));
        assertArrayEquals(new byte[]{1, 2, (byte) 0xAC, 0x02, 4, (byte) 0xC8, 0x01, (byte) 0xF0, 0x2E, 5, 1}, b);
    }

    @Test
    void brokenOrUnknown() {
        assertTrue(ClockMessage.decode(new byte[0]).isEmpty());
        assertTrue(ClockMessage.decode(new byte[]{2, 0}).isEmpty(), "a newer format");
        assertTrue(ClockMessage.decode(new byte[]{1, 3, 1}).isEmpty(), "cut short");
        assertTrue(ClockMessage.decode(new byte[]{1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F}).isEmpty());
    }

    @Test
    void splitIntoMessages() {
        List<ItemClock> many = new ArrayList<>();
        for (int i = 0; i < ClockMessage.MAX_PER_MESSAGE * 2 + 5; i++) {
            many.add(ItemClock.of(i, i, 6000, false, false));
        }
        List<byte[]> messages = ClockMessage.encodeAll(many);
        assertEquals(3, messages.size());
        List<ItemClock> back = new ArrayList<>();
        messages.forEach(m -> back.addAll(ClockMessage.decode(m)));
        assertEquals(many, back);
        assertTrue(messages.get(0).length < 32767, "fits one custom payload");
    }

    @Test
    void hello() {
        HelloMessage h = HelloMessage.decode(new HelloMessage(1, "0.1.0").encode());
        assertNotNull(h);
        assertEquals(1, h.protocol);
        assertEquals("0.1.0", h.modVersion);
        assertNull(HelloMessage.decode(new byte[0]));
        assertNull(HelloMessage.decode(new byte[]{0}));
        assertEquals("", HelloMessage.decode(new byte[]{2}).modVersion, "a hello without a version is still one");
    }

    @Test
    void zigzag() {
        for (int v : new int[]{0, 1, -1, 63, -64, 6000, -6000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertEquals(v, new Bytes.Reader(new Bytes.Writer().zigzag(v).toByteArray()).zigzag());
        }
    }
}
