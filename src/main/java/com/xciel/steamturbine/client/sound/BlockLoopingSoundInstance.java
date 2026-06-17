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
    private float directVolume;

    public BlockLoopingSoundInstance(SoundEvent event, BlockPos pos) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.directVolume = 1.0f;
        this.active = true;

        Vec3 center = Vec3.atCenterOf(pos);
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
    }

    public void keepAlive() {
        keepAlive = 2;
    }

    public void setVolume(float volume) {
        this.directVolume = volume;
    }

    public void stopSound() {
        this.active = false;
    }

    @Override
    public float getVolume() {
        return directVolume;
    }

    @Override
    public void tick() {
        if (!active) {
            stop();
            return;
        }
        keepAlive--;
        if (keepAlive <= 0)
            stopSound();
    }
}
