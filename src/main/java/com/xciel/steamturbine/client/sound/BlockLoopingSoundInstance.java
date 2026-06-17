package com.xciel.steamturbine.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class BlockLoopingSoundInstance extends AbstractTickableSoundInstance {

    private boolean active;
    private int keepAlive;
    private float maxVolume;

    public BlockLoopingSoundInstance(SoundEvent event, BlockPos pos, float maxVolume) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.05f;
        this.active = true;
        this.maxVolume = maxVolume;

        Vec3 center = Vec3.atCenterOf(pos);
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
    }

    public void keepAlive() {
        keepAlive = 2;
    }

    public void stopSound() {
        this.active = false;
    }

    @Override
    public void tick() {
        if (active) {
            volume = Math.min(maxVolume, volume + 0.25f);
            keepAlive--;
            if (keepAlive <= 0)
                stopSound();
            return;
        }
        volume = Math.max(0, volume - 0.25f);
        if (volume == 0)
            stop();
    }
}
