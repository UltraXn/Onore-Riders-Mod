package com.neroferno.krm_onore.particle;

import com.neroferno.krm_onore.KRMRevoMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KRMRevoMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LINEAR_SPARK =
            PARTICLE_TYPES.register("linear_spark", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLYING_SPARK =
            PARTICLE_TYPES.register("flying_spark", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK_FLASH =
            PARTICLE_TYPES.register("spark_flash", () -> new SimpleParticleType(true));
}
