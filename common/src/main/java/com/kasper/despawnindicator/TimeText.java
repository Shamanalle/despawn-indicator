package com.kasper.despawnindicator;

/** How the time left is written: "4:59", "0:07", "1:02:03"; an estimate starts with "≈". */
public final class TimeText {

    public static final String ESTIMATE = "≈";

    private TimeText() {
    }

    /** Ticks left as minutes and seconds, rounded up, so "0:00" only shows when the item is gone. */
    public static String format(int ticks, boolean estimate) {
        int seconds = Math.max(0, (ticks + DespawnIndicator.TICKS_PER_SECOND - 1) / DespawnIndicator.TICKS_PER_SECOND);
        int h = seconds / 3600;
        int m = seconds / 60 % 60;
        int s = seconds % 60;
        String text = h > 0 ? h + ":" + two(m) + ":" + two(s) : m + ":" + two(s);
        return estimate ? ESTIMATE + text : text;
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : Integer.toString(n);
    }
}
