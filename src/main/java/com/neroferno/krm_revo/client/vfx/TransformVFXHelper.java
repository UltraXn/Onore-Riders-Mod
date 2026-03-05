package com.neroferno.krm_revo.client.vfx;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central dispatcher for all KRM: REvolution visual effects.
 *
 * All VFX methods are client-side only. Server-side callers must dispatch
 * via the ClientboundCustomPayload packet system.
 */
@SuppressWarnings("null")
public class TransformVFXHelper {

    /**
     * Plays the full transformation visual sequence:
     *  - Intense screen shake
     *  - Electric spark ring around the player
     *  - Rising energy pillar
     */
    public static void playTransformBurst(Vec3 pos) {
        if (!isClient()) return;

        ScreenShakeHandler.shake(3.0f);
        spawnElectricRing(pos.add(0, 1.0, 0), 1.2f, 30);
        spawnEnergyPillar(pos, 3.0f);
    }

    /**
     * Plays the de-transformation visual sequence.
     */
    public static void playDetransformBurst(Vec3 pos) {
        if (!isClient()) return;

        ScreenShakeHandler.shake(1.2f);
        spawnElectricRing(pos.add(0, 1.0, 0), 0.8f, 16);
    }

    /**
     * Small electric spark burst used on unarmed hit impacts.
     */
    public static void playImpactSparks(Vec3 pos) {
        if (!isClient()) return;

        ScreenShakeHandler.shake(0.5f);
        playImpactSparks(pos, new Vec3(0, 1, 0));
    }

    private static void spawnElectricRing(Vec3 center, float radius, int count) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 / count) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double vx = Math.cos(angle) * 0.3;
            double vy = 0.2 + ThreadLocalRandom.current().nextDouble() * 0.2;
            double vz = Math.sin(angle) * 0.3;

            // Firework jumping sparks 
            engine.createParticle(ParticleTypes.FIREWORK, center.x, center.y, center.z, vx * 1.5, vy * 1.5, vz * 1.5);
            
            // Linear flying sparks bursting outward
            engine.createParticle(ModParticles.LINEAR_SPARK.get(), center.x, center.y, center.z, vx * 2.5, vy * 1.8, vz * 2.5);
        }
    }

    private static void spawnEnergyPillar(Vec3 base, float height) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        int steps = (int) (height * 10);
        for (int i = 0; i < steps; i++) {
            double y = base.y + (height / steps) * i;
            double spread = 0.4;
            double vx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
            double vz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
            double vy = 0.05 + i * 0.01;

            // Adding lava/scrape for the central energy column
            engine.createParticle(
                    ParticleTypes.LAVA,
                    base.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread,
                    y,
                    base.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread,
                    vx,
                    vy,
                    vz
            );
            
            if (i % 2 == 0) {
                 engine.createParticle(
                    ParticleTypes.FIREWORK,
                    base.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread,
                    y,
                    base.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread,
                    vx * 2.5,
                    0.2,
                    vz * 2.5
                 );
            }
        }
    }

    public static void playImpactSparks(Vec3 pos, Vec3 direction) {
        if (!isClient()) return;

        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        int count = 15 + ThreadLocalRandom.current().nextInt(10); // More sparks to make it look forceful
        
        Vec3 dir = direction.normalize();

        for (int i = 0; i < count; i++) {
            double offsetX = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.4;
            double offsetY = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.4;
            double offsetZ = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.4;
            
            double x = pos.x + offsetX;
            double y = pos.y + offsetY;
            double z = pos.z + offsetZ;
            
            // Push mostly in the hit direction with some random spread
            double spread = 0.6;
            double vx = dir.x * 0.8 + (ThreadLocalRandom.current().nextFloat() - 0.5f) * spread;
            double vy = dir.y * 0.8 + (ThreadLocalRandom.current().nextFloat() - 0.5f) * spread + 0.2;
            double vz = dir.z * 0.8 + (ThreadLocalRandom.current().nextFloat() - 0.5f) * spread;

            // Only spawn the custom stretched spark, no bouncy round dots
            engine.createParticle(ModParticles.LINEAR_SPARK.get(), x, y, z, vx, vy, vz);
        }
    }

    private static boolean isClient() {
        return FMLLoader.getDist() == Dist.CLIENT && Minecraft.getInstance().level != null;
    }
}