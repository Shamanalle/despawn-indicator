package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.Compat;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.TimeText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The time label over an item: on the item the player looks at, and while sneaking on the items around them
 * ({@link ClientSettings#labels}); in the label style also on the items in their last seconds, which then do
 * not blink.
 * <p>
 * The label is the item's name tag, set on this client only (the server never hears of it). Name tags are
 * drawn the same way on every Minecraft version, while the renderer methods behind them changed several
 * times. Items that already have a name keep it and get no label.
 */
final class ItemLabels {

    /** How far a looked-at item gets a label, in blocks. */
    private static final double LOOK_REACH = 24;
    /** How much the ray may miss a small item by and still count it as looked at. */
    private static final double LOOK_SLACK = 0.2;
    /** Sneaking labels the items this close, at most {@link #SNEAK_MAX} of them, the nearest first. */
    private static final double SNEAK_RADIUS = 8;
    private static final int SNEAK_MAX = 32;

    private static final class Label {
        final Component name;
        final String text;
        final boolean wasVisible;

        Label(Component name, String text, boolean wasVisible) {
            this.name = name;
            this.text = text;
            this.wasVisible = wasVisible;
        }
    }

    private static final Map<Integer, Label> LABELS = new HashMap<>();

    private ItemLabels() {
    }

    static boolean labeled(ItemEntity item) {
        return LABELS.containsKey(item.getId());
    }

    static void update(Minecraft mc, ClientLevel level, ClientSettings settings) {
        LocalPlayer player = mc.player;
        Map<Integer, ItemEntity> targets = player == null ? new HashMap<>() : targets(player, level, settings);
        for (Iterator<Map.Entry<Integer, Label>> i = LABELS.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Integer, Label> e = i.next();
            if (!targets.containsKey(e.getKey())) {
                Entity entity = level.getEntity(e.getKey());
                if (entity instanceof ItemEntity) {
                    restore((ItemEntity) entity, e.getValue());
                }
                i.remove();
            }
        }
        for (ItemEntity item : targets.values()) {
            show(item, settings.warnSeconds);
        }
    }

    private static void show(ItemEntity item, int warnSeconds) {
        ClientClocks.Reading r = ItemLook.read(item);
        Label old = LABELS.get(item.getId());
        if (old != null && item.getCustomName() != old.name) {
            // Someone else named the item meanwhile: theirs stays
            LABELS.remove(item.getId());
            return;
        }
        if (r.unlimited) {
            if (old != null) {
                restore(item, old);
                LABELS.remove(item.getId());
            }
            return;
        }
        String text = TimeText.format(r.ticksLeft, r.estimate);
        if (old != null && old.text.equals(text)) {
            return;
        }
        if (old == null && item.getCustomName() != null) {
            return;
        }
        Component name = Compat.literal(text).withStyle(colour(r.ticksLeft, warnSeconds));
        boolean wasVisible = old != null ? old.wasVisible : item.isCustomNameVisible();
        item.setCustomName(name);
        item.setCustomNameVisible(true);
        LABELS.put(item.getId(), new Label(name, text, wasVisible));
    }

    private static ChatFormatting colour(int ticksLeft, int warnSeconds) {
        // Rounded up like the text, so "0:20" is the first red one
        int seconds = (ticksLeft + DespawnIndicator.TICKS_PER_SECOND - 1) / DespawnIndicator.TICKS_PER_SECOND;
        if (seconds <= warnSeconds) {
            return ChatFormatting.RED;
        }
        return seconds <= 60 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
    }

    private static void restore(ItemEntity item, Label label) {
        if (item.getCustomName() == label.name) {
            item.setCustomName(null);
            item.setCustomNameVisible(label.wasVisible);
        }
    }

    /** The items to label now, by entity id. */
    private static Map<Integer, ItemEntity> targets(LocalPlayer player, ClientLevel level, ClientSettings settings) {
        Map<Integer, ItemEntity> out = new HashMap<>();
        Vec3 eye = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(LOOK_REACH));
        double blockHit = LOOK_REACH * LOOK_REACH;
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            blockHit = block.getLocation().distanceToSqr(eye);
        }
        ItemEntity looked = null;
        double lookedAt = blockHit;
        List<ItemEntity> near = new ArrayList<>();
        double reach = (LOOK_REACH + 1) * (LOOK_REACH + 1);
        boolean sneaking = settings.labels.sneak() && player.isShiftKeyDown();
        boolean warn = settings.style == ClientSettings.Style.LABEL;
        double warnTicks = settings.warnSeconds * (double) DespawnIndicator.TICKS_PER_SECOND;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity) || e.isRemoved()) {
                continue;
            }
            ItemEntity item = (ItemEntity) e;
            double d = item.distanceToSqr(player);
            if (d > reach) {
                continue;
            }
            if (sneaking && d <= SNEAK_RADIUS * SNEAK_RADIUS) {
                near.add(item);
            } else if (warn) {
                ClientClocks.Reading r = ItemLook.read(item);
                if (!r.unlimited && r.ticksLeft <= warnTicks) {
                    near.add(item);
                }
            }
            if (!settings.labels.look()) {
                continue;
            }
            Optional<Vec3> hit = item.getBoundingBox().inflate(LOOK_SLACK).clip(eye, end);
            if (hit.isPresent()) {
                double h = hit.get().distanceToSqr(eye);
                if (h < lookedAt) {
                    lookedAt = h;
                    looked = item;
                }
            }
        }
        if (looked != null) {
            out.put(looked.getId(), looked);
        }
        if (near.size() > SNEAK_MAX) {
            // The nearest first, the warned ones too
            near.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
            near = near.subList(0, SNEAK_MAX);
        }
        for (ItemEntity item : near) {
            out.put(item.getId(), item);
        }
        return out;
    }

    /** Left the world: its entities are gone with their names. */
    static void clear() {
        LABELS.clear();
    }
}
