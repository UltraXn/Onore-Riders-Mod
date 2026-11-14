package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.network.GrowingBlockAnimationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class GrowingBlockAnimationPacketHandler {
    private GrowingBlockAnimationPacketHandler() {}

    public static void handle(GrowingBlockAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getEntity(packet.playerId()) instanceof AbstractClientPlayer player) {
            if (packet.holdTicks() > 0) {
                SuitRenderLayer.markBlocking(player, packet.holdTicks());
                SuitRenderLayer.playGrowingBlock(player, packet.holdTicks());
            } else {
                SuitRenderLayer.clearBlocking(player);
                SuitRenderLayer.releaseGrowingBlock(player);
            }
        }
    }
}
