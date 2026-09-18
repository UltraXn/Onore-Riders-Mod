package com.neroferno.krm_revo.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LinearSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected double prevPrevX;
    protected double prevPrevY;
    protected double prevPrevZ;
    protected float prevPitch;
    protected float prevYaw;

    private static final Vector3f[] CROSS_VERTS = new Vector3f[]{
            new Vector3f(-0.5f,  0.5f, -0.5f), new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f( 0.5f, -0.5f,  0.5f), new Vector3f( 0.5f,  0.5f,  0.5f),
            new Vector3f( 0.5f,  0.5f,  0.5f), new Vector3f( 0.5f, -0.5f,  0.5f), new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(-0.5f,  0.5f, -0.5f),
            new Vector3f( 0.5f,  0.5f, -0.5f), new Vector3f( 0.5f, -0.5f, -0.5f), new Vector3f(-0.5f, -0.5f,  0.5f), new Vector3f(-0.5f,  0.5f,  0.5f),
            new Vector3f(-0.5f,  0.5f,  0.5f), new Vector3f(-0.5f, -0.5f,  0.5f), new Vector3f( 0.5f, -0.5f, -0.5f), new Vector3f( 0.5f,  0.5f, -0.5f)
    };

    protected LinearSparkParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.prevPrevX = this.xo;
        this.prevPrevY = this.yo;
        this.prevPrevZ = this.zo;

        this.quadSize = 0.02F + (this.random.nextFloat() * 0.02F); // Based on FlyingSpark's ~0.03F
        this.lifetime = 10 + this.random.nextInt(15);
        this.gravity = 0.5F; 
        this.friction = 0.9F;
        this.hasPhysics = true;

        // Start almost white (high R, G, B) and fade down
        float colorRand = this.random.nextFloat() * 0.1f;
        this.rCol = 1.0f;
        this.gCol = 0.95f + colorRand; // Very high green
        this.bCol = 0.8f + colorRand;  // High blue for white-ish core

        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @Override
    public int getLightColor(float partialTick) {
        // Bright particle
        return 240 | (240 << 16);
    }

    @Override
    public void tick() {
        this.prevPrevX = this.xo;
        this.prevPrevY = this.yo;
        this.prevPrevZ = this.zo;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float percentageTimeUntilDeath = (float) this.age / this.lifetime;
        
        // Spawn random companion flash particles, exactly as in particle-interactions 
        if (
            this.random.nextFloat() > percentageTimeUntilDeath + 0.8f ||
            (this.random.nextFloat() * this.random.nextFloat() + this.xd*this.xd + this.yd*this.yd + this.zd*this.zd > 0.001)
        ) {
            this.level.addParticle(com.neroferno.krm_revo.particle.ModParticles.SPARK_FLASH.get(), this.prevPrevX, this.prevPrevY, this.prevPrevZ, 0, 0, 0);
        }

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
            
            this.yd -= 0.04D * (double)this.gravity;
            this.move(this.xd, this.yd, this.zd);
            
            // Cooldown color: fade blue quickly to lose the white, then fade green to turn yellow into orange/red
            this.bCol = Math.max(0.0f, this.bCol - 0.2f);
            this.gCol = Math.max(0.2f, this.gCol - 0.08f);

            // Reapply speed checks / friction
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;

            if (this.onGround) {
                this.xd *= 0.7D;
                this.zd *= 0.7D;
                // Bounce
                this.yd *= -0.6D;
            }
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();

        float x = (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cameraPos.z());

        float px = (float) (Mth.lerp((double) partialTicks, this.prevPrevX, this.xo) - cameraPos.x());
        float py = (float) (Mth.lerp((double) partialTicks, this.prevPrevY, this.yo) - cameraPos.y());
        float pz = (float) (Mth.lerp((double) partialTicks, this.prevPrevZ, this.zo) - cameraPos.z());

        Vector3f pos = new Vector3f(x, y, z);
        Vector3f prevPos = new Vector3f(px, py, pz);

        Vector3f moveDir = new Vector3f(pos).sub(prevPos);
        float distance = moveDir.length();

        if (distance > 0.001f) {
            moveDir.normalize();
        } else {
            moveDir.set(0, 1, 0); 
        }

        float pitch = (float) Math.asin(moveDir.y);
        if (!Float.isFinite(pitch)) pitch = this.prevPitch;
        this.prevPitch = pitch;

        float yaw = (float) Math.atan2(moveDir.x, moveDir.z);
        if (!Float.isFinite(yaw)) yaw = this.prevYaw;
        this.prevYaw = yaw;

        // Origin of shape is exactly between prevPos and pos
        Vector3f shapePos = new Vector3f(pos).add(prevPos).mul(0.5f);

        // Calculate stretch based on velocity to give it exactly that "FlyingSpark" scale multiplier
        float stretchScale = Math.max(distance * 40.0f, 1.0f); 

        // Thin width (using average random scale 0.75) and long height proportional to distance
        float thickness = this.quadSize * 0.75f;
        Vector3f scaleVec = new Vector3f(thickness, this.quadSize * stretchScale, thickness);

        // FlyingSpark rotation: align Y-axis to vector
        Quaternionf rot = new Quaternionf();
        rot.rotateY(yaw);
        rot.rotateX(-(pitch - (float)(Math.PI/2))); // Pitch minus 90 degrees

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        for (int i = 0; i < CROSS_VERTS.length; i += 4) {
            for (int k = 0; k < 4; k++) {
                Vector3f v = new Vector3f(CROSS_VERTS[i + k]);
                v.mul(scaleVec);
                v.rotate(rot);
                v.add(shapePos);

                float u = (k == 0 || k == 1) ? u0 : u1;
                float vCoord = (k == 0 || k == 3) ? v1 : v0;

                buffer.addVertex(v.x(), v.y(), v.z())
                      .setUv(u, vCoord)
                      .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                      .setLight(light);
            }
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                                                     double x, double y, double z,
                                                                     double xSpeed, double ySpeed, double zSpeed) {
            return new LinearSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
