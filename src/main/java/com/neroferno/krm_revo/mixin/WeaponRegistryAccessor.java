package com.neroferno.krm_revo.mixin;

import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor mixin to expose WeaponRegistry's package-private maps.
 */
@Mixin(value = WeaponRegistry.class, remap = false)
public interface WeaponRegistryAccessor {

    @Accessor("containers")
    static Map<ResourceLocation, AttributesContainer> getContainers() {
        throw new AssertionError("Mixin not applied");
    }

    @Accessor("registrations")
    static Map<ResourceLocation, WeaponAttributes> getRegistrations() {
        throw new AssertionError("Mixin not applied");
    }
}
