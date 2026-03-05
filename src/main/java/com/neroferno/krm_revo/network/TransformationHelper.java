package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.event.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side helper for toggling the player's transformation state.
 *
 * State is stored in the player's persistent data (NBT tag) under the key
 * "krm_revo:transformed". This is accessible from both client and server.
 */
@SuppressWarnings({"null", "deprecation"})
public class TransformationHelper {

    private static final String TAG_TRANSFORMED  = "krm_revo:transformed";
    private static final String TAG_LAST_TRANSFORM_TIME = "krm_revo:last_transform_time";

    /**
     * Returns true if the player is currently transformed.
     */
    public static boolean isTransformed(Player player) {
        return player.getPersistentData().getBoolean(TAG_TRANSFORMED);
    }

    /**
     * Retrieves the game time (ticks) when the player last transformed/detransformed.
     */
    public static long getLastTransformTime(Player player) {
        return player.getPersistentData().getLong(TAG_LAST_TRANSFORM_TIME);
    }

    /**
     * Sets the game time (ticks) of the player's last transformation action.
     */
    public static void setLastTransformTime(Player player, long time) {
        player.getPersistentData().putLong(TAG_LAST_TRANSFORM_TIME, time);
    }

    /**
     * Toggles the transformation state and notifies the player.
     */
    public static void toggleTransformation(ServerPlayer player) {
        boolean current = isTransformed(player);
        setTransformed(player, !current);

        if (!current) {
            // Became transformed
            KRMRevoMod.LOGGER.debug("{} transformed!", player.getName().getString());
            player.displayClientMessage(
                    Component.translatable("krm_revo.transform.on"), true); // true = action bar
            
            // Play transformation sounds
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            // Detransformed
            KRMRevoMod.LOGGER.debug("{} detransformed.", player.getName().getString());
            player.displayClientMessage(
                    Component.translatable("krm_revo.transform.off"), true);
        }
    }

    /**
     * Directly sets the transformation state.
     */
    public static void setTransformed(Player player, boolean transformed) {
        player.getPersistentData().putBoolean(TAG_TRANSFORMED, transformed);
    }

    /**
     * Returns true if the player has the Driver Belt equipped in the new Custom Rider Inventory.
     */
    public static boolean isBeltEquipped(Player player) {
        if (!player.hasData(com.neroferno.krm_revo.attachment.ModAttachments.RIDER_INVENTORY)) return false;
        
        net.neoforged.neoforge.items.ItemStackHandler inv = player.getData(com.neroferno.krm_revo.attachment.ModAttachments.RIDER_INVENTORY);
        return inv.getStackInSlot(0).is(com.neroferno.krm_revo.item.ModItems.DRIVER_BELT.get());
    }
}

