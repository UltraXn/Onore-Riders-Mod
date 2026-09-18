package com.neroferno.krm_revo.rider;

import com.neroferno.krm_revo.rider.ability.RiderAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/**
 * Static definition of a Rider: which items it uses, which texture its suit
 * renders with, and which abilities it grants.
 */
public class RiderDefinition {

    private final String id;
    private final Supplier<? extends Item> driver;
    private final Supplier<? extends Item> suit;
    private final ResourceLocation suitTexture;
    private final List<RiderAbility> abilities;

    public RiderDefinition(String id,
                            Supplier<? extends Item> driver,
                            Supplier<? extends Item> suit,
                            ResourceLocation suitTexture,
                            List<RiderAbility> abilities) {
        this.id = id;
        this.driver = driver;
        this.suit = suit;
        this.suitTexture = suitTexture;
        this.abilities = abilities;
    }

    public String getId() {
        return id;
    }

    public Item getDriver() {
        return driver.get();
    }

    public Item getSuit() {
        return suit.get();
    }

    public ResourceLocation getSuitTexture() {
        return suitTexture;
    }

    public List<RiderAbility> getAbilities() {
        return abilities;
    }

    public RiderAbility getAbility(String abilityId) {
        return abilities.stream()
                .filter(a -> a.getId().equals(abilityId))
                .findFirst()
                .orElse(null);
    }
}
