package com.neroferno.krm_onore.event;

import com.google.common.collect.ImmutableSet;
import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom Villager Professions and POI Types.
 */
@SuppressWarnings("null")
public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KRMRevoMod.MODID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, KRMRevoMod.MODID);

    public static final DeferredHolder<PoiType, PoiType> RESEARCHER_POI =
            POI_TYPES.register("researcher_poi", () -> new PoiType(ImmutableSet.copyOf(ModBlocks.RESEARCHER_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> KRM_RESEARCHER =
            VILLAGER_PROFESSIONS.register("krm_researcher",
                    () -> new VillagerProfession(
                            "krm_researcher",
                            holder -> holder.value() == RESEARCHER_POI.get(),
                            holder -> holder.value() == RESEARCHER_POI.get(),
                            ImmutableSet.of(),
                            ImmutableSet.of(),
                            SoundEvents.VILLAGER_WORK_LEATHERWORKER
                    ));


    public static final void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}

