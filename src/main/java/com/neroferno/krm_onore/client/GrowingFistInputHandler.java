package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.combat.RiderFistTiming;
import com.neroferno.krm_onore.network.GrowingLeftAttackStartPacket;
import com.neroferno.krm_onore.network.RightClickFistPacket;
import com.neroferno.krm_onore.network.TransformationHelper;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class GrowingFistInputHandler {
    private static final Map<LocalPlayer, PredictionState> PREDICTIONS = new WeakHashMap<>();

    private GrowingFistInputHandler() {}

    /** Llamado al llegar la animacion del Especial 3: descarta golpes predichos/en buffer y bloquea nuevos hasta que termine. */
    public static void blockForSpecial(LocalPlayer player, int ticks) {
        long until = player.level().getGameTime() + ticks;
        PredictionState state = PREDICTIONS.computeIfAbsent(player, ignored -> new PredictionState());
        state.hasBufferedAttack = false;
        state.specialUntil = until;
        state.nextAttackTime = Math.max(state.nextAttackTime, until);
    }

    /** Especial 1: anula WASD/agacharse (el servidor mueve al jugador). */
    private static long rootUntil;
    private static boolean rootedJumpPressed;
    private static boolean autoJumpOverridden;
    private static boolean previousAutoJump;

    private static float lockYaw;
    private static float lockPitch;
    /** Cuanto ha girado la camara respecto al personaje (grados); vuelve a 0 al terminar la llave. */
    private static float camYawOffset;
    private static float camPitchOffset;
    private static long lastCamNanos;

    public static void rootFor(LocalPlayer player, int ticks) {
        if (!isRooted(player)) {
            restoreAutoJump();
            // Al empezar la llave el personaje queda fijo mirando hacia donde miraba; la camara si puede girar.
            lockYaw = player.getYRot();
            lockPitch = player.getXRot();
            camYawOffset = 0.0F;
            camPitchOffset = 0.0F;
            previousAutoJump = Minecraft.getInstance().options.autoJump().get();
            Minecraft.getInstance().options.autoJump().set(false);
            autoJumpOverridden = true;
        }
        rootUntil = player.level().getGameTime() + ticks;
    }

    /**
     * Especial 1: el personaje no gira pero la camara si. Lo que el raton giro este fotograma se pasa a un desfase
     * de camara y el personaje vuelve a su direccion fija. Al terminar la llave el desfase se desvanece y la camara
     * vuelve a donde miraba el personaje.
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        long now = System.nanoTime();
        float dt = lastCamNanos == 0L ? 0.0F : Math.min(0.1F, (now - lastCamNanos) / 1.0E9F);
        lastCamNanos = now;

        if (isRooted(player)) {
            camYawOffset += player.getYRot() - lockYaw;
            camPitchOffset += player.getXRot() - lockPitch;
            camPitchOffset = Math.max(-90.0F - lockPitch, Math.min(90.0F - lockPitch, camPitchOffset));
            player.setYRot(lockYaw);
            player.setXRot(lockPitch);
            player.yRotO = lockYaw;
            player.xRotO = lockPitch;

            // Camara absoluta: direccion fija del personaje + lo que giro el raton (en vista frontal va invertida).
            boolean mirrored = Minecraft.getInstance().options.getCameraType().isMirrored();
            event.setYaw(lockYaw + camYawOffset + (mirrored ? 180.0F : 0.0F));
            float pitch = lockPitch + camPitchOffset;
            event.setPitch(mirrored ? -pitch : pitch);
            return;
        }

        if (camYawOffset == 0.0F && camPitchOffset == 0.0F) {
            return;
        }
        // Termino la llave: el desfase se desvanece y la camara vuelve a donde mira el personaje.
        float decay = (float) Math.exp(-dt * 14.0F);
        camYawOffset *= decay;
        camPitchOffset *= decay;
        if (Math.abs(camYawOffset) < 0.05F && Math.abs(camPitchOffset) < 0.05F) {
            camYawOffset = 0.0F;
            camPitchOffset = 0.0F;
            return;
        }
        event.setYaw(event.getYaw() + camYawOffset);
        event.setPitch(event.getPitch() + camPitchOffset);
    }

    public static void releaseRoot() {
        rootUntil = 0L;
        restoreAutoJump();
    }

    public static boolean isRooted(LocalPlayer player) {
        return player.level().getGameTime() < rootUntil;
    }

    private static void restoreAutoJump() {
        if (autoJumpOverridden) {
            Minecraft.getInstance().options.autoJump().set(previousAutoJump);
            autoJumpOverridden = false;
        }
    }

    public static boolean consumeRootedJumpPress(boolean pressed) {
        boolean justPressed = pressed && !rootedJumpPressed;
        if (pressed) {
            rootedJumpPressed = true;
        }
        return justPressed;
    }

    public static boolean suppressJumpUntilReleased(boolean pressed) {
        if (!pressed) {
            rootedJumpPressed = false;
            return false;
        }
        return rootedJumpPressed;
    }

    /** Libera de inmediato el bloqueo de golpes (Especial 2 cancelado antes de tiempo). */
    public static void releaseSpecial(LocalPlayer player) {
        PredictionState state = PREDICTIONS.computeIfAbsent(player, ignored -> new PredictionState());
        long now = player.level().getGameTime();
        state.hasBufferedAttack = false;
        state.specialUntil = now;
        state.nextAttackTime = now;
    }

    private static boolean isSpecialActive(LocalPlayer player) {
        PredictionState state = PREDICTIONS.get(player);
        return state != null && player.level().getGameTime() < state.specialUntil;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isUseItem()
                || player == null
                || !isGrowingFistAttack(player)) {
            return;
        }

        event.setCanceled(true);
        if (isSpecialActive(player) || SuitRenderLayer.isBlocking(player)) {
            return;
        }
        predictAttack(player, true);
        PacketDistributor.sendToServer(new RightClickFistPacket());
    }

    public static void onLeftClickStart(LocalPlayer player) {
        if (!isGrowingFistAttack(player) || isSpecialActive(player) || SuitRenderLayer.isBlocking(player)) {
            return;
        }

        predictAttack(player, false);
        PacketDistributor.sendToServer(new GrowingLeftAttackStartPacket());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            releaseRoot();
            PREDICTIONS.clear();
            return;
        }

        if (!isRooted(player)) {
            releaseRoot();
        }

        PredictionState state = PREDICTIONS.get(player);
        if (state == null || !state.hasBufferedAttack) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (!isGrowingFistAttack(player) || gameTime > state.bufferedExpiresAt || isSpecialActive(player)
                || SuitRenderLayer.isBlocking(player)) {
            state.hasBufferedAttack = false;
        } else if (gameTime >= state.nextAttackTime) {
            boolean heavy = state.bufferedHeavy;
            state.hasBufferedAttack = false;
            startPredictedAttack(player, state, heavy, gameTime);
        }
    }

    private static void predictAttack(LocalPlayer player, boolean heavy) {
        long gameTime = player.level().getGameTime();
        PredictionState state = PREDICTIONS.computeIfAbsent(player, ignored -> new PredictionState());
        if (gameTime >= state.nextAttackTime) {
            state.hasBufferedAttack = false;
            startPredictedAttack(player, state, heavy, gameTime);
        } else {
            state.bufferedHeavy = heavy;
            state.bufferedExpiresAt = state.nextAttackTime + 1;
            state.hasBufferedAttack = true;
        }
    }

    private static void startPredictedAttack(LocalPlayer player, PredictionState state, boolean heavy, long gameTime) {
        if (gameTime - state.nextAttackTime > RiderFistTiming.comboResetTicks()) {
            state.nextAttackIndex = 0;
        }

        int attackIndex = state.nextAttackIndex;
        state.nextAttackIndex = (state.nextAttackIndex + 1) % 3;
        state.nextAttackTime = gameTime + RiderFistTiming.animationTicks(heavy, attackIndex);
        GrowingPunchAnimationPacketHandler.playPredicted(player, attackIndex, heavy);
    }

    static boolean isGrowingFistAttack(LocalPlayer player) {
        return TransformationHelper.isTransformed(player)
                && player.hasData(ModAttachments.RIDER_STATE)
                && com.neroferno.krm_onore.combat.RiderFistCombat.usesFistSystem(
                        player.getData(ModAttachments.RIDER_STATE).getForm())
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()
                && TransformationHelper.isBeltEquipped(player);
    }

    private static final class PredictionState {
        private int nextAttackIndex;
        private long nextAttackTime;
        private boolean hasBufferedAttack;
        private boolean bufferedHeavy;
        private long bufferedExpiresAt;
        private long specialUntil;
    }
}
