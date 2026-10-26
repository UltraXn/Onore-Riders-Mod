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
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderStateData;
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
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.minecraft.client.player.Input;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import com.neroferno.krm_onore.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_onore.item.ModItems;
import com.neroferno.krm_onore.item.SuitVisualItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;


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

    @SubscribeEvent
    public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR,
                ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "rider_energy_overlay"),
                com.neroferno.krm_onore.client.gui.RiderEnergyOverlay::render
        );
        KRMRevoMod.LOGGER.info("Onore Rider — Custom GUI Layers registered.");
    }
}

/**
 * Game bus events handled in a separate subscriber to avoid confusion.
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class KRMRevoGameClientEvents {
    private static SuitVisualRenderer firstPersonSuitRenderer;
    private static ItemStack firstPersonDummySuit;
    // DEBUG temporal de la mano en primera persona (quitar cuando se resuelva)
    private static final boolean FP_ARM_DEBUG = false;
    private static long fpArmLastLogTick = -100;
    // Tick de kuuga_transform en el que arranca el sonido (0.5 s * 20). Igual que CORE_FADE_START_TICKS.
    private static final int TRANSFORM_SOUND_TICK = TransformationHelper.TRANSFORM_SOUND_START_TICKS;
    // Si es true, el brazo de primera persona ignora el sombreado direccional (se ve el color real del PNG).
    // Ponlo en false para volver al sombreado vanilla.
    private static final boolean FP_ARM_FLAT_LIGHT = true;

    /**
     * Envuelve un VertexConsumer y sustituye todas las normales por (0, 1, 0). El shader de entidades calcula
     * la luz difusa con esa normal, así que todas las caras reciben el brillo máximo. La luz del mundo
     * (lightmap / packedLight) no se toca.
     */
    private static final class FlatLitVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        FlatLitVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(0.0F, 1.0F, 0.0F);
            return this;
        }
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (!TransformationHelper.isTransformed(player)) return;

        if (firstPersonSuitRenderer == null) {
            firstPersonSuitRenderer = new SuitVisualRenderer();
            firstPersonDummySuit = new ItemStack(ModItems.KUUGA_SUIT_VISUAL.get());
        }

        HumanoidArm arm = event.getArm();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        Minecraft mc = Minecraft.getInstance();
        PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> parentModel = playerRenderer.getModel();

        parentModel.attackTime = 0.0F;
        parentModel.crouching = false;
        parentModel.swimAmount = 0.0F;
        // AgeableListModel.young vale true por defecto y solo se corrige cuando el renderer dibuja a ese
        // jugador en tercera persona. Si entras al mundo en primera persona, GeckoLib ve young=true y
        // aplica la escala de bebé (x0.5) al brazo.
        parentModel.young = false;
        parentModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        if (arm == HumanoidArm.RIGHT) {
            parentModel.rightArm.xRot = 0.0F;
            parentModel.rightArm.xScale = 1.0F;
            parentModel.rightArm.yScale = 1.0F;
            parentModel.rightArm.zScale = 1.0F;
        } else {
            parentModel.leftArm.xRot = 0.0F;
            parentModel.leftArm.xScale = 1.0F;
            parentModel.leftArm.yScale = 1.0F;
            parentModel.leftArm.zScale = 1.0F;
        }

        firstPersonSuitRenderer.setFirstPersonArm(arm);
        firstPersonSuitRenderer.setFirstPersonFlatLight(FP_ARM_FLAT_LIGHT);
        firstPersonSuitRenderer.prepForRender(player, firstPersonDummySuit, EquipmentSlot.CHEST, parentModel);

        int form = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE).getForm() : 1;
        ResourceLocation textureRes = form == 1
            ? ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/growing_suit.png")
            : ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");

        RenderType renderType = firstPersonSuitRenderer.getRenderType((SuitVisualItem) firstPersonDummySuit.getItem(),
                                                                     textureRes,
                                                                     bufferSource, 0.0F);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        if (FP_ARM_FLAT_LIGHT) {
            // Las caras que miran "de espaldas" a las luces direccionales de Minecraft se multiplican por ~0.4,
            // y un PNG blanco (p. ej. la placa del puño) se ve gris oscuro. Aquí se fuerza una normal hacia
            // arriba para que el brazo de primera persona se dibuje con el color real de la textura.
            vertexConsumer = new FlatLitVertexConsumer(vertexConsumer);
        }

        poseStack.pushPose();
        firstPersonSuitRenderer.renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        if (FP_ARM_DEBUG && player.tickCount - fpArmLastLogTick >= 20) {
            fpArmLastLogTick = player.tickCount;
            try {
                net.minecraft.client.model.geom.ModelPart part = arm == HumanoidArm.RIGHT ? parentModel.rightArm : parentModel.leftArm;
                org.joml.Vector3f ps = new org.joml.Vector3f();
                poseStack.last().pose().getScale(ps);
                var st = player.getData(ModAttachments.RIDER_STATE);
                StringBuilder sb = new StringBuilder("[FPArm] tick=").append(player.tickCount)
                        .append(" arm=").append(arm)
                        .append(" poseScale=").append(ps)
                        .append(" part pos=(").append(part.x).append(',').append(part.y).append(',').append(part.z).append(')')
                        .append(" rot=(").append(part.xRot).append(',').append(part.yRot).append(',').append(part.zRot).append(')')
                        .append(" scale=(").append(part.xScale).append(',').append(part.yScale).append(',').append(part.zScale).append(')')
                        .append(" | form=").append(st.getForm())
                        .append(" hasAnimStart=").append(st.hasTransformAnimationStartTick())
                        .append(" hasStart=").append(st.hasTransformStartTick())
                        .append(" start=").append(st.getTransformStartTick())
                        .append(" reveal=").append(SuitVisualRenderer.getGrowingRevealElapsedTicks(player, 0.0F))
                        .append(" young=").append(parentModel.young);
                var geo = firstPersonSuitRenderer.getGeoModel();
                for (String n : new String[]{"steve", "RightArm", "LeftArm", "Body", "armorRightArm", "armorLeftArm"}) {
                    geo.getBone(n).ifPresent(b -> sb.append(" | ").append(n)
                            .append(" scale=(").append(b.getScaleX()).append(',').append(b.getScaleY()).append(',').append(b.getScaleZ()).append(')')
                            .append(" pos=(").append(b.getPosX()).append(',').append(b.getPosY()).append(',').append(b.getPosZ()).append(')')
                            .append(" rot=(").append(b.getRotX()).append(',').append(b.getRotY()).append(',').append(b.getRotZ()).append(')')
                            .append(" hidden=").append(b.isHidden()));
                }
                KRMRevoMod.LOGGER.info(sb.toString());
            } catch (Throwable t) {
                KRMRevoMod.LOGGER.warn("[FPArm] debug failed", t);
            }
        }
        poseStack.popPose();

        firstPersonSuitRenderer.clearFirstPerson();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        RiderStateData riderState = player.hasData(ModAttachments.RIDER_STATE)
                ? player.getData(ModAttachments.RIDER_STATE)
                : null;
        boolean transforming = riderState != null
                && riderState.hasTransformAnimationStartTick()
                && player.tickCount - riderState.getTransformAnimationStartTick() >= 0
                && player.tickCount - riderState.getTransformAnimationStartTick()
                        <= riderState.getTransformAnimationTicks() + (int) SuitVisualRenderer.GROWING_REVEAL_TOTAL_TICKS;

        boolean lockForTransformation = transforming
                && riderState.getTransformAnimationTicks() != TransformationHelper.TRANSFORM_ANIMATION_FAST_TICKS;
        if (lockForTransformation || (riderState != null && riderState.getRiderKickState() > 0)) {
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
            RiderStateData riderState = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE) : null;
            // Sonido del henshin: arranca en 0.5 s de kuuga_transform (no en kuuga_transform_fast).
            if (riderState != null
                    && riderState.hasTransformAnimationStartTick()
                    && !riderState.isTransformSoundPlayed()
                    && riderState.getTransformAnimationTicks() != TransformationHelper.TRANSFORM_ANIMATION_FAST_TICKS) {
                long soundElapsed = player.tickCount - riderState.getTransformAnimationStartTick();
                if (soundElapsed >= TRANSFORM_SOUND_TICK) {
                    riderState.setTransformSoundPlayed(true);
                    // Si llegamos muy tarde (p. ej. al cargar el chunk) no lo reproducimos a medias.
                    if (soundElapsed <= TRANSFORM_SOUND_TICK + 5) {
                        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                                com.neroferno.krm_onore.event.ModSounds.KUUGA_TRANSFORM.get(),
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F, false);
                    }
                }
            }

            // Client-side visual tick for all players
            int riderKickState = riderState != null ? riderState.getRiderKickState() : 0;
            if (riderKickState > 0) {
                spawnRiderKickParticles(player, riderKickState);
            }

            // Client-side target immobilization to prevent client prediction drift
            int targetId = riderState != null ? riderState.getRiderKickTargetId() : 0;
            if (targetId != 0) {
                net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                if (targetEntity instanceof net.minecraft.world.entity.LivingEntity target && target.isAlive()) {
                    target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                    target.hurtMarked = true;
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setNoAi(true);
                    }
                    if (!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 100, false, false));
                    }
                }
            }

            // Only process key clicks and local movement simulation for the local player
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer localPlayer = mc.player;
            if (localPlayer != null && player == localPlayer && riderState != null) {
                // Client-side movement replication for local player to prevent rubberbanding/desync
                int prevKickState = riderState.getPrevRiderKickState();
                int clientTicks = riderState.getClientRiderKickTicks();

                if (riderKickState != prevKickState) {
                    clientTicks = 0;
                    riderState.setPrevRiderKickState(riderKickState);
                } else if (riderKickState > 0) {
                    clientTicks++;
                }
                riderState.setClientRiderKickTicks(clientTicks);

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

                if (riderKickState == 1) { // Phase 1: High Leap & Tokusatsu Apex Hover
                    // When upward momentum begins flattening (approaching apex), apply gentle hover float
                    if (player.getDeltaMovement().y <= 0.08D && player.getDeltaMovement().y > -0.05D) {
                        player.setDeltaMovement(player.getDeltaMovement().x * 0.7D, 0.02D, player.getDeltaMovement().z * 0.7D);
                    }
                } else if (riderKickState == 2) { // Phase 2: Curved Bézier Dive Kick
                    // Smooth cinematic camera target-lock towards target's torso
                    double tx = riderState.getRiderKickTargetX();
                    double ty = riderState.getRiderKickTargetY();
                    double tz = riderState.getRiderKickTargetZ();
                    if (tx != 0 || ty != 0 || tz != 0) {
                        double dx = tx - localPlayer.getX();
                        double dy = (ty + 0.9D) - localPlayer.getEyeY(); // Aim at enemy torso/chest
                        double dz = tz - localPlayer.getZ();
                        double distHoriz = Math.sqrt(dx * dx + dz * dz);
                        float targetPitch = (float) (-(Mth.atan2(dy, distHoriz) * (180.0F / (float) Math.PI)));
                        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F;

                        localPlayer.setXRot(Mth.rotLerp(0.25F, localPlayer.getXRot(), targetPitch));
                        localPlayer.setYRot(Mth.rotLerp(0.25F, localPlayer.getYRot(), targetYaw));
                    }
                } else if (riderKickState == 3) { // Phase 3: Hit-Stop & Impact Pause
                    player.setDeltaMovement(Vec3.ZERO);
                }

                if (ModKeybindings.TRANSFORM_KEY.consumeClick()) {
                    Input input = localPlayer.input;
                    boolean moving = localPlayer.isSprinting()
                            || (input != null && (input.up || input.down || input.left || input.right));
                    PacketDistributor.sendToServer(new TransformPacket(moving));
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

        if (state == 1) { // Leap / Apex Hover Phase
            // Swirling sparks concentrating around the player's body and right leg
            for (int i = 0; i < 3; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.45D + rand.nextDouble() * 0.25D;
                double px = player.getX() + Math.cos(angle) * radius;
                double py = player.getY() + rand.nextDouble() * player.getBbHeight();
                double pz = player.getZ() + Math.sin(angle) * radius;

                // Slow rising sparks
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), px, py, pz, 0, 0.15D, 0);
                if (rand.nextBoolean()) {
                    engine.createParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.05D, 0);
                }
            }
        } else if (state == 2) { // Curved Bézier Dive Kick Phase
            // Dense fiery trail and custom sparks flowing behind the player's kicking foot
            Vec3 footPos = player.position().add(0, 0.15D, 0);
            Vec3 vel = player.getDeltaMovement();

            for (int i = 0; i < 6; i++) {
                double rx = (rand.nextDouble() - 0.5D) * 0.4D;
                double ry = (rand.nextDouble() - 0.5D) * 0.2D;
                double rz = (rand.nextDouble() - 0.5D) * 0.4D;

                // Fiery combustion trailing backwards along the velocity vector
                engine.createParticle(ParticleTypes.FLAME, footPos.x + rx, footPos.y + ry, footPos.z + rz,
                        -vel.x * 0.18D, -vel.y * 0.18D, -vel.z * 0.18D);

                // Linear sparks shooting backwards with slight dispersion
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), footPos.x + rx, footPos.y + ry, footPos.z + rz,
                        -vel.x * 0.35D + (rand.nextDouble() - 0.5D) * 0.15D,
                        -vel.y * 0.35D + (rand.nextDouble() - 0.5D) * 0.15D,
                        -vel.z * 0.35D + (rand.nextDouble() - 0.5D) * 0.15D);
            }

            // Spark flashes bursting occasionally along the trajectory
            if (rand.nextFloat() < 0.25F) {
                engine.createParticle(ModParticles.SPARK_FLASH.get(), footPos.x, footPos.y, footPos.z, 0, 0, 0);
            }
            // Lava/embers drippings
            if (rand.nextInt(3) == 0) {
                engine.createParticle(ParticleTypes.LAVA, footPos.x, footPos.y, footPos.z, 0, 0, 0);
            }
        } else if (state == 3) { // Hit-Stop (5 ticks) & Landing Slide
            Vec3 pos = player.position();
            // Intense spark flashes at the impact point during hit-stop
            for (int i = 0; i < 5; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double speed = 0.2D + rand.nextDouble() * 0.3D;
                engine.createParticle(ModParticles.LINEAR_SPARK.get(), pos.x, pos.y + 0.8D, pos.z,
                        Math.cos(angle) * speed, (rand.nextDouble() - 0.3D) * 0.25D, Math.sin(angle) * speed);
            }

            if (rand.nextBoolean()) {
                engine.createParticle(ModParticles.SPARK_FLASH.get(), pos.x, pos.y + 0.8D, pos.z, 0, 0, 0);
            }

            // Expanding flame/spark circles on the ground
            double radius = 1.0D + (player.tickCount % 5) * 0.3D;
            for (int i = 0; i < 4; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double px = pos.x + Math.cos(angle) * radius;
                double pz = pos.z + Math.sin(angle) * radius;

                engine.createParticle(ParticleTypes.FLAME, px, pos.y + 0.1D, pz, 0, 0.02D, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        if (SuitVisualRenderer.shouldHideVanillaBody(player, event.getPartialTick())) {
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
