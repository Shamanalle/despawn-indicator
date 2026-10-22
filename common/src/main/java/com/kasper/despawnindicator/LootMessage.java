package com.kasper.despawnindicator;

/**
 * Server to client, on the clocks channel: where the items the player dropped when they last died are, and
 * how long the first of them has left. Sent when they die, every second while the items are there, once more
 * when the last one is gone, and on hello (then possibly "none", which tells the client the server keeps
 * track, so it does not guess on its own).
 * <pre>
 *   byte    format (2; a clock message starts with 1, and clients that only know 1 skip this one)
 *   byte    0 none, 1 loot
 *   if loot: string dimension id, zigzag VarInt x, y, z, VarInt items, byte flags,
 *            and unless UNLIMITED: zigzag VarInt ticks left, VarInt lifespan
 * </pre>
 */
public final class LootMessage {

    public static final int FORMAT = 2;
    private static final int MAX_DIMENSION_BYTES = 256;

    /** Whether there is loot; everything else is meaningless without it. */
    public final boolean present;
    public final String dimension;
    public final int x;
    public final int y;
    public final int z;
    /** How many stacks are left. */
    public final int items;
    /** {@link ItemClock#UNLIMITED} and {@link ItemClock#FROZEN}, of the stack that despawns first. */
    public final int flags;
    public final int remaining;
    public final int lifespan;

    public LootMessage(String dimension, int x, int y, int z, int items, int flags, int remaining, int lifespan) {
        this.present = true;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.items = items;
        this.flags = flags;
        this.remaining = remaining;
        this.lifespan = lifespan;
    }

    private LootMessage() {
        this.present = false;
        this.dimension = "";
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.items = 0;
        this.flags = 0;
        this.remaining = 0;
        this.lifespan = 0;
    }

    public static final LootMessage NONE = new LootMessage();

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** Whether a message on the clocks channel is this kind rather than a {@link ClockMessage}. */
    public static boolean is(byte[] data) {
        return data.length > 0 && (data[0] & 0xFF) == FORMAT;
    }

    public byte[] encode() {
        Bytes.Writer out = new Bytes.Writer().unsignedByte(FORMAT).unsignedByte(present ? 1 : 0);
        if (present) {
            out.string(dimension).zigzag(x).zigzag(y).zigzag(z).varInt(items).unsignedByte(flags);
            if (!has(ItemClock.UNLIMITED)) {
                out.zigzag(remaining).varInt(lifespan);
            }
        }
        return out.toByteArray();
    }

    /** The message in {@code data}, or null when it is not one or broken. */
    public static LootMessage decode(byte[] data) {
        try {
            Bytes.Reader in = new Bytes.Reader(data);
            if (in.unsignedByte() != FORMAT) {
                return null;
            }
            int state = in.unsignedByte();
            if (state == 0) {
                return NONE;
            }
            if (state != 1) {
                return null;
            }
            String dimension = in.string(MAX_DIMENSION_BYTES);
            int x = in.zigzag();
            int y = in.zigzag();
            int z = in.zigzag();
            int items = in.varInt();
            int flags = in.unsignedByte();
            if ((flags & ItemClock.UNLIMITED) != 0) {
                return new LootMessage(dimension, x, y, z, items, flags, 0, 0);
            }
            return new LootMessage(dimension, x, y, z, items, flags, in.zigzag(), in.varInt());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LootMessage)) {
            return false;
        }
        LootMessage m = (LootMessage) o;
        return present == m.present && dimension.equals(m.dimension) && x == m.x && y == m.y && z == m.z
                && items == m.items && flags == m.flags && remaining == m.remaining && lifespan == m.lifespan;
    }

    @Override
    public int hashCode() {
        return ((dimension.hashCode() * 31 + x) * 31 + z) * 31 + remaining;
    }

    @Override
    public String toString() {
        return present ? "LootMessage{" + dimension + " " + x + " " + y + " " + z + ", " + items + " stacks, "
                + (has(ItemClock.UNLIMITED) ? "unlimited" : remaining + "/" + lifespan)
                + (has(ItemClock.FROZEN) ? ", frozen" : "") + "}" : "LootMessage{none}";
    }
}
