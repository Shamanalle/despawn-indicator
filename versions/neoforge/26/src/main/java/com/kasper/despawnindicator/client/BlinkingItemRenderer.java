package com.kasper.despawnindicator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out, or
 * drawn smaller while it fades ({@link ItemLook}). NeoForge, Minecraft 26.x. The item is handed to the frame
 * from a render state taken from it first, so the size goes into that state.
 * <p>
 * One jar runs on all of 26.x, compiled against 26.3. Two things it needs changed within that range:
 * the item entity type moved from {@code EntityType.ITEM} to {@code EntityTypes.ITEM} in 26.2, so it is
 * looked up by id; and 26.3 added the partial tick to {@code shouldRender}, so both versions of that method
 * are overridden here, and the older one reaches the vanilla method through a method handle.
 * The compatibility check lists the differences this leaves (.github/scripts/linkcheck-allowed-26.txt).
 */
public class BlinkingItemRenderer extends ItemEntityRenderer {

    /** The vanilla {@code shouldRender} of 26.1 - 26.2, absent on 26.3. */
    private static final MethodHandle SHOULD_RENDER_OLD = findOld();

    public BlinkingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** The item entity type, looked up by id. */
    @SuppressWarnings("unchecked")
    public static EntityType<ItemEntity> itemType() {
        return (EntityType<ItemEntity>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item"));
    }

    /** Minecraft 26.3. */
    @Override
    public boolean shouldRender(ItemEntity item, Frustum frustum, double x, double y, double z, float partialTick) {
        return !ItemLook.hidden(item) && super.shouldRender(item, frustum, x, y, z, partialTick);
    }

    /** Minecraft 26.1 - 26.2: overrides the vanilla method there, never called on 26.3. */
    public boolean shouldRender(Entity entity, Frustum frustum, double x, double y, double z) {
        if (entity instanceof ItemEntity item && ItemLook.hidden(item)) {
            return false;
        }
        try {
            return (boolean) SHOULD_RENDER_OLD.invokeExact(this, entity, frustum, x, y, z);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
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

    private static MethodHandle findOld() {
        try {
            return MethodHandles.lookup().findSpecial(ItemEntityRenderer.class, "shouldRender",
                    MethodType.methodType(boolean.class, Entity.class, Frustum.class,
                            double.class, double.class, double.class),
                    BlinkingItemRenderer.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }
}
