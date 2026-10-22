package com.kasper.despawnindicator;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class LootTest {

    @Test
    void messageRoundTrip() {
        LootMessage m = new LootMessage("minecraft:the_nether", -120, -40, 3000000, 7, ItemClock.FROZEN, 4321, 6000);
        byte[] data = m.encode();
        assertTrue(LootMessage.is(data));
        assertEquals(m, LootMessage.decode(data));
        LootMessage unlimited = new LootMessage("minecraft:overworld", 1, 2, 3, 1, ItemClock.UNLIMITED, 0, 0);
        assertEquals(unlimited, LootMessage.decode(unlimited.encode()));
        assertSame(LootMessage.NONE, LootMessage.decode(LootMessage.NONE.encode()));
    }

    @Test
    void clockClientsSkipLoot() {
        // A client that only knows clock messages reads nothing from a loot message, and the other way round
        byte[] loot = new LootMessage("minecraft:overworld", 0, 64, 0, 1, 0, 6000, 6000).encode();
        assertEquals(Collections.emptyList(), ClockMessage.decode(loot));
        byte[] clocks = ClockMessage.encode(Collections.singletonList(ItemClock.of(1, 0, 6000, false, false)));
        assertFalse(LootMessage.is(clocks));
        assertNull(LootMessage.decode(clocks));
        assertNull(LootMessage.decode(new byte[]{2, 1, 5}));
        assertNull(LootMessage.decode(new byte[0]));
    }

    @Test
    void serverLootCountsDown() {
        LootTracker t = new LootTracker();
        assertNull(t.read());
        t.receive(new LootMessage("minecraft:overworld", 10, 64, -5, 3, 0, 1000, 6000));
        for (int i = 0; i < 40; i++) {
            t.tick();
        }
        LootTracker.Reading r = t.read();
        assertEquals(960, r.ticksLeft);
        assertFalse(r.estimate);
        assertEquals(3, r.items);
        assertEquals(-5, r.z);
        t.receive(new LootMessage("minecraft:overworld", 10, 64, -5, 3, ItemClock.FROZEN, 900, 6000));
        t.tick();
        assertEquals(900, t.read().ticksLeft);
        assertTrue(t.read().frozen);
        t.receive(LootMessage.NONE);
        assertNull(t.read());
    }

    @Test
    void guessesWithoutServer() {
        LootTracker t = new LootTracker();
        t.tick();
        t.died("minecraft:overworld", 1, 2, 3);
        assertNotNull(t.death());
        assertNull(t.read());
        for (int i = 0; i < 40; i++) {
            t.tick();
        }
        t.found(5, 6000);
        assertNull(t.death());
        LootTracker.Reading r = t.read();
        assertTrue(r.estimate);
        // Counted from the death, not from when the items were found
        assertEquals(5960, r.ticksLeft);
        assertTrue(t.guessing());
        t.gone();
        assertNull(t.read());
        t.died("minecraft:overworld", 1, 2, 3);
        t.notFound();
        assertNull(t.death());
        assertNull(t.read());
    }

    @Test
    void serverStopsTheGuess() {
        LootTracker t = new LootTracker();
        t.died("minecraft:overworld", 1, 2, 3);
        t.receive(LootMessage.NONE);
        assertTrue(t.serverTells());
        assertNull(t.death());
        t.died("minecraft:overworld", 1, 2, 3);
        assertNull(t.death(), "the server says where the loot is");
        t.clear();
        assertFalse(t.serverTells());
    }

    @Test
    void levels() {
        assertEquals(LootHud.Level.CALM, LootHud.level(61 * 20));
        assertEquals(LootHud.Level.CALM, LootHud.level(60 * 20 + 1));
        assertEquals(LootHud.Level.SOON, LootHud.level(60 * 20));
        assertEquals(LootHud.Level.SOON, LootHud.level(15 * 20 + 1));
        assertEquals(LootHud.Level.URGENT, LootHud.level(15 * 20));
        assertEquals(LootHud.Level.URGENT, LootHud.level(0));
        assertEquals(1f, LootHud.alpha(ClientSettings.Style.LABEL, 10, false));
        assertEquals(1f, LootHud.alpha(ClientSettings.Style.BLINK, 10, true));
        assertEquals(1f, LootHud.alpha(ClientSettings.Style.BLINK, 16 * 20, false));
    }

    @Test
    void arrows() {
        // Facing south (+z)
        assertEquals('↑', LootHud.arrow(0, 10, 0));
        assertEquals('↓', LootHud.arrow(0, -10, 0));
        // West is -x, to the right of a player facing south
        assertEquals('→', LootHud.arrow(-10, 0, 0));
        assertEquals('←', LootHud.arrow(10, 0, 0));
        assertEquals('↗', LootHud.arrow(-10, 10, 0));
        // Facing west (yaw 90): west is ahead, south to the left
        assertEquals('↑', LootHud.arrow(-10, 0, 90));
        assertEquals('←', LootHud.arrow(0, 10, 90));
        // Yaw keeps growing past 360 while the player turns
        assertEquals('↑', LootHud.arrow(-10, 0, 90 + 720));
        assertEquals('↑', LootHud.arrow(-10, 0, 90 - 720));
        assertEquals(5, LootHud.distance(3, 0, 4));
    }

    @Test
    void dimensions() {
        assertEquals("minecraft:the_nether", Dimensions.id("ResourceKey[minecraft:dimension / minecraft:the_nether]"));
        assertEquals("odd", Dimensions.id("odd"));
        assertEquals("gui.despawnindicator.dimension.the_end", Dimensions.nameKey("minecraft:the_end"));
        assertNull(Dimensions.nameKey("aether:the_aether"));
        assertEquals("the_aether", Dimensions.path("aether:the_aether"));
        assertEquals("gui.despawnindicator.dimension.overworld", Dimensions.nameKey("minecraft:overworld"));
        assertEquals("gui.despawnindicator.dimension.the_nether", Dimensions.nameKey("minecraft:the_nether"));
    }
}
