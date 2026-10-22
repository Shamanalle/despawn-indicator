package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.Dimensions;
import com.kasper.despawnindicator.LootHud;
import com.kasper.despawnindicator.LootTracker;
import com.kasper.despawnindicator.TimeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;

/**
 * The death loot timer in the top left corner: how long until the first item despawns, which way and how far
 * the loot is, its coordinates and dimension. Yellow in the last minute, red and blinking (or fading) in the
 * last seconds. Each loader calls {@link #draw} from its HUD event.
 */
public final class LootOverlay {

    private static final int MARGIN = 4;
    private static final int PAD = 3;
    private static final int LINE = 10;
    private static final int BACKGROUND = 0x80000000;
    private static final int WHITE = 0xFFFFFF;
    private static final int YELLOW = 0xFFFF55;
    private static final int RED = 0xFF5555;

    private LootOverlay() {
    }

    public static void draw(Canvas canvas) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientSettings settings = ClientConfig.get();
        LootTracker.Reading r = ClientTicker.LOOT.read();
        if (!settings.deathHud || r == null || player == null || mc.level == null) {
            return;
        }
        String first;
        int colour = WHITE;
        double alpha = 1;
        if (r.unlimited) {
            first = I18n.get("gui.despawnindicator.loot.unlimited");
        } else {
            first = I18n.get("gui.despawnindicator.loot.title", TimeText.format(r.ticksLeft, r.estimate));
            if (r.frozen) {
                first += " (" + I18n.get("gui.despawnindicator.loot.paused") + ")";
            }
            LootHud.Level level = LootHud.level(r.ticksLeft);
            colour = level == LootHud.Level.URGENT ? RED : level == LootHud.Level.SOON ? YELLOW : WHITE;
            alpha = LootHud.alpha(settings.style, r.ticksLeft, r.frozen);
        }
        StringBuilder second = new StringBuilder();
        if (r.dimension.equals(LootClient.dimension(mc.level))) {
            double dx = r.x + 0.5 - player.getX();
            double dy = r.y - player.getY();
            double dz = r.z + 0.5 - player.getZ();
            second.append(LootHud.arrow(dx, dz, player.getYRot())).append(' ')
                    .append(I18n.get("gui.despawnindicator.loot.distance", LootHud.distance(dx, dy, dz)))
                    .append("  ");
        }
        second.append(r.x).append(", ").append(r.y).append(", ").append(r.z).append("  ").append(dimensionName(r.dimension));
        String line2 = second.toString();

        int width = Math.max(mc.font.width(first), mc.font.width(line2)) + 2 * PAD;
        int height = 2 * LINE + 2 * PAD - 2;
        int x = MARGIN;
        int y = MARGIN;
        canvas.fill(x, y, x + width, y + height, BACKGROUND);
        int cx = x + width / 2;
        int a = (int) Math.round(alpha * 255);
        // Older versions draw text with almost no alpha as opaque
        if (a >= 26) {
            canvas.centeredText(first, cx, y + PAD, a << 24 | colour);
        }
        canvas.centeredText(line2, cx, y + PAD + LINE, 0xFF000000 | WHITE);
    }

    private static String dimensionName(String id) {
        String key = Dimensions.nameKey(id);
        return key != null ? I18n.get(key) : Dimensions.path(id);
    }
}
