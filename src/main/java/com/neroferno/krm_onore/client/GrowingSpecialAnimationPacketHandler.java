package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.network.GrowingSpecialAnimationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class GrowingSpecialAnimationPacketHandler {
    private GrowingSpecialAnimationPacketHandler() {}

    public static void handle(GrowingSpecialAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !(minecraft.level.getEntity(packet.playerId()) instanceof AbstractClientPlayer player)) {
            return;
        }

        int blockTicks = switch (packet.animation()) {
            case "especial3" -> com.neroferno.krm_onore.combat.Especial3Combat.durationTicks();
            case "especial2_1" -> com.neroferno.krm_onore.combat.Especial2Combat.stanceTicks();
            case "especial2_2" -> com.neroferno.krm_onore.combat.Especial2Combat.strikeTicks();
            case "especial2_cancel", "especial1_cancel" -> 0;
            case "especial1_1", "especial1_2", "especial1_loop", "especial1_end" ->
                    com.neroferno.krm_onore.combat.Especial1Combat.clientLockTicks(packet.animation());
            default -> -1;
        };
        if (blockTicks < 0) {
            return;
        }

        if (minecraft.player != null && player.getId() == minecraft.player.getId()) {
            if (blockTicks == 0) {
                GrowingFistInputHandler.releaseSpecial(minecraft.player);
                GrowingFistInputHandler.releaseRoot();
            } else {
                GrowingFistInputHandler.blockForSpecial(minecraft.player, blockTicks);
                if (packet.animation().startsWith("especial1")) {
                    GrowingFistInputHandler.rootFor(minecraft.player, blockTicks);
                }
            }
        }
        SuitRenderLayer.playGrowingSpecial(player, packet.animation());
    }
}
