package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.ModLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the Bukkit API does not say about a dropped item: how old it is and at what age it despawns. Both are
 * read from the server's own item entity, whose names changed between versions:
 * <ul>
 *   <li>the age is the field {@code age}, or, on servers with obfuscated names (1.17 - 1.20.4), the first
 *   int field of the item entity class;</li>
 *   <li>the despawn age is {@code getDespawnRate()} (Paper 1.16.5 - 1.17.1), the field {@code despawnRate}
 *   (Paper 1.18 and newer) or {@code lifespan} (servers with Forge), and else spigot.yml's
 *   {@code item-despawn-rate} for the item's world, which is what Spigot uses.</li>
 * </ul>
 */
final class ItemAccess {

    private static final class Handle {
        final Method getHandle;
        final Field age;
        final Method despawnRate;
        final Field despawnRateField;

        Handle(Method getHandle, Field age, Method despawnRate, Field despawnRateField) {
            this.getHandle = getHandle;
            this.age = age;
            this.despawnRate = despawnRate;
            this.despawnRateField = despawnRateField;
        }
    }

    private static final Map<Class<?>, Handle> HANDLES = new ConcurrentHashMap<>();
    private static volatile boolean warned;

    private ItemAccess() {
    }

    /**
     * The item's age in ticks; -32768 for an item that never despawns. When it cannot be read, the ticks the
     * item has existed, which is the same unless something reset its age.
     */
    static int age(Item item) {
        try {
            Handle h = handle(item);
            if (h != null && h.age != null) {
                return h.age.getInt(h.getHandle.invoke(item));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
        return item.getTicksLived();
    }

    /** The age at which the item despawns, in ticks. */
    static int lifespan(Item item) {
        try {
            Handle h = handle(item);
            if (h != null && (h.despawnRate != null || h.despawnRateField != null)) {
                Object entity = h.getHandle.invoke(item);
                return h.despawnRate != null ? (int) h.despawnRate.invoke(entity) : h.despawnRateField.getInt(entity);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
        return SpigotConfig.itemDespawnRate(item.getWorld());
    }

    private static Handle handle(Item item) {
        return HANDLES.computeIfAbsent(item.getClass(), ItemAccess::find);
    }

    private static Handle find(Class<?> craftItem) {
        try {
            Method getHandle = craftItem.getMethod("getHandle");
            Class<?> entity = getHandle.getReturnType();
            Field age = null;
            Method rate = null;
            Field rateField = null;
            for (Class<?> c = entity; c != null && c != Object.class; c = c.getSuperclass()) {
                if (age == null) {
                    age = intField(c, "age");
                }
                if (rate == null) {
                    rate = intMethod(c, "getDespawnRate");
                }
                if (rateField == null) {
                    rateField = intField(c, "despawnRate");
                }
                if (rateField == null) {
                    rateField = intField(c, "lifespan");
                }
            }
            if (age == null) {
                age = firstIntField(entity);
            }
            return new Handle(getHandle, age, rate, rate != null ? null : rateField);
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
            return null;
        }
    }

    private static Field intField(Class<?> c, String name) {
        try {
            Field f = c.getDeclaredField(name);
            if (f.getType() == int.class && !Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                return f;
            }
        } catch (NoSuchFieldException | RuntimeException ignored) {
            // Another version's name
        }
        return null;
    }

    private static Method intMethod(Class<?> c, String name) {
        try {
            Method m = c.getDeclaredMethod(name);
            if (m.getReturnType() == int.class && !Modifier.isStatic(m.getModifiers())) {
                m.setAccessible(true);
                return m;
            }
        } catch (NoSuchMethodException | RuntimeException ignored) {
            // Another version's name
        }
        return null;
    }

    /** The item entity class's own first int field: the age, before the pickup delay and the health. */
    private static Field firstIntField(Class<?> entity) {
        for (Class<?> c = entity; c != null && c != Object.class; c = c.getSuperclass()) {
            if (!c.getSimpleName().equals("EntityItem") && !c.getSimpleName().equals("ItemEntity")) {
                continue;
            }
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == int.class && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    private static void warn(Throwable e) {
        if (!warned) {
            warned = true;
            ModLog.warn("Could not read an item's age on this server version (" + Bukkit.getVersion()
                    + "); players with the mod see estimates: " + e);
        }
    }
}
