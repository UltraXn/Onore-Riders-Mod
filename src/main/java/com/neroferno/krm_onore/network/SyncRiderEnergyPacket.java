package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderEnergyData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings({"null", "resource"})
public record SyncRiderEnergyPacket(int entityId, float energy, float maxEnergy) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRiderEnergyPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_rider_energy"));

    public static final StreamCodec<ByteBuf, SyncRiderEnergyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRiderEnergyPacket::entityId,
            ByteBufCodecs.FLOAT, SyncRiderEnergyPacket::energy,
            ByteBufCodecs.FLOAT, SyncRiderEnergyPacket::maxEnergy,
            SyncRiderEnergyPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRiderEnergyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof Player player) {
                RiderEnergyData data = player.getData(ModAttachments.RIDER_ENERGY);
                data.setMaxEnergy(packet.maxEnergy());
                data.setEnergy(packet.energy());
            }
        });
    }
}
