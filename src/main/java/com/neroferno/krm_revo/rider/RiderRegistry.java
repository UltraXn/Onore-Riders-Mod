package com.neroferno.krm_revo.rider;

import com.neroferno.krm_revo.item.ModItems;
import com.neroferno.krm_revo.rider.ability.RiderKickAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiderRegistry {

    private static final Map<String, RiderDefinition> RIDERS = new HashMap<>();

    public static final RiderDefinition KUUGA = register(new RiderDefinition(
            "kuuga",
            ModItems.DRIVER_BELT,
            ModItems.KUUGA_SUIT_VISUAL,
            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png"),
            List.of(new RiderKickAbility())
    ));

    private static RiderDefinition register(RiderDefinition def) {
        RIDERS.put(def.getId(), def);
        return def;
    }

    public static RiderDefinition get(String id) {
        return RIDERS.get(id);
    }

    /**
     * Reverse lookup: finds the Rider whose Driver item matches the given
     * item, e.g. to resolve which Rider a player has active just from
     * what's sitting in their belt slot. Returns null if no registered
     * Rider uses this item as its Driver.
     */
    public static RiderDefinition getByDriverItem(Item item) {
        if (item == null) return null;
        for (RiderDefinition def : RIDERS.values()) {
            if (def.getDriver() == item) {
                return def;
            }
        }
        return null;
    }

    public static Collection<RiderDefinition> all() {
        return RIDERS.values();
    }
}
