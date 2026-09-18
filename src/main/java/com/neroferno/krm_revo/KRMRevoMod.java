package com.neroferno.krm_revo;

import com.mojang.logging.LogUtils;
import com.neroferno.krm_revo.block.ModBlocks;
import com.neroferno.krm_revo.event.ModSounds;
import com.neroferno.krm_revo.event.ModVillagers;
import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.menu.ModMenuTypes;
import com.neroferno.krm_revo.item.ModArmorMaterials;
import com.neroferno.krm_revo.item.ModItems;
import com.neroferno.krm_revo.network.ModNetwork;
import com.neroferno.krm_revo.particle.ModParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(KRMRevoMod.MODID)
@SuppressWarnings("null")
public class KRMRevoMod {

    public static final String MODID = "krm_revo";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Creative tab registry
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KRM_TAB =
            CREATIVE_MODE_TABS.register("krm_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.krm_revo"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.DRIVER_BELT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DRIVER_BELT.get());
                        output.accept(ModItems.RESEARCHER_DESK_ITEM.get());
                        output.accept(ModItems.MESA_DE_TRABAJO_ITEM.get());
                    }).build());

    public KRMRevoMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Onore Rider — Initializing...");

        // Register item and creative tab registries
        ModItems.ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);

        // Register network packets
        modEventBus.addListener(ModNetwork::register);


        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Onore Rider — Common Setup complete.");
        com.neroferno.krm_revo.seasons.heisei1.Kuuga.init();
        LOGGER.info("Onore Rider — Kamen Rider Engine initialized with {} riders.",
                com.neroferno.krm_revo.engine.RiderRegistry.getAll().size());
    }
}

