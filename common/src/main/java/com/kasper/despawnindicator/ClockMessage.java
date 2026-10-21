package com.kasper.despawnindicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server to client: the despawn clocks of items the player sees, all changes of one tick in one message.
 * <pre>
 *   byte    format (1)
 *   VarInt  count
 *   count x VarInt entity id, byte flags, and unless UNLIMITED: zigzag VarInt ticks left, VarInt lifespan
 * </pre>
 */
public final class ClockMessage {

    public static final int FORMAT = 1;
    /** Messages are split well below Minecraft's limit for one custom payload. */
    public static final int MAX_PER_MESSAGE = 2048;

    private ClockMessage() {
    }

    public static byte[] encode(List<ItemClock> clocks) {
        if (clocks.size() > MAX_PER_MESSAGE) {
            throw new IllegalArgumentException("Too many clocks for one message: " + clocks.size());
        }
        Bytes.Writer out = new Bytes.Writer().unsignedByte(FORMAT).varInt(clocks.size());
        for (ItemClock c : clocks) {
            out.varInt(c.entityId).unsignedByte(c.flags);
            if (!c.has(ItemClock.UNLIMITED)) {
                out.zigzag(c.remaining).varInt(c.lifespan);
            }
        }
        return out.toByteArray();
    }

    /** {@code clocks} in messages of at most {@link #MAX_PER_MESSAGE}. */
    public static List<byte[]> encodeAll(List<ItemClock> clocks) {
        List<byte[]> out = new ArrayList<>();
        for (int from = 0; from < clocks.size(); from += MAX_PER_MESSAGE) {
            out.add(encode(clocks.subList(from, Math.min(clocks.size(), from + MAX_PER_MESSAGE))));
        }
        return out;
    }

    /** The clocks in {@code data}; empty for a message of a format this build does not know or a broken one. */
    public static List<ItemClock> decode(byte[] data) {
        try {
            Bytes.Reader in = new Bytes.Reader(data);
            if (in.unsignedByte() != FORMAT) {
                return Collections.emptyList();
            }
            int count = in.varInt();
            if (count < 0 || count > MAX_PER_MESSAGE) {
                return Collections.emptyList();
            }
            List<ItemClock> clocks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int id = in.varInt();
                int flags = in.unsignedByte();
                if ((flags & ItemClock.UNLIMITED) != 0) {
                    clocks.add(new ItemClock(id, flags, 0, 0));
                } else {
                    clocks.add(new ItemClock(id, flags, in.zigzag(), in.varInt()));
                }
            }
            return clocks;
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
}
