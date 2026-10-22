package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.Dimensions;
import com.kasper.despawnindicator.LootHud;
import com.kasper.despawnindicator.LootTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The death loot timer on the client, apart from drawing it ({@link LootOverlay}). A server with the mod says
 * where the loot is; on other servers this guesses: when the player dies it looks for fresh items at that spot
 * a second later, and while the timer runs it checks now and then whether they are still there. It also
 * plays the warning sounds, when they are on.
 */
final class LootClient {

    /** How long after the death the items are looked for, in ticks. */
    private static final int LOOK_AFTER = 20;
    /** How long a death waits for the player to come back and look, in ticks. */
    private static final int GIVE_UP = 6000;
    /** How far from the death spot the items are looked for, in blocks. */
    private static final double SPREAD = 6;
    /** How close the player must be to the spot to see whether the items are there, in blocks. */
    private static final double SEE = 24;
    private static final int CHECK_EVERY = 20;

    private static boolean wasDead;
    private static LootHud.Level heard;

    private LootClient() {
    }

    static void tick(Minecraft mc, ClientLevel level, ClientSettings settings) {
        LootTracker loot = ClientTicker.LOOT;
        LocalPlayer player = mc.player;
        if (player != null) {
            boolean dead = player.isDeadOrDying();
            if (dead && !wasDead) {
                loot.died(dimension(level), (int) Math.floor(player.getX()), (int) Math.floor(player.getY()),
                        (int) Math.floor(player.getZ()));
            }
            wasDead = dead;
            if (!loot.serverTells()) {
                guess(loot, player, level);
            }
        }
        sound(loot.read(), settings);
    }

    private static void guess(LootTracker loot, LocalPlayer player, ClientLevel level) {
        LootTracker.Death death = loot.death();
        long now = loot.now();
        if (death != null) {
            long since = now - death.tick;
            if (since > GIVE_UP) {
                loot.notFound();
            } else if (since >= LOOK_AFTER && sees(player, level, death.dimension, death.x, death.z)) {
                List<ItemEntity> items = new ArrayList<>(items(level, death.x, death.y, death.z));
                // Dropped at the death: younger than the time since (items have their own client-side age)
                items.removeIf(item -> item.tickCount > since + 5);
                if (items.isEmpty()) {
                    loot.notFound();
                } else {
                    loot.found(items.size(), ClientPlatform.lifespan(items.get(0)));
                }
            }
            return;
        }
        if (!loot.guessing()) {
            return;
        }
        LootTracker.Reading r = loot.read();
        boolean sees = sees(player, level, r.dimension, r.x, r.z);
        if (sees && now % CHECK_EVERY == 0 && items(level, r.x, r.y, r.z).isEmpty()) {
            // Picked up, or despawned sooner than guessed
            loot.gone();
        } else if (!sees && !r.unlimited && r.ticksLeft == 0) {
            loot.gone();
        }
    }

    /** Whether the player is near enough the spot to see its items. */
    private static boolean sees(LocalPlayer player, ClientLevel level, String dimension, int x, int z) {
        if (!dimension.equals(dimension(level))) {
            return false;
        }
        double dx = player.getX() - (x + 0.5);
        double dz = player.getZ() - (z + 0.5);
        return dx * dx + dz * dz <= SEE * SEE;
    }

    private static List<ItemEntity> items(ClientLevel level, int x, int y, int z) {
        return level.getEntitiesOfClass(ItemEntity.class,
                new AABB(x - SPREAD, y - SPREAD, z - SPREAD, x + 1 + SPREAD, y + 1 + SPREAD, z + 1 + SPREAD));
    }

    /** A note when the timer turns yellow and another when it turns red. */
    private static void sound(LootTracker.Reading r, ClientSettings settings) {
        if (r == null || r.unlimited) {
            heard = null;
            return;
        }
        LootHud.Level now = LootHud.level(r.ticksLeft);
        if (heard != null && now.ordinal() > heard.ordinal() && settings.sound && !r.frozen) {
            ClientCompat.playNote(now == LootHud.Level.URGENT);
        }
        heard = now;
    }

    static String dimension(ClientLevel level) {
        return Dimensions.id(level.dimension().toString());
    }

    /** Left the server. */
    static void clear() {
        wasDead = false;
        heard = null;
    }
}
