package com.neroferno.krm_onore.mixin;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderStateData;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"null", "unused"})
@Mixin(Inventory.class)
public abstract class InventoryPickupMixin {

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void krm_skipSelectedSlotForUnarmedForms(CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!inventory.player.hasData(ModAttachments.RIDER_STATE)) return;

        RiderStateData riderState = inventory.player.getData(ModAttachments.RIDER_STATE);
        int form = riderState.getForm();
        if (!riderState.isTransformed() || (form != 1 && form != 2)
                || !inventory.getItem(inventory.selected).isEmpty()) {
            return;
        }

        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot != inventory.selected && inventory.items.get(slot).isEmpty()) {
                cir.setReturnValue(slot);
                return;
            }
        }

        cir.setReturnValue(-1);
    }
}
