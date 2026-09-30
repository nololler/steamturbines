package com.xciel.turbines.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class BlueHydroPoofParticle extends SimpleAnimatedParticle {

    private final SpriteSet sprites;
    private final float initialSize;

    private BlueHydroPoofParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, sprites, level.random.nextFloat() * 0.5f);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.88f;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 7 + level.random.nextInt(3);
        this.quadSize = 0.28f + level.random.nextFloat() * 0.12f;
        this.initialSize = quadSize;
        setColor(0.08f, 0.45f, 0.98f);
        setAlpha(0.95f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = Math.min(1, (float) age / lifetime);
        quadSize = initialSize * (1 + progress * 1.8f);
        setAlpha(0.95f * (1 - progress));
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new BlueHydroPoofParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
