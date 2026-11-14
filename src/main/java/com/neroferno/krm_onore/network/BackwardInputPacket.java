package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.combat.BackwardBlockCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> servidor: el jugador empezo (true) o dejo (false) de retroceder con la tecla S. Solo se envia al cambiar. */
public record BackwardInputPacket(boolean backing) implements CustomPacketPayload {
    public static final Type<BackwardInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "backward_input"));

    public static final StreamCodec<FriendlyByteBuf, BackwardInputPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.backing()),
            buf -> new BackwardInputPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackwardInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BackwardBlockCombat.setBacking(player, packet.backing());
            }
        });
    }
}
