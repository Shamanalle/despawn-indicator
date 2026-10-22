package com.kasper.despawnindicator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out, or
 * drawn smaller while it fades ({@link ItemLook}). Fabric, Minecraft 1.16.5.
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    public BlinkingItemRenderer(EntityRenderDispatcher dispatcher, ItemRenderer items) {
        super(dispatcher, items);
    }

    /** Puts it in place of the vanilla one. */
    public static void register() {
        EntityRendererRegistry.INSTANCE.register(EntityType.ITEM,
                (dispatcher, context) -> new BlinkingItemRenderer(dispatcher, context.getItemRenderer()));
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
