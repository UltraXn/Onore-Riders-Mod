package com.neroferno.krm_revo;

import com.neroferno.krm_revo.client.ModKeybindings;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.neroferno.krm_revo.client.screen.RiderScreen;
import com.neroferno.krm_revo.menu.ModMenuTypes;

import com.neroferno.krm_revo.network.OpenRiderMenuPacket;
import com.neroferno.krm_revo.network.AbilityPacket;
import com.neroferno.krm_revo.network.TransformPacket;
import com.neroferno.krm_revo.particle.ModParticles;
import com.neroferno.krm_revo.client.particle.FlyingSparkParticle;
import com.neroferno.krm_revo.client.particle.SparkFlashParticle;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.neroferno.krm_revo.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_revo.client.renderer.layer.BeltRenderLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only mod class.
 */
@Mod(value = KRMRevoMod.MODID, dist = Dist.CLIENT)
@SuppressWarnings({"null", "removal"})
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KRMRevoModClient {

    public KRMRevoModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
                KRMRevoMod.LOGGER.debug("Registered animation stack for a player.");
            });
            KRMRevoMod.LOGGER.info("KRM: REvolution — Client setup complete.");
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybindings.TRANSFORM_KEY);
        event.register(ModKeybindings.ABILITY_KEY);
        event.register(ModKeybindings.SKILL_TREE_KEY);
        KRMRevoMod.LOGGER.info("KRM: REvolution — Keybindings registered.");
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RIDER_MENU.get(), RiderScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FLYING_SPARK.get(), FlyingSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARK_FLASH.get(), SparkFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.LINEAR_SPARK.get(), com.neroferno.krm_revo.client.particle.LinearSparkParticle.Provider::new);
        KRMRevoMod.LOGGER.info("KRM: REvolution — Custom Particle Providers registered.");
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new SuitRenderLayer(renderer));
                renderer.addLayer(new BeltRenderLayer(renderer));
            }
        }
        KRMRevoMod.LOGGER.info("KRM: REvolution — Custom Render Layers registered.");
    }
}

/**
 * Game bus events handled in a separate subscriber to avoid confusion.
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class KRMRevoGameClientEvents {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;

        if (ModKeybindings.TRANSFORM_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new TransformPacket());
        }

        if (ModKeybindings.ABILITY_KEY.consumeClick()) {
            // Abilities only work when no weapon is equipped (per design doc)
            if (mc.player != null && mc.player.getMainHandItem().isEmpty()) {
                PacketDistributor.sendToServer(new AbilityPacket("rider_kick"));
            }
        }

        if (ModKeybindings.SKILL_TREE_KEY.consumeClick()) {
            if (mc.player != null) {
                PacketDistributor.sendToServer(new OpenRiderMenuPacket());
            }
        }
    }
}
