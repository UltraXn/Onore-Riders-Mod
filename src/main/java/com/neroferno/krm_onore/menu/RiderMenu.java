package com.neroferno.krm_onore.menu;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.item.BeltItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
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

        // 2x3 slot grid in the bottom-left compartment (x=15..79, y=120..191) of marco.png
        this.addSlot(new SlotItemHandler(armorInv, 3, 22, 125)); // Helmet
        this.addSlot(new SlotItemHandler(armorInv, 2, 22, 145)); // Chestplate
        this.addSlot(new SlotItemHandler(armorInv, 1, 22, 165)); // Leggings
        this.addSlot(new SlotItemHandler(armorInv, 0, 52, 125)); // Boots
        this.addSlot(new SlotItemHandler(offhandInv, 0, 52, 145)); // Offhand
        this.addSlot(new SlotItemHandler(riderInv, 0, 52, 165) {  // Driver Belt
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BeltItem;
            }
        });

        // Not adding standard player inventory because the GUI layout bounds (320x180) leaves no room
        // and it looks cleaner without it according to reference images.
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

    public IItemHandler getRiderInv() {
        return this.riderInv;
    }
}
