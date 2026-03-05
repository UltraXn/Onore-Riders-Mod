package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.client.animation.AnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client packet to trigger an animation on a specific player.
 */
@SuppressWarnings("null")
public record PlayerAnimationPacket(int playerId, String animationName) implements CustomPacketPayload {

    public static final Type<PlayerAnimationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "player_animation"));

    public static final StreamCodec<FriendlyByteBuf, PlayerAnimationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.playerId);
                buf.writeUtf(packet.animationName);
            },
            buf -> new PlayerAnimationPacket(buf.readInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final PlayerAnimationPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            AnimationHandler.playAnimation(data.playerId(), data.animationName());
        });
    }
}

