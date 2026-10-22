package com.kasper.despawnindicator;

/** The arithmetic of the death loot timer: how urgent it is and which way the items are. */
public final class LootHud {

    /** The timer turns yellow in the last minute and red, blinking, in the last 15 seconds. */
    public static final int SOON_SECONDS = 60;
    public static final int URGENT_SECONDS = 15;

    /** Up, up-right, right... as seen from the player looking ahead, clockwise. */
    public static final String ARROWS = "↑↗→↘↓↙←↖";

    public enum Level {
        CALM, SOON, URGENT
    }

    private LootHud() {
    }

    /** Seconds left, rounded up like the text, so "1:00" is the first yellow one and "0:15" the first red one. */
    public static int seconds(int ticksLeft) {
        return Math.max(0, (ticksLeft + DespawnIndicator.TICKS_PER_SECOND - 1) / DespawnIndicator.TICKS_PER_SECOND);
    }

    public static Level level(int ticksLeft) {
        int s = seconds(ticksLeft);
        if (s <= URGENT_SECONDS) {
            return Level.URGENT;
        }
        return s <= SOON_SECONDS ? Level.SOON : Level.CALM;
    }

    /**
     * Which of the 8 {@link #ARROWS} points to a spot {@code dx}, {@code dz} blocks away for a player facing
     * {@code yaw} (Minecraft's: 0 is south, +z, and it grows turning right).
     */
    public static int direction(double dx, double dz, double yaw) {
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double turn = target - yaw;
        turn = turn - 360 * Math.floor((turn + 180) / 360);
        int i = (int) Math.round(turn / 45);
        return ((i % 8) + 8) % 8;
    }

    public static char arrow(double dx, double dz, double yaw) {
        return ARROWS.charAt(direction(dx, dz, yaw));
    }

    /** Whole blocks to the spot. */
    public static int distance(double dx, double dy, double dz) {
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    /** How opaque the timer is drawn now in the style, 0 - 1; it warns in the last {@link #URGENT_SECONDS}. */
    public static double alpha(ClientSettings.Style style, int ticksLeft, boolean frozen) {
        if (frozen || style == ClientSettings.Style.LABEL) {
            return 1;
        }
        double s = ticksLeft / (double) DespawnIndicator.TICKS_PER_SECOND;
        if (style == ClientSettings.Style.FADE) {
            return Blink.alpha(s, URGENT_SECONDS);
        }
        return Blink.shown(s, URGENT_SECONDS) ? 1 : 0;
    }
}
