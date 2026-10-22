package com.kasper.despawnindicator;

/**
 * When an item about to despawn is drawn. In the last {@code warn} seconds it blinks, slowly at first and
 * faster towards the end: once a second at the start of the warning, four times a second at its end.
 * <p>
 * The phase is a function of the time left alone (the integral of the blink rate), so the blinking speeds up
 * smoothly without jumps, and items with the same time left blink together.
 */
public final class Blink {

    /** Blinks per second when the warning starts and when the item is about to go. */
    public static final double SLOW = 1.0;
    public static final double FAST = 4.0;
    /** The part of each blink the item is shown. */
    private static final double SHOWN = 0.6;
    /** How faint the fading death loot timer gets at its faintest. */
    private static final double MIN_ALPHA = 0.25;
    /** How small a fading item gets at its smallest. */
    private static final double MIN_SCALE = 0.3;

    private Blink() {
    }

    /**
     * Blinks since the warning began; 0 before it. Past the end (an estimate that ran out while the item is
     * still there) it goes on at the fast rate, so the item keeps blinking rather than staying hidden.
     */
    public static double phase(double secondsLeft, double warnSeconds) {
        if (warnSeconds <= 0 || secondsLeft >= warnSeconds) {
            return 0;
        }
        double u = warnSeconds - Math.max(0, secondsLeft);
        double overtime = Math.max(0, -secondsLeft);
        return SLOW * u + (FAST - SLOW) * u * u / (2 * warnSeconds) + FAST * overtime;
    }

    /** Whether the item is drawn now (the blinking style). */
    public static boolean shown(double secondsLeft, double warnSeconds) {
        if (secondsLeft >= warnSeconds) {
            return true;
        }
        double p = phase(secondsLeft, warnSeconds);
        return p - Math.floor(p) < SHOWN;
    }

    /** How opaque the death loot timer is drawn now, 0.25 - 1 (the fading style). */
    public static double alpha(double secondsLeft, double warnSeconds) {
        return MIN_ALPHA + (1 - MIN_ALPHA) * wave(secondsLeft, warnSeconds);
    }

    /**
     * How large an item is drawn now, 0.3 - 1 (the fading style): it shrinks and grows back at the blink rate.
     * Not transparency, which shader packs often draw as fully opaque.
     */
    public static double scale(double secondsLeft, double warnSeconds) {
        return MIN_SCALE + (1 - MIN_SCALE) * wave(secondsLeft, warnSeconds);
    }

    /** 1 at the start of each blink, 0 halfway through it; 1 before the warning. */
    private static double wave(double secondsLeft, double warnSeconds) {
        if (secondsLeft >= warnSeconds) {
            return 1;
        }
        return 0.5 + 0.5 * Math.cos(2 * Math.PI * phase(secondsLeft, warnSeconds));
    }
}
