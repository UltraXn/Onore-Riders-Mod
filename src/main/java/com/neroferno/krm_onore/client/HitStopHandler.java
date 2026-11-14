package com.neroferno.krm_onore.client;

import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.client.vfx.ScreenShakeHandler;
import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.network.HitStopPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cliente: temblor de camara y hit-stop (congelar las animaciones del traje y del cinturon unos ticks).
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID, value = Dist.CLIENT)
public final class HitStopHandler {
    /** id de entidad del atacante -> ticks restantes de pausa. */
    private static final Map<Integer, Integer> FROZEN = new HashMap<>();

    private HitStopHandler() {}

    public static void handle(HitStopPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (minecraft.player != null && minecraft.player.getId() == packet.attackerId() && packet.shake() > 0.0F) {
            ScreenShakeHandler.shake(packet.shake());
        }

        if (packet.hitStopTicks() > 0
                && minecraft.level.getEntity(packet.attackerId()) instanceof AbstractClientPlayer attacker) {
            SuitRenderLayer.setAnimationFrozen(attacker, true);
            FROZEN.put(packet.attackerId(), packet.hitStopTicks());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (FROZEN.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, Integer>> iterator = FROZEN.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft > 0) {
                entry.setValue(ticksLeft);
                continue;
            }

            iterator.remove();
            if (minecraft.level != null
                    && minecraft.level.getEntity(entry.getKey()) instanceof AbstractClientPlayer attacker) {
                SuitRenderLayer.setAnimationFrozen(attacker, false);
            }
        }
    }
}
