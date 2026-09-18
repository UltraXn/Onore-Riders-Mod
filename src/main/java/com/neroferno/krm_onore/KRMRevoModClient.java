package com.neroferno.krm_onore;

import com.neroferno.krm_onore.client.ModKeybindings;
import com.neroferno.krm_onore.client.screen.RiderScreen;
import com.neroferno.krm_onore.menu.ModMenuTypes;

import com.neroferno.krm_onore.network.OpenRiderMenuPacket;
import com.neroferno.krm_onore.network.AbilityPacket;
import com.neroferno.krm_onore.network.TransformPacket;
import com.neroferno.krm_onore.particle.ModParticles;
import com.neroferno.krm_onore.client.particle.FlyingSparkParticle;
import com.neroferno.krm_onore.client.particle.SparkFlashParticle;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.client.renderer.layer.BeltRenderLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.minecraft.client.player.Input;


/**
 * Client-only mod class.
 */
@Mod(value = KRMRevoMod.MODID, dist = Dist.CLIENT)
@SuppressWarnings({"null", "removal"})
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KRMRevoModClient {

    private static final java.util.WeakHashMap<AbstractClientPlayer, ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>> animationLayers = new java.util.WeakHashMap<>();

    public static ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation> getAnimationLayer(AbstractClientPlayer player) {
        return animationLayers.get(player);
    }

    public KRMRevoModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
                ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation> layer = new ModifierLayer<>();
                animationStack.addAnimLayer(1500, layer);
                animationLayers.put(player, layer);
                KRMRevoMod.LOGGER.debug("Registered custom persistent animation layer for a player instance");
            });
            KRMRevoMod.LOGGER.info("Onore Rider — Client setup complete.");
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybindings.TRANSFORM_KEY);
        event.register(ModKeybindings.ABILITY_KEY);
        event.register(ModKeybindings.SKILL_TREE_KEY);
        KRMRevoMod.LOGGER.info("Onore Rider — Keybindings registered.");
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RIDER_MENU.get(), RiderScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FLYING_SPARK.get(), FlyingSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARK_FLASH.get(), SparkFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.LINEAR_SPARK.get(), com.neroferno.krm_onore.client.particle.LinearSparkParticle.Provider::new);
        KRMRevoMod.LOGGER.info("Onore Rider — Custom Particle Providers registered.");
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
        KRMRevoMod.LOGGER.info("Onore Rider — Custom Render Layers registered.");
    }
}

