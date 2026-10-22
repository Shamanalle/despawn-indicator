package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.ClockMessage;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ItemClock;
import com.kasper.despawnindicator.LootMessage;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PluginLogicTest {

    private static DeathLoot.Stack stack(int x, int age, int lifespan) {
        return new DeathLoot.Stack(UUID.randomUUID(), x, 64, 0, age, lifespan);
    }

    @Test
    void lootPointsAtTheFirstToDespawn() {
        List<DeathLoot.Stack> stacks = Arrays.asList(stack(1, 100, 6000), stack(2, 900, 6000), stack(3, 50, 1200));
        LootMessage m = DeathLoot.message("minecraft:overworld", stacks, 0);
        assertTrue(m.present);
        assertEquals(3, m.items);
        assertEquals(3, m.x);
        assertEquals(1150, m.remaining);
        assertEquals(1200, m.lifespan);
        assertEquals(0, m.flags);

        stacks.get(2).frozen = true;
        assertTrue(DeathLoot.message("minecraft:overworld", stacks, 0).has(ItemClock.FROZEN));
    }

    @Test
    void lootThatNeverDespawns() {
        List<DeathLoot.Stack> stacks = Arrays.asList(stack(5, DespawnIndicator.UNLIMITED_AGE, 6000), stack(6, 0, 0));
        LootMessage m = DeathLoot.message("minecraft:the_end", stacks, 0);
        assertTrue(m.has(ItemClock.UNLIMITED));
        assertEquals(5, m.x);
        assertEquals(2, m.items);
        assertSame(LootMessage.NONE, DeathLoot.message("minecraft:the_end", Collections.emptyList(), 0));
    }

    @Test
    void lootIsSentAgainOnlyWhenTheClientWouldBeWrong() {
        LootMessage sent = new LootMessage("minecraft:overworld", 0, 64, 0, 3, 0, 1000, 6000);
        // Counted down as the client does: nothing to say
        assertFalse(DeathLoot.changed(sent, 20, new LootMessage("minecraft:overworld", 0, 64, 0, 3, 0, 980, 6000)));
        assertFalse(DeathLoot.changed(sent, 20, new LootMessage("minecraft:overworld", 0, 64, 0, 3, 0, 982, 6000)));
        // The items stood still while nobody was near
        assertTrue(DeathLoot.changed(sent, 20, new LootMessage("minecraft:overworld", 0, 64, 0, 3, 0, 1000, 6000)));
        assertTrue(DeathLoot.changed(sent, 20, new LootMessage("minecraft:overworld", 0, 64, 0, 2, 0, 980, 6000)));
        assertTrue(DeathLoot.changed(sent, 20, new LootMessage("minecraft:overworld", 1, 64, 0, 3, 0, 980, 6000)));
        assertTrue(DeathLoot.changed(sent, 20, LootMessage.NONE));
        LootMessage frozen = new LootMessage("minecraft:overworld", 0, 64, 0, 3, ItemClock.FROZEN, 1000, 6000);
        assertFalse(DeathLoot.changed(frozen, 400, frozen));
    }

    @Test
    void dimensionsOfWorldsWithoutKeys() {
        assertEquals("minecraft:overworld", DeathLoot.dimension("world", World.Environment.NORMAL, "world"));
        assertEquals("minecraft:the_nether", DeathLoot.dimension("world_nether", World.Environment.NETHER, "world"));
        assertEquals("minecraft:the_end", DeathLoot.dimension("world_the_end", World.Environment.THE_END, "world"));
        assertEquals("minecraft:mining", DeathLoot.dimension("Mining", World.Environment.NORMAL, "world"));
        assertEquals("minecraft:world_nether", DeathLoot.dimension("world_nether", World.Environment.NORMAL, "world"));
    }

    @Test
    void itemsAreClockedWithinWhatThePlayerSees() {
        // Spigot's defaults: items tracked within 32 blocks, view distance 10
        assertEquals(31, Viewer.range(32, 10));
        // A short view distance shows less than the tracking range
        assertEquals(31, Viewer.range(64, 3));
        assertEquals(15, Viewer.range(16, 0));
        assertEquals(1, Viewer.range(0, 10));
    }

    @Test
    void theLargestMessageFitsEveryServer() {
        List<ItemClock> clocks = new ArrayList<>();
        for (int i = 0; i < Viewer.CLOCKS_PER_MESSAGE; i++) {
            clocks.add(ItemClock.of(Integer.MIN_VALUE + i, Integer.MAX_VALUE - i, Integer.MAX_VALUE, true, true));
        }
        byte[] data = ClockMessage.encode(clocks);
        // The size older servers allow for a plugin message to a player
        assertTrue(data.length < 32767, data.length + " bytes");
        assertEquals(clocks.size(), ClockMessage.decode(data).size());
    }

    @Test
    void channelsAreTheModsChannels() {
        // Bukkit wants namespaced channels, the mod listens on these
        assertEquals("despawnindicator:hello", DespawnIndicatorPlugin.HELLO);
        assertEquals("despawnindicator:clocks", DespawnIndicatorPlugin.CLOCKS);
    }

    @Test
    void lootIsNotSentAgainBetweenLooks() {
        // The items are looked at once a second; in between the timer runs on as the client's does
        List<DeathLoot.Stack> stacks = new ArrayList<>(Collections.singletonList(stack(0, 100, 6000)));
        LootMessage sent = DeathLoot.message("minecraft:overworld", stacks, 0);
        for (long tick = 1; tick < 40; tick++) {
            LootMessage now = DeathLoot.message("minecraft:overworld", stacks, tick);
            assertEquals(5900 - tick, now.remaining);
            assertFalse(DeathLoot.changed(sent, tick, now), "tick " + tick);
        }
    }
}
