package com.kasper.despawnindicator;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** The Minecraft calls of the shared code that changed in 1.17, 1.19, 1.19.4 and 1.20, as they are on 1.16.5. */
public final class Compat {

    private Compat() {
    }

    public static MutableComponent literal(String text) {
        return new TextComponent(text);
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.getLevel();
    }

    /** Tells the player who ran a command, and not the operators. */
    public static void reply(CommandSourceStack source, Component text) {
        source.sendSuccess(text, false);
    }

    /** Who dropped the item, by hand or when they died; null if nobody did. */
    public static UUID ownerId(ItemEntity item) {
        return item.getThrower();
    }

    /** Whether the entity left its level: picked up, despawned, killed, or unloaded with its chunk. */
    public static boolean removed(Entity entity) {
        return entity.removed || unloaded(entity);
    }

    /**
     * Whether it left its level only because its chunk was unloaded, and so comes back with it. Minecraft 1.16.5 does
     * not mark such an entity: its level just no longer holds it, and holds a new one once the chunk is back.
     */
    public static boolean unloaded(Entity entity) {
        return !entity.removed && entity.level instanceof ServerLevel
                && ((ServerLevel) entity.level).getEntity(entity.getUUID()) != entity;
    }

    /** Whether the entities of the chunk (as {@code ChunkPos.asLong}) are loaded: on 1.16.5, the chunk itself. */
    public static boolean entitiesLoaded(ServerLevel level, long chunk) {
        return level.getChunkSource().hasChunk((int) chunk, (int) (chunk >> 32));
    }

    /** Where the entity looks, in degrees. */
    public static float yRot(Entity entity) {
        return entity.yRot;
    }
}
