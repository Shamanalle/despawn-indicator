package com.kasper.despawnindicator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClocksTest {

    @Test
    void exactCountsDown() {
        ClientClocks clocks = new ClientClocks();
        clocks.receive(Collections.singletonList(ItemClock.of(7, 5000, 6000, false, false)));
        for (int i = 0; i < 40; i++) {
            clocks.tick();
        }
        ClientClocks.Reading r = clocks.read(7, 6000);
        assertFalse(r.estimate);
        assertEquals(960, r.ticksLeft);
    }

    @Test
    void frozenStandsStill() {
        ClientClocks clocks = new ClientClocks();
        clocks.receive(Collections.singletonList(ItemClock.of(7, 5000, 6000, true, false)));
        clocks.tick();
        clocks.tick();
        assertEquals(1000, clocks.read(7, 6000).ticksLeft);
    }

    @Test
    void estimateStartsWhenFirstSeen() {
        ClientClocks clocks = new ClientClocks();
        clocks.tick();
        ClientClocks.Reading first = clocks.read(8, 6000);
        assertTrue(first.estimate);
        assertEquals(6000, first.ticksLeft);
        for (int i = 0; i < 100; i++) {
            clocks.tick();
        }
        assertEquals(5900, clocks.read(8, 6000).ticksLeft);
        // The server's word replaces the estimate
        clocks.receive(Collections.singletonList(ItemClock.of(8, 5900, 6000, false, true)));
        ClientClocks.Reading told = clocks.read(8, 6000);
        assertFalse(told.estimate);
        assertTrue(told.mine);
        assertEquals(100, told.ticksLeft);
    }

    @Test
    void unlimitedAndForgotten() {
        ClientClocks clocks = new ClientClocks();
        clocks.receive(Arrays.asList(ItemClock.of(1, DespawnIndicator.UNLIMITED_AGE, 6000, false, false),
                ItemClock.of(2, 0, 6000, false, false)));
        assertTrue(clocks.read(1, 6000).unlimited);
        clocks.retain(id -> id == 1);
        assertTrue(clocks.isExact(1));
        assertFalse(clocks.isExact(2));
        clocks.clear();
        assertEquals(0, clocks.exactCount());
    }

    @Test
    void senderOnlyForPlayersWithTheMod() {
        ClockSender<String> sender = new ClockSender<>();
        UUID with = UUID.randomUUID();
        UUID without = UUID.randomUUID();
        sender.hello(with, HelloMessage.ours());
        sender.queue(with, "a");
        sender.queue(with, "a");
        sender.queue(with, "b");
        sender.queue(without, "c");
        Map<UUID, List<String>> out = sender.drain();
        assertEquals(1, out.size());
        assertEquals(Arrays.asList("a", "b"), out.get(with));
        assertTrue(sender.drain().isEmpty());
        assertEquals(1, sender.playersWithMod());
        sender.left(with);
        assertFalse(sender.hasMod(with));
    }
}
