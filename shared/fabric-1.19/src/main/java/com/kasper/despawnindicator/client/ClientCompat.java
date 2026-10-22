package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** The client side of {@link com.kasper.despawnindicator.Compat}: Minecraft 1.19 - 1.19.4. */
public final class ClientCompat {

    private ClientCompat() {
    }

    /** A note block note in the interface: the bell, or else the pling. */
    public static void playNote(boolean bell) {
        // By name, as forUI did: the sound fields of SoundEvents changed their type in 1.19.3
        ResourceLocation id = new ResourceLocation(bell ? "block.note_block.bell" : "block.note_block.pling");
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(id, SoundSource.MASTER, 0.25F, 1.0F,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
}
