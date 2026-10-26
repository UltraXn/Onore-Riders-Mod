package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings({"null", "resource"})
public record SyncRiderStatePacket(int entityId, boolean isTransformed, boolean hasBelt, int riderKickState, int form) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRiderStatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_rider_state"));

    public static final StreamCodec<ByteBuf, SyncRiderStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRiderStatePacket::entityId,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::isTransformed,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::hasBelt,
            ByteBufCodecs.INT, SyncRiderStatePacket::riderKickState,
            ByteBufCodecs.INT, SyncRiderStatePacket::form,
            SyncRiderStatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRiderStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null || context.player().level() == null) return;

            Entity entity = context.player().level().getEntity(packet.entityId());
            if (entity instanceof Player player) {
                // Update RiderStateData attachment
                com.neroferno.krm_onore.attachment.RiderStateData stateData = player.getData(ModAttachments.RIDER_STATE);
                boolean wasTransformed = stateData.isTransformed();
                if (packet.isTransformed() && !wasTransformed) {
                    if (stateData.hasTransformAnimationStartTick()) {
                        stateData.setTransformStartTick(
                                stateData.getTransformAnimationStartTick()
                                        + stateData.getTransformAnimationTicks());
                        stateData.setHasTransformStartTick(true);
                    } else if (stateData.hasReceivedInitialSync()) {
                        // Se transformó en vivo sin animación (p. ej. comando): reproducir el reveal.
                        stateData.setTransformStartTick(player.tickCount);
                        stateData.setHasTransformStartTick(true);
                    } else {
                        // Primer paquete de este jugador y ya llega transformado: estaba transformado antes de
                        // entrar al mundo. Antes se usaba "tickCount > 5", pero el primer sync llega ~10 ticks
                        // después de entrar, así que el reveal del traje se reproducía en cada login.
                        stateData.setTransformStartTick(player.tickCount - 100);
                        stateData.setHasTransformStartTick(true);
                    }
                }
                stateData.setReceivedInitialSync(true);
                if (!packet.isTransformed()) {
                    // El servidor reenvía el estado cada 10 ticks (CombatEventHandler) con isTransformed=false
                    // mientras la animación aún corre. Si borramos el start tick aquí, el fade del core pierde
                    // su reloj y cae al respaldo (12 ticks) justo cuando termina la animación.
                    // Solo se borra al destransformarse, al quitar el cinturón o si la animación ya expiró.
                    boolean animationRunning = !wasTransformed
                            && packet.hasBelt()
                            && stateData.hasTransformAnimationStartTick()
                            && player.tickCount - stateData.getTransformAnimationStartTick()
                                    <= stateData.getTransformAnimationTicks() + 40;
                    if (!animationRunning) {
                        stateData.clearTransformAnimationStartTick();
                    }
                }
                stateData.setTransformed(packet.isTransformed());
                stateData.setRiderKickState(packet.riderKickState());
                stateData.setForm(packet.form());

                // Update specific Rider Inventory for belt
                ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
                if (packet.hasBelt()) {
                    if (inv.getStackInSlot(0).isEmpty()) {
                        inv.setStackInSlot(0, new ItemStack(com.neroferno.krm_onore.item.ModItems.ARCLE_DRIVER.get()));
                    }
                } else {
                    // Clear the belt slot
                    if (!inv.getStackInSlot(0).isEmpty()) {
                        inv.setStackInSlot(0, ItemStack.EMPTY);
                    }
                }
            }
        });
    }
}
