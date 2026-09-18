package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.event.ModSounds;
import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.rider.RiderDefinition;
import com.neroferno.krm_revo.rider.RiderRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Server-side helper for toggling the player's transformation state.
 *
 * Transformed/last-transform-time are stored in the player's persistent data
 * (NBT), same as before. The "active Rider", however, is no longer stored —
 * it's resolved live from whatever Driver item sits in the player's
 * RIDER_INVENTORY slot 0. See {@link #getActiveRider(Player)}.
 */
@SuppressWarnings({"null", "deprecation"})
public class TransformationHelper {

    private static final String TAG_TRANSFORMED  = "krm_revo:transformed";
    private static final String TAG_LAST_TRANSFORM_TIME = "krm_revo:last_transform_time";

    /**
     * Resolves the Rider currently equipped for this player, live, from
     * whichever Driver item sits in RIDER_INVENTORY slot 0.
     *
     * FIX: this replaces the old "krm_revo:active_rider" NBT tag, which
     * nothing ever wrote to, so it always fell back to the hardcoded
     * "kuuga" default regardless of which belt was actually equipped.
     * Resolving live means a second (or third) Rider's belt works the
     * moment it's registered in RiderRegistry, with no extra state to
     * keep in sync (and no way for it to silently go stale).
     *
     * Requires a {@code RiderRegistry.getByDriverItem(Item)} lookup —
     * see the note at the end of this response if that method doesn't
     * exist yet.
     *
     * @return the matching RiderDefinition, or null if the slot is empty
     *         or holds an item RiderRegistry doesn't recognize as a Driver.
     */
    public static RiderDefinition getActiveRider(Player player) {
        if (!player.hasData(ModAttachments.RIDER_INVENTORY)) {
            return null;
        }
        ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
        ItemStack beltStack = inv.getStackInSlot(0);
        if (beltStack.isEmpty()) {
            return null;
        }
        return RiderRegistry.getByDriverItem(beltStack.getItem());
    }

    /**
     * Returns the id of the Rider currently equipped (e.g. "kuuga"), or
     * null if nothing recognized is equipped. Unlike before, this is a
     * live lookup — there is no separate "set" method anymore.
     */
    public static String getActiveRiderId(Player player) {
        RiderDefinition rider = getActiveRider(player);
        return rider == null ? null : rider.getId();
    }

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
     * Returns true if the player has a recognized Driver Belt equipped in
     * the Custom Rider Inventory — any registered Rider, not just Kuuga.
     */
    public static boolean isBeltEquipped(Player player) {
        return getActiveRider(player) != null;
    }
}
