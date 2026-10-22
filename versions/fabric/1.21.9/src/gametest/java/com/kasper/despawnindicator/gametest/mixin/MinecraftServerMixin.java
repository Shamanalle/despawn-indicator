package com.kasper.despawnindicator.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since 1.21.9 the spawn chunks load while the player joins, and the server finishes loaded chunks only in the
 * spare time between its ticks. The client gametest ticks the server in step with the client, so on a slow
 * machine (a CI runner drawing with the CPU) the server is always behind, never has spare time, and the world
 * never loads. In the test the server finishes them even when behind, as it does when sprinting.
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {

    @ModifyExpressionValue(method = "pollTaskInternal",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;haveTime()Z"))
    private boolean despawnindicator$pollChunksWhenBehind(boolean haveTime) {
        return true;
    }
}
