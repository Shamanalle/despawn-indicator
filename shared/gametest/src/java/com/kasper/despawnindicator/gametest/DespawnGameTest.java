package com.kasper.despawnindicator.gametest;

import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.LootTracker;
import com.kasper.despawnindicator.client.ClientConfig;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.client.ItemLook;
import com.kasper.despawnindicator.server.Platform;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drops items in front of the player in a real singleplayer world and checks the whole way from the server half
 * to the client: an old item gets its exact time left, an item that never despawns gets no timer, a fresh one
 * the full five minutes. The old diamond blinks, and with the fading style shrinks and grows back instead;
 * looking at an item and sneaking put time labels over them.
 * Then runs {@code /despawnindicator} as the player and checks the reply is real text. Last the player dies away
 * from their spawn point: after the respawn the death loot timer shows the server's exact time, and picking the
 * items up takes it away. Screenshots are uploaded by CI.
 */
public class DespawnGameTest implements FabricClientGameTest {

    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> RECEIVED.add(message.getString()));
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            // The chunks around the player are drawn by then
            context.waitTicks(60);
            // An old diamond (15 seconds left), an emerald that never despawns, a fresh gold ingot; nobody may pick them up
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^-1 ^1 ^3 "
                    + "{Item:{id:\"minecraft:diamond\",count:1},Age:5700s,PickupDelay:32767s,NoGravity:1b}");
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^ ^1 ^3 "
                    + "{Item:{id:\"minecraft:emerald\",count:1},Age:-32768s,PickupDelay:32767s,NoGravity:1b}");
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^1 ^1 ^3 "
                    + "{Item:{id:\"minecraft:gold_ingot\",count:1},PickupDelay:32767s,NoGravity:1b}");
            context.waitTicks(20);
            context.runOnClient(DespawnGameTest::checkClocks);
            context.takeScreenshot("world-items");

            checkBlinking(context);
            checkFading(context);

            world.getServer().runCommand("execute as @a at @s anchored eyes run tp @s ~ ~ ~ facing entity "
                    + "@e[type=item,limit=1,sort=nearest,nbt={Item:{id:\"minecraft:gold_ingot\"}}] feet");
            context.waitTicks(10);
            context.takeScreenshot("label-look");
            context.runOnClient(client -> {
                expectLabel(client, Items.GOLD_INGOT, true);
                expectLabel(client, Items.DIAMOND, false);
            });

            context.getInput().holdKey(options -> options.keyShift);
            context.waitTicks(10);
            context.takeScreenshot("label-sneak");
            context.runOnClient(client -> {
                expectLabel(client, Items.GOLD_INGOT, true);
                expectLabel(client, Items.DIAMOND, true);
                expectLabel(client, Items.EMERALD, false);
            });
            context.getInput().releaseKey(options -> options.keyShift);
            context.waitTicks(5);
            context.runOnClient(client -> expectLabel(client, Items.DIAMOND, false));

            RECEIVED.clear();
            context.runOnClient(client -> client.player.connection.sendCommand(DespawnIndicator.MOD_ID));
            context.waitTicks(20);
            context.takeScreenshot("status-chat");
            checkStatus();

            checkDeathLoot(context, world);
        }
    }

    /** Dies 30 blocks from the spawn point with a few stacks, respawns, then gets the stacks back. */
    private static void checkDeathLoot(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("gamemode survival @a");
        world.getServer().runCommand("execute as @a at @s run spawnpoint @s ~ ~ ~");
        world.getServer().runCommand("give @a minecraft:cobblestone 5");
        world.getServer().runCommand("give @a minecraft:oak_log 3");
        world.getServer().runCommand("execute as @a at @s run tp @s ~30 ~ ~");
        world.getServer().runCommand("kill @a");
        context.waitForScreen(DeathScreen.class);
        context.waitTicks(30);
        context.takeScreenshot("death-screen");
        context.clickScreenButton("deathScreen.respawn");
        context.waitForScreen(null);
        context.waitTicks(20);
        context.takeScreenshot("death-loot-hud");
        context.runOnClient(client -> {
            LootTracker.Reading r = ClientTicker.LOOT.read();
            String what = r == null ? "none" : r.items + " stacks, " + (r.estimate ? "estimate " : "")
                    + (r.unlimited ? "unlimited" : r.ticksLeft + " ticks") + " at " + r.x + " " + r.y + " " + r.z;
            System.out.println("[gametest] death loot: " + what);
            if (r == null) {
                throw new AssertionError("No death loot timer after the respawn");
            }
            if (r.estimate || r.unlimited || r.ticksLeft < 5800 || r.ticksLeft > DespawnIndicator.VANILLA_LIFETIME) {
                throw new AssertionError("Wrong death loot timer: " + what);
            }
            double dx = r.x + 0.5 - client.player.getX();
            double dz = r.z + 0.5 - client.player.getZ();
            if (Math.sqrt(dx * dx + dz * dz) < 20) {
                throw new AssertionError("The death loot timer does not point where the player died: " + what);
            }
        });

        // The loot is the only thing 20 blocks away from the spawn point
        world.getServer().runCommand("execute as @a at @s run tp @e[type=item,distance=20..] @s");
        context.waitTicks(40);
        context.takeScreenshot("death-loot-picked-up");
        context.runOnClient(client -> {
            if (ClientTicker.LOOT.read() != null) {
                throw new AssertionError("The death loot timer stays after the items were picked up");
            }
        });
    }

    private static void checkClocks(Minecraft client) {
        List<String> problems = new ArrayList<>();
        int items = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item)) {
                continue;
            }
            items++;
            ClientClocks.Reading r = ClientTicker.CLOCKS.read(item.getId(), DespawnIndicator.VANILLA_LIFETIME);
            String what = item.getItem().getItem() + ": " + (r.estimate ? "estimate " : "") + (r.unlimited ? "unlimited" : r.ticksLeft + " ticks");
            System.out.println("[gametest] " + what);
            if (r.estimate) {
                problems.add("no time from the server for " + what);
            } else if (item.getItem().is(Items.DIAMOND) && (r.ticksLeft <= 200 || r.ticksLeft > 300)) {
                problems.add("wrong time left for " + what + " (expected 200 - 300)");
            } else if (item.getItem().is(Items.EMERALD) && !r.unlimited) {
                problems.add("a timer on an item that never despawns: " + what);
            } else if (item.getItem().is(Items.GOLD_INGOT) && (r.unlimited || r.ticksLeft < 5900)) {
                problems.add("wrong time left for a fresh item: " + what);
            }
        }
        if (items < 3) {
            problems.add("expected 3 items in the world, found " + items);
        }
        if (!problems.isEmpty()) {
            throw new AssertionError(String.join("; ", problems));
        }
    }

    /** The diamond has seconds left: over a second and a half it must be both hidden and shown. */
    private static void checkBlinking(ClientGameTestContext context) {
        int[] counts = new int[2];
        for (int i = 0; i < 30; i++) {
            context.runOnClient(client -> counts[ItemLook.hidden(find(client, Items.DIAMOND)) ? 0 : 1]++);
            context.waitTick();
        }
        System.out.println("[gametest] diamond hidden " + counts[0] + ", shown " + counts[1] + " of 30 ticks");
        if (counts[0] == 0 || counts[1] == 0) {
            throw new AssertionError("The diamond does not blink: hidden " + counts[0] + ", shown " + counts[1] + " of 30 ticks");
        }
        context.runOnClient(client -> {
            if (ItemLook.hidden(find(client, Items.GOLD_INGOT))) {
                throw new AssertionError("A fresh item blinks");
            }
        });
    }

    /**
     * Switches to the fading style in the player's settings file, as a player would: over a second and a half
     * the diamond must shrink and grow back, never be hidden, and be drawn smaller. Then switches back.
     */
    private static void checkFading(ClientGameTestContext context) {
        Path file = Platform.configDir().resolve(ClientSettings.FILE);
        String before = read(file);
        write(file, before.replace("style=blink", "style=fade"), 1);
        waitForStyle(context, ClientSettings.Style.FADE);

        float[] range = {Float.MAX_VALUE, -Float.MAX_VALUE};
        int[] hidden = new int[1];
        long[] shrunk = new long[1];
        context.runOnClient(client -> shrunk[0] = ItemLook.shrunk());
        for (int i = 0; i < 30; i++) {
            context.runOnClient(client -> {
                ItemEntity diamond = find(client, Items.DIAMOND);
                float scale = ItemLook.scale(diamond, 0);
                range[0] = Math.min(range[0], scale);
                range[1] = Math.max(range[1], scale);
                if (ItemLook.hidden(diamond)) {
                    hidden[0]++;
                }
            });
            context.waitTick();
        }
        context.takeScreenshot("world-items-fade");
        context.runOnClient(client -> shrunk[0] = ItemLook.shrunk() - shrunk[0]);
        System.out.println("[gametest] fading diamond: size " + range[0] + " - " + range[1] + ", hidden " + hidden[0]
                + " of 30 ticks, drawn smaller " + shrunk[0] + " times");
        if (range[1] - range[0] < 0.3F || range[0] < 0.3F - 1e-4F || range[1] > 1) {
            throw new AssertionError("The diamond does not shrink and grow back: size " + range[0] + " - " + range[1]);
        }
        if (hidden[0] > 0) {
            throw new AssertionError("A fading diamond was hidden on " + hidden[0] + " ticks");
        }
        if (shrunk[0] == 0) {
            throw new AssertionError("The renderer never drew the fading diamond smaller");
        }
        context.runOnClient(client -> {
            if (ItemLook.scale(find(client, Items.GOLD_INGOT), 0) != 1) {
                throw new AssertionError("A fresh item fades");
            }
        });

        write(file, before, 2);
        waitForStyle(context, ClientSettings.Style.BLINK);
    }

    /** The settings are read again when the file's time changes, so each write gets a new one. */
    private static void write(Path file, String text, int seconds) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + seconds * 1000L));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The file is looked at once a second. How items look is worked out at the end of a tick, so one more tick
     * passes after the new style is read: until then the items look as the old style had them.
     */
    private static void waitForStyle(ClientGameTestContext context, ClientSettings.Style style) {
        for (int i = 0; i < 60; i++) {
            if (context.computeOnClient(client -> ClientConfig.get().style) == style) {
                context.waitTick();
                return;
            }
            context.waitTick();
        }
        throw new AssertionError("The settings file was not read again: the style is not " + style);
    }

    private static void expectLabel(Minecraft client, Item which, boolean labeled) {
        ItemEntity item = find(client, which);
        Component name = item.getCustomName();
        String text = name == null ? null : name.getString();
        System.out.println("[gametest] label over " + which + ": " + text);
        if (!labeled) {
            if (text != null) {
                throw new AssertionError("Unexpected label over " + which + ": " + text);
            }
        } else if (text == null || !item.isCustomNameVisible()) {
            throw new AssertionError("No label over " + which);
        } else if (!text.matches("\\d+:\\d\\d")) {
            throw new AssertionError("Not an exact time over " + which + ": " + text);
        }
    }

    private static ItemEntity find(Minecraft client, Item which) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && item.getItem().is(which)) {
                return item;
            }
        }
        throw new AssertionError("No " + which + " in the world");
    }

    private static void checkStatus() {
        if (RECEIVED.stream().noneMatch(l -> l.startsWith(DespawnIndicator.NAME + " "))) {
            throw new AssertionError("No reply to /despawnindicator: " + RECEIVED);
        }
        for (String line : RECEIVED) {
            if (line.contains("command." + DespawnIndicator.MOD_ID + ".")) {
                throw new AssertionError("Untranslated text: " + line);
            }
        }
        if (RECEIVED.stream().noneMatch(l -> l.contains("1 of 1"))) {
            throw new AssertionError("The server does not count this client as one with the mod: " + RECEIVED);
        }
    }
}
