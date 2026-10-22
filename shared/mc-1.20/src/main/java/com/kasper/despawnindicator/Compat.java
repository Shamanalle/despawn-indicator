package com.kasper.despawnindicator;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The few Minecraft calls of the shared code that changed in 1.19.4 or 1.20, as they are on 1.20 and later. A band of
 * an older Minecraft has its own Compat instead of this one.
 */
public final class Compat {

    private Compat() {
    }

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static ServerLevel level(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

    /** Tells the player who ran a command, and not the operators. */
    public static void reply(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, false);
    }

    /** Who dropped the item, by hand or when they died; null if nobody did or they are not in the world. */
    public static UUID ownerId(ItemEntity item) {
        Entity owner = item.getOwner();
        return owner == null ? null : owner.getUUID();
    }

    /** Whether the entity left its level: picked up, despawned, killed, or unloaded with its chunk. */
    public static boolean removed(Entity entity) {
        return entity.isRemoved();
    }

    /** Whether it left its level only because its chunk was unloaded, and so comes back with it. */
    public static boolean unloaded(Entity entity) {
        return entity.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK;
    }

    /** Whether the entities of the chunk (as {@code ChunkPos.asLong}) are loaded. */
    public static boolean entitiesLoaded(ServerLevel level, long chunk) {
        return level.areEntitiesLoaded(chunk);
    }

    /** Where the entity looks, in degrees. */
    public static float yRot(Entity entity) {
        return entity.getYRot();
    }
}
