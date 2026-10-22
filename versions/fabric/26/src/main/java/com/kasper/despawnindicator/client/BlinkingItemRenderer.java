package com.kasper.despawnindicator.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * The vanilla item renderer, except that an item about to despawn is skipped on the frames it blinks out
 * ({@link ItemLook}). Fabric, Minecraft 26.x.
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

    /** Puts it in place of the vanilla one. */
    @SuppressWarnings("unchecked")
    public static void register() {
        EntityType<ItemEntity> item = (EntityType<ItemEntity>) BuiltInRegistries.ENTITY_TYPE
                .getValue(Identifier.withDefaultNamespace("item"));
        EntityRendererRegistry.register(item, BlinkingItemRenderer::new);
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
