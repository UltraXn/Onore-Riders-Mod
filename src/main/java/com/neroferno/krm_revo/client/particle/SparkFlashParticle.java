package com.neroferno.krm_revo.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class SparkFlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float originalQuadSize;

    protected SparkFlashParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.96F;
        this.speedUpWhenYMotionIsBlocked = true;

        this.xd = (xSpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.07 * (this.random.nextFloat() > 0.95 ? 2 : 1);
        this.yd = (ySpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.07 * (this.random.nextFloat() > 0.95 ? 2 : 1);
        this.zd = (zSpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.07 * (this.random.nextFloat() > 0.95 ? 2 : 1);
        
        int rot = this.random.nextInt(4);
        this.roll = rot * (float)(Math.PI / 2);
        this.oRoll = this.roll;

        this.lifetime = this.random.nextInt(4) + 3;

        this.sprites = sprites;
        this.setSpriteFromAge(sprites);

        this.scale(2f / 16f);
        this.originalQuadSize = this.quadSize;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSprite(this.sprites.get(this.random));
        this.quadSize = this.originalQuadSize * (0.5f + (Math.abs(1 - ((float)this.age / this.lifetime)) * 0.5f));
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SparkFlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
