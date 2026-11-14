package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.network.BackwardInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Avisa al servidor cuando el jugador empieza/deja de retroceder (S) y suelta las poses de bloqueo vencidas. */
@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BackwardGuardInputHandler {
    private static boolean lastSent;

    private BackwardGuardInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastSent = false;
            return;
        }

        boolean backing = player.input != null
                && player.input.down
                && !player.input.up
                && GrowingFistInputHandler.isGrowingFistAttack(player);
        if (backing != lastSent) {
            lastSent = backing;
            PacketDistributor.sendToServer(new BackwardInputPacket(backing));
        }

        for (AbstractClientPlayer other : minecraft.level.players()) {
            SuitRenderLayer.tickGrowingBlock(other);
        }
    }
}
