package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.ClockMessage;
import com.kasper.despawnindicator.HelloMessage;
import com.kasper.despawnindicator.LootMessage;
import com.kasper.despawnindicator.LootTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The client half on every loader: counts the clocks down, says hello to a server with the mod, blinks and
 * labels the items ({@link ItemLook}), keeps the death loot timer ({@link LootClient}) and forgets items
 * that are gone. The loader glue calls these from its events.
 */
public final class ClientTicker {

    public static final ClientClocks CLOCKS = new ClientClocks();
    public static final LootTracker LOOT = new LootTracker();
    private static final int RETAIN_EVERY = 20;

    private static Object connection;
    private static ClientLevel level;
    private static boolean helloSent;
    private static int ticks;

    private ClientTicker() {
    }

    /** A message on the despawnindicator:clocks channel: item clocks or the death loot timer. */
    public static void receive(byte[] data) {
        if (LootMessage.is(data)) {
            LOOT.receive(LootMessage.decode(data));
        } else {
            CLOCKS.receive(ClockMessage.decode(data));
        }
    }

    /** The end of a client tick. */
    public static void tick(Minecraft mc) {
        Object now = mc.getConnection();
        if (now != connection) {
            connection = now;
            helloSent = false;
            CLOCKS.clear();
            ItemLook.clear();
            LOOT.clear();
            LootClient.clear();
        }
        if (mc.level != level) {
            // Another world or dimension: entity ids start over
            level = mc.level;
            CLOCKS.clear();
            ItemLook.clear();
        }
        if (now == null || level == null) {
            return;
        }
        if (!helloSent) {
            helloSent = ClientPlatform.trySendHello(HelloMessage.ours().encode());
        }
        if (!mc.isPaused()) {
            CLOCKS.tick();
            LOOT.tick();
        }
        if (++ticks % RETAIN_EVERY == 0) {
            ClientLevel l = level;
            CLOCKS.retain(id -> l.getEntity(id) != null);
        }
        ClientSettings settings = ClientConfig.get();
        ItemLook.tick(mc, level, settings);
        LootClient.tick(mc, level, settings);
    }
}
