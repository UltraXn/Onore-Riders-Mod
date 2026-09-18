package com.neroferno.krm_revo.menu;

import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.rider.RiderRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerArmorInvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerOffhandInvWrapper;

/**
 * Standard neo-forge container menu for the Rider GUI.
 */
public class RiderMenu extends AbstractContainerMenu {

    private final Player player;
    private final IItemHandler riderInv;

    public RiderMenu(int pContainerId, Inventory inv) {
        this(pContainerId, inv, null);
    }

    public RiderMenu(int pContainerId, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.RIDER_MENU.get(), pContainerId);
        this.player = inv.player;
        this.riderInv = player.getData(ModAttachments.RIDER_INVENTORY);
        
        PlayerArmorInvWrapper armorInv = new PlayerArmorInvWrapper(inv);
        PlayerOffhandInvWrapper offhandInv = new PlayerOffhandInvWrapper(inv);

        // Armor slots (helmet, chest, legs, boots)
        // Matching RiderScreen coords for slots: 10, 20 / 38 / 56 / 74
        this.addSlot(new SlotItemHandler(armorInv, 3, 10, 20)); // Helmet
        this.addSlot(new SlotItemHandler(armorInv, 2, 10, 38)); // Chestplate
        this.addSlot(new SlotItemHandler(armorInv, 1, 10, 56)); // Leggings
        this.addSlot(new SlotItemHandler(armorInv, 0, 10, 74)); // Boots

        // Offhand
        this.addSlot(new SlotItemHandler(offhandInv, 0, 10, 92)); 

        // Driver Belt — accepts any item registered as a Rider's Driver in
        // RiderRegistry, not just the original Kuuga belt (fix: this used to
        // hardcode ModItems.DRIVER_BELT, so a second Rider's belt couldn't
        // even be placed in the slot).
        this.addSlot(new SlotItemHandler(riderInv, 0, 10, 115) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RiderRegistry.getByDriverItem(stack.getItem()) != null;
            }
        });

        // Not adding standard player inventory because the GUI layout bounds (320x180) leaves no room
        // and it looks cleaner without it according to reference images.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Player getPlayer() {
        return this.player;
    }

    public IItemHandler getRiderInv() {
        return this.riderInv;
    }
}
