package com.kasper.despawnindicator.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out
 * ({@link ItemLook}). Fabric, Minecraft 26.x, where the method also takes the partial tick.
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    public BlinkingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Puts it in place of the vanilla one. */
    public static void register() {
        EntityRendererRegistry.register(EntityTypes.ITEM, BlinkingItemRenderer::new);
    }

    @Override
    public boolean shouldRender(ItemEntity item, Frustum frustum, double x, double y, double z, float partialTick) {
        return !ItemLook.hidden(item) && super.shouldRender(item, frustum, x, y, z, partialTick);
    }
}
