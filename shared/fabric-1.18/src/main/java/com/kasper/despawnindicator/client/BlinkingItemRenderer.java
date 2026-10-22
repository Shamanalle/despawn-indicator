package com.kasper.despawnindicator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out, or
 * drawn smaller while it fades ({@link ItemLook}). Fabric, Minecraft 1.18.2 - 1.19.4.
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    public BlinkingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Puts it in place of the vanilla one. */
    public static void register() {
        EntityRendererRegistry.register(EntityType.ITEM, BlinkingItemRenderer::new);
    }

    @Override
    public boolean shouldRender(ItemEntity item, Frustum frustum, double x, double y, double z) {
        return !ItemLook.hidden(item) && super.shouldRender(item, frustum, x, y, z);
    }

    @Override
    public void render(ItemEntity item, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        ItemLook.drawScaled(pose, ItemLook.scale(item, partialTick),
                () -> super.render(item, yaw, partialTick, pose, buffers, light));
    }
}
