package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.network.GrowingPunchAnimationPacket;
import net.bettercombat.client.particle.SlashParticleUtil;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.Iterator;

public final class GrowingPunchAnimationPacketHandler {
    private static final ResourceLocation FIST_PUNCH_SOUND =
            ResourceLocation.fromNamespaceAndPath("bettercombat", "fist_punch");
    private static final float[] PUNCH_PITCH = {1.1F, 1.2F, 1.3F};
    private static final long PREDICTION_ACK_WINDOW_TICKS = 10L;
    private static final ArrayDeque<PredictedPunch> LOCAL_PREDICTIONS = new ArrayDeque<>();

    private GrowingPunchAnimationPacketHandler() {}

    public static void handle(GrowingPunchAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getEntity(packet.playerId()) instanceof AbstractClientPlayer player
                && packet.attackIndex() >= 0
                && packet.attackIndex() < 3) {
            playAttackSound(minecraft, player, packet.attackIndex());
            if (minecraft.player == null
                    || player.getId() != minecraft.player.getId()
                    || !consumePrediction(player, packet.attackIndex(), packet.heavy())) {
                play(player, packet.attackIndex(), packet.heavy());
            }
        }
    }

    public static void playPredicted(AbstractClientPlayer player, int attackIndex, boolean heavy) {
        if (attackIndex < 0 || attackIndex >= 3) {
            return;
        }

        prunePredictions(player.level().getGameTime());
        LOCAL_PREDICTIONS.addLast(new PredictedPunch(
                attackIndex, heavy, player.level().getGameTime()));
        play(player, attackIndex, heavy);
    }

    public static void play(AbstractClientPlayer player, int attackIndex, boolean heavy) {
        if (attackIndex < 0 || attackIndex >= 3) {
            return;
        }

        if (heavy) {
            var attackHand = PlayerAttackHelper.getCurrentAttack(player, 0);
            if (attackHand != null) {
                var trail = SlashParticleUtil.trailParticlesFromAttack(attackHand);
                SlashParticleUtil.spawnParticles(
                        player,
                        attackHand.isOffHand(),
                        (float) PlayerAttackHelper.getStaticRange(player, attackHand.itemStack()),
                        trail,
                        SlashParticleUtil.appearanceFromItemStack(attackHand.itemStack())
                );
            }
            SuitRenderLayer.playGrowingHeavyPunch(player, attackIndex);
        } else {
            SuitRenderLayer.playGrowingPunch(player, attackIndex);
        }
    }

    private static boolean consumePrediction(AbstractClientPlayer player, int attackIndex, boolean heavy) {
        long gameTime = player.level().getGameTime();
        prunePredictions(gameTime);
        Iterator<PredictedPunch> predictions = LOCAL_PREDICTIONS.iterator();
        while (predictions.hasNext()) {
            PredictedPunch prediction = predictions.next();
            if (prediction.attackIndex() == attackIndex && prediction.heavy() == heavy) {
                predictions.remove();
                return true;
            }
        }
        return false;
    }

    private static void prunePredictions(long gameTime) {
        while (!LOCAL_PREDICTIONS.isEmpty()
                && gameTime - LOCAL_PREDICTIONS.peekFirst().gameTime() > PREDICTION_ACK_WINDOW_TICKS) {
            LOCAL_PREDICTIONS.removeFirst();
        }
    }

    private record PredictedPunch(int attackIndex, boolean heavy, long gameTime) {}

    private static void playAttackSound(Minecraft minecraft, AbstractClientPlayer player, int attackIndex) {
        BuiltInRegistries.SOUND_EVENT.getOptional(FIST_PUNCH_SOUND).ifPresentOrElse(
                sound -> minecraft.getSoundManager().play(new SimpleSoundInstance(
                        sound,
                        SoundSource.PLAYERS,
                        0.8F,
                        PUNCH_PITCH[attackIndex],
                        RandomSource.create(),
                        player.getX(),
                        player.getY(),
                        player.getZ()
                )),
                () -> com.neroferno.krm_onore.KRMRevoMod.LOGGER.error(
                        "Could not play punch sound: Better Combat sound event {} is not registered.",
                        FIST_PUNCH_SOUND
                )
        );
    }
}
