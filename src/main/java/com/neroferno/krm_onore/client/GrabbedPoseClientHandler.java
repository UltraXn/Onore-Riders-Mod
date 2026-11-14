package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.combat.GrabbedPlayers;
import com.neroferno.krm_onore.network.GrabbedPoseSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = KRMRevoMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GrabbedPoseClientHandler {
    private GrabbedPoseClientHandler() {}

    public static void handle(GrabbedPoseSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(packet.entityId()) instanceof Player player) {
            if (packet.grabbed()) {
                GrabbedPlayers.add(player.getUUID());
                player.setPose(Pose.SLEEPING);
            } else {
                GrabbedPlayers.remove(player.getUUID());
                player.setPose(Pose.STANDING);
            }
            player.refreshDimensions();
        }
    }

    /** Al salir del mundo no debe quedar nadie marcado como agarrado. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GrabbedPlayers.clear();
    }
}
