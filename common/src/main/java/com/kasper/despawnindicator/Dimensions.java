package com.kasper.despawnindicator;

/** Dimension ids as the mod passes them around ("minecraft:the_nether"), and the names players see. */
public final class Dimensions {

    private Dimensions() {
    }

    /**
     * The id in a dimension key's text, "ResourceKey[minecraft:dimension / minecraft:the_nether]". The
     * method that gives the id directly was renamed in Minecraft 26.1, the text stayed the same; anything
     * else comes back whole.
     */
    public static String id(String resourceKey) {
        int slash = resourceKey.lastIndexOf(" / ");
        if (resourceKey.startsWith("ResourceKey[") && resourceKey.endsWith("]") && slash >= 0) {
            return resourceKey.substring(slash + 3, resourceKey.length() - 1);
        }
        return resourceKey;
    }

    /** The translation key of a vanilla dimension's name, or null for others ({@link #path} then). */
    public static String nameKey(String id) {
        if (id.equals("minecraft:overworld")) {
            return "gui.despawnindicator.dimension.overworld";
        }
        if (id.equals("minecraft:the_nether")) {
            return "gui.despawnindicator.dimension.the_nether";
        }
        if (id.equals("minecraft:the_end")) {
            return "gui.despawnindicator.dimension.the_end";
        }
        return null;
    }

    /** "twilightforest:twilight_forest" without its namespace, as a name for a dimension of a mod. */
    public static String path(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
