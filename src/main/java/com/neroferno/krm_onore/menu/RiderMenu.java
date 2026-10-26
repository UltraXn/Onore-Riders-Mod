package com.neroferno.krm_onore.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Standard neo-forge container menu for the Rider GUI.
 */
public class RiderMenu extends AbstractContainerMenu {

    private final Player player;

    public RiderMenu(int pContainerId, Inventory inv) {
        this(pContainerId, inv, null);
    }

    public RiderMenu(int pContainerId, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.RIDER_MENU.get(), pContainerId);
        this.player = inv.player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public Player getPlayer() {
        return this.player;
    }
}
