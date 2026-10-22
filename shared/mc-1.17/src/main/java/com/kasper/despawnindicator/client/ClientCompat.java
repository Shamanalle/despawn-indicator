package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The client side of {@link com.kasper.despawnindicator.Compat}: Minecraft 1.17.1 - 1.18.2. */
public final class ClientCompat {

    private ClientCompat() {
    }

    /** A note block note in the interface: the bell, or else the pling. */
    public static void playNote(boolean bell) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(bell ? SoundEvents.NOTE_BLOCK_BELL : SoundEvents.NOTE_BLOCK_PLING, 1.0F));
    }
}
