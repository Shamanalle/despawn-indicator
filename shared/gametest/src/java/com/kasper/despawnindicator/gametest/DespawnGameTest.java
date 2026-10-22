package com.kasper.despawnindicator.gametest;

import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.client.ItemLook;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drops items in front of the player in a real singleplayer world and checks the whole way from the server half
 * to the client: an old item gets its exact time left, an item that never despawns gets no timer, a fresh one
 * the full five minutes. The old diamond blinks; looking at an item and sneaking put time labels over them.
 * Then runs {@code /despawnindicator} as the player and checks the reply is real text. Screenshots are uploaded
 * by CI.
 */
public class DespawnGameTest implements FabricClientGameTest {

    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> RECEIVED.add(message.getString()));
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            // The chunks around the player are drawn by then
            context.waitTicks(60);
            // An old diamond (10 seconds left), an emerald that never despawns, a fresh gold ingot; nobody may pick them up
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^-1 ^1 ^3 "
                    + "{Item:{id:\"minecraft:diamond\",count:1},Age:5800s,PickupDelay:32767s,NoGravity:1b}");
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^ ^1 ^3 "
                    + "{Item:{id:\"minecraft:emerald\",count:1},Age:-32768s,PickupDelay:32767s,NoGravity:1b}");
            world.getServer().runCommand("execute as @a at @s run summon minecraft:item ^1 ^1 ^3 "
                    + "{Item:{id:\"minecraft:gold_ingot\",count:1},PickupDelay:32767s,NoGravity:1b}");
            context.waitTicks(20);
            context.runOnClient(DespawnGameTest::checkClocks);
            context.takeScreenshot("world-items");

            checkBlinking(context);

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
        }
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
            } else if (item.getItem().is(Items.DIAMOND) && (r.ticksLeft <= 100 || r.ticksLeft > 200)) {
                problems.add("wrong time left for " + what + " (expected 100 - 200)");
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
