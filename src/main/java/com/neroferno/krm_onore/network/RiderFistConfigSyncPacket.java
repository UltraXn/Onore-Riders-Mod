package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.combat.RiderFistTiming;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RiderFistConfigSyncPacket(String json) implements CustomPacketPayload {
    public static final Type<RiderFistConfigSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "rider_fist_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, RiderFistConfigSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.json(), 32767),
            buf -> new RiderFistConfigSyncPacket(buf.readUtf(32767))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RiderFistConfigSyncPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> RiderFistTiming.applySyncedData(packet.json()));
    }
}
