package com.kasper.despawnindicator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out, or
 * drawn smaller while it fades ({@link ItemLook}). NeoForge, Minecraft 1.21.11: the item is handed to
 * the frame from a render state taken from it first, so the size goes into that state.
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    public BlinkingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ItemEntity item, Frustum frustum, double x, double y, double z) {
        return !ItemLook.hidden(item) && super.shouldRender(item, frustum, x, y, z);
    }

    @Override
    public ItemEntityRenderState createRenderState() {
        return new FadeState();
    }

    @Override
    public void extractRenderState(ItemEntity item, ItemEntityRenderState state, float partialTick) {
        super.extractRenderState(item, state, partialTick);
        ((FadeState) state).scale = ItemLook.scale(item, partialTick);
    }

    @Override
    public void submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        ItemLook.drawScaled(pose, ((FadeState) state).scale, () -> super.submit(state, pose, nodes, camera));
    }

    /** The vanilla state and the size to draw the item at. */
    public static class FadeState extends ItemEntityRenderState {
        public float scale = 1;
    }
}
