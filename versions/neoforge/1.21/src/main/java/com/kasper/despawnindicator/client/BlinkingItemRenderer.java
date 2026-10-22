package com.kasper.despawnindicator.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out
 * ({@link ItemLook}). NeoForge, Minecraft 1.21 - 1.21.1.
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    public BlinkingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ItemEntity item, Frustum frustum, double x, double y, double z) {
        return !ItemLook.hidden(item) && super.shouldRender(item, frustum, x, y, z);
    }
}