/**
 * Game bus events handled in a separate subscriber to avoid confusion.
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class KRMRevoGameClientEvents {
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getInt("RiderKickState") > 0) {
            Input input = event.getInput();
            if (input != null) {
                input.left = false;
                input.right = false;
                input.up = false;
                input.down = false;
                input.leftImpulse = 0.0F;
                input.forwardImpulse = 0.0F;
                input.jumping = false;
                input.shiftKeyDown = false;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isClient = player.level().isClientSide();

        if (isClient) {
            // Client-side visual tick for all players
            int riderKickState = player.getPersistentData().getInt("RiderKickState");
            if (riderKickState > 0) {
                spawnRiderKickParticles(player, riderKickState);
            }

            // Client-side target stasis freezing to prevent client prediction drift and gravity falling
            int targetId = player.getPersistentData().getInt("RiderKickTargetId");
            if (targetId != 0) {
                net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                if (targetEntity instanceof net.minecraft.world.entity.LivingEntity target && target.isAlive()) {
                    double tx = player.getPersistentData().getDouble("RiderKickTargetX");
                    double ty = player.getPersistentData().getDouble("RiderKickTargetY");
                    double tz = player.getPersistentData().getDouble("RiderKickTargetZ");
                    target.setDeltaMovement(Vec3.ZERO);
                    target.setPos(tx, ty, tz);
                    target.hurtMarked = true;
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setNoAi(true);
                    }
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 100, false, false));
                }
            }
            
            // Only process key clicks and local movement simulation for the local player
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer localPlayer = mc.player;
            if (localPlayer != null && player == localPlayer) {
                // Client-side movement replication for local player to prevent rubberbanding/desync
                int prevKickState = player.getPersistentData().getInt("PrevRiderKickState");
                int clientTicks = player.getPersistentData().getInt("ClientRiderKickTicks");
                
                if (riderKickState != prevKickState) {
                    clientTicks = 0;
                    player.getPersistentData().putInt("PrevRiderKickState", riderKickState);
                } else if (riderKickState > 0) {
                    clientTicks++;
                }
                player.getPersistentData().putInt("ClientRiderKickTicks", clientTicks);

                if (riderKickState > 0) {
                    if (localPlayer.input != null) {
                        localPlayer.input.left = false;
                        localPlayer.input.right = false;
                        localPlayer.input.up = false;
                        localPlayer.input.down = false;
                        localPlayer.input.leftImpulse = 0.0F;
                        localPlayer.input.forwardImpulse = 0.0F;
                        localPlayer.input.jumping = false;
                        localPlayer.input.shiftKeyDown = false;
                    }
                }

                if (riderKickState == 1) { // Phase 1: High Leap
                    if (clientTicks >= 8) {
                        player.setDeltaMovement(Vec3.ZERO);
                    }
                } else if (riderKickState == 2) { // Phase 2: Dive Kick
                    if (clientTicks >= 4) {
                        Vec3 lookVec = player.getLookAngle();
                        double diveY = lookVec.y;
                        if (diveY > -0.4D) diveY = -0.8D; // force diving down
                        Vec3 diveVec = new Vec3(lookVec.x, diveY, lookVec.z).normalize().scale(4.0D); // Upgraded to 4.0D!
                        player.setDeltaMovement(diveVec);
                    }
                } else if (riderKickState == 3) { // Phase 3: Impact Pause
                    player.setDeltaMovement(Vec3.ZERO);
                }

                if (ModKeybindings.TRANSFORM_KEY.consumeClick()) {
                    PacketDistributor.sendToServer(new TransformPacket());
                }

                if (ModKeybindings.ABILITY_KEY.consumeClick()) {
                    // Abilities only work when no weapon is equipped (per design doc) and player is transformed
                    if (localPlayer.getMainHandItem().isEmpty() && TransformationHelper.isTransformed(localPlayer)) {
                        PacketDistributor.sendToServer(new AbilityPacket("rider_kick"));
                    }
                }

                if (ModKeybindings.SKILL_TREE_KEY.consumeClick()) {
                    PacketDistributor.sendToServer(new OpenRiderMenuPacket());
                }
            }
        }
    }

    private static void spawnRiderKickParticles(Player player, int state) {
        net.minecraft.client.particle.ParticleEngine engine = Minecraft.getInstance().particleEngine;
        if (engine == null) return;
        
        java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();
        
        if (state == 1) { // Leap / Charging Phase
            // Swirling sparks rising up around the player's body
            for (int i = 0; i < 3; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.5D + rand.nextDouble() * 0.3D;
                double px = player.getX() + Math.cos(angle) * radius;
                double py = player.getY() + rand.nextDouble() * player.getBbHeight();
                double pz = player.getZ() + Math.sin(angle) * radius;
                
                // Slow rising sparks
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), px, py, pz, 0, 0.15D, 0);
                if (rand.nextBoolean()) {
                    engine.createParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.05D, 0);
                }
            }
        } else if (state == 2) { // Dive Kick Phase
            // Dense fiery trail and custom sparks flowing from the player's feet (especially the right leg!)
            Vec3 footPos = player.position().add(0, 0.15D, 0);
            
            for (int i = 0; i < 5; i++) {
                double rx = (rand.nextDouble() - 0.5D) * 0.4D;
                double ry = (rand.nextDouble() - 0.5D) * 0.2D;
                double rz = (rand.nextDouble() - 0.5D) * 0.4D;
                
                // Fiery combustion
                engine.createParticle(ParticleTypes.FLAME, footPos.x + rx, footPos.y + ry, footPos.z + rz, 
                        -player.getDeltaMovement().x * 0.2, -player.getDeltaMovement().y * 0.2, -player.getDeltaMovement().z * 0.2);
                
                // Sparks shooting backwards from the kick direction
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), footPos.x + rx, footPos.y + ry, footPos.z + rz,
                        -player.getDeltaMovement().x * 0.4 + (rand.nextDouble() - 0.5D) * 0.2D,
                        -player.getDeltaMovement().y * 0.4 + (rand.nextDouble() - 0.5D) * 0.2D,
                        -player.getDeltaMovement().z * 0.4 + (rand.nextDouble() - 0.5D) * 0.2D);
            }
            
            // Lava/embers drippings
            if (rand.nextInt(3) == 0) {
                engine.createParticle(ParticleTypes.LAVA, footPos.x, footPos.y, footPos.z, 0, 0, 0);
            }
        } else if (state == 3) { // Landing Slide / Delayed explosion charging phase
            // Flame/spark circles expanding on the ground!
            Vec3 pos = player.position();
            double radius = 1.0D + (player.tickCount % 5) * 0.3D;
            
            for (int i = 0; i < 4; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double px = pos.x + Math.cos(angle) * radius;
                double pz = pos.z + Math.sin(angle) * radius;
                
                engine.createParticle(ParticleTypes.FLAME, px, pos.y + 0.1D, pz, 0, 0.02D, 0);
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), px, pos.y + 0.1D, pz, 
                        Math.cos(angle) * 0.1D, 0.05D, Math.sin(angle) * 0.1D);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        if (TransformationHelper.isTransformed(player)) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.setAllVisible(false);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.setAllVisible(true);
    }
}

