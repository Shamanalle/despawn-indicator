package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The client side of {@link com.kasper.despawnindicator.Compat}: Minecraft 1.20 and later. */
public final class ClientCompat {

    private ClientCompat() {
    }

    /** A note block note in the interface: the bell, or else the pling. */
    public static void playNote(boolean bell) {
        // playDelayed: play() returns a result since 1.21.6, so its signature differs within 1.21.x
        Minecraft.getInstance().getSoundManager().playDelayed(SimpleSoundInstance.forUI(
                bell ? SoundEvents.NOTE_BLOCK_BELL : SoundEvents.NOTE_BLOCK_PLING, 1.0F), 0);
    }
}
