package com.neroferno.krm_onore.client.particle;

import com.neroferno.krm_onore.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlyingSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private double prevPrevX, prevPrevY, prevPrevZ;

    protected FlyingSparkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.98F;
        this.gravity = 0.8F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.hasPhysics = true;
        
        this.xd = (xSpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.05 * (this.random.nextFloat() > 0.95 ? 2 : 1);
        this.yd = (ySpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.05 * (this.random.nextFloat() > 0.95 ? 2 : 1);
        this.zd = (zSpeed / 2) + (Math.random() * 3.0 - 1.5) * 0.05 * (this.random.nextFloat() > 0.95 ? 2 : 1);

        this.lifetime = Mth.randomBetweenInclusive(level.getRandom(), 20, 60);

        float particleSize = (this.random.nextBoolean() ? 0.025F : 0.03F);
        this.setSize(particleSize, particleSize);
        this.scale(particleSize * 10f); // Adjust scale for render size

        this.sprites = spriteSet;
        this.setSpriteFromAge(this.sprites);
        
        this.prevPrevX = this.xo;
        this.prevPrevY = this.yo;
        this.prevPrevZ = this.zo;
    }

    @Override
    public void tick() {
        this.prevPrevX = this.xo;
        this.prevPrevY = this.yo;
        this.prevPrevZ = this.zo;
        
        super.tick();

        if (this.age < 0 || this.removed) {
            return;
        }

        this.setSpriteFromAge(this.sprites);

        float percentageTimeUntilDeath = (float) this.age / this.lifetime;

        // Spawn random spark flashes
        if ((this.random.nextFloat() > percentageTimeUntilDeath + 0.8f ||
            (this.random.nextFloat() < 0.01f && (Math.abs(this.xd) + Math.abs(this.yd) + Math.abs(this.zd)) > 0.001))) {
            this.level.addParticle(ModParticles.SPARK_FLASH.get(), this.prevPrevX, this.prevPrevY, this.prevPrevZ, 0, 0, 0);
        }
        
        // Simple bounce simulation
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
            this.yd = Math.abs(this.yd) * 0.6; // Bounce up slightly
            if (this.yd < 0.05) {
                this.yd = 0; // Stop bouncing if moving too slow
            } else {
                this.onGround = false; // We bounced!
            }
        }
    }

    @Override
    public int getLightColor(float partialTicks) {
        float percentageTimeAlive = Math.abs(1 - ((float) this.age / this.lifetime));
        int sparkLight = (int) (percentageTimeAlive * 15f);

        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        int blockLight = this.level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = this.level.getBrightness(LightLayer.SKY, pos);

        return net.minecraft.client.renderer.LightTexture.pack(Math.max(blockLight, sparkLight), skyLight);
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
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new FlyingSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
