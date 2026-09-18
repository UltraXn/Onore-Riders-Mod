package com.neroferno.krm_onore.mixin.client;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.mixin.WeaponRegistryAccessor;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Client-only mixin for Better Combat's WeaponRegistry.
 * Intercepts getAttributes(ItemStack) to return our rider_fist preset
 * when the local player is fighting unarmed with the Driver Belt.
 * This is necessary because Better Combat's client mixin (MinecraftClientInject)
 * queries getAttributes directly before triggering the combo logic and animations.
 */
@Mixin(value = WeaponRegistry.class, remap = false)
public class ClientWeaponRegistryMixin {

    private static final ResourceLocation RIDER_FIST_ID =
            ResourceLocation.fromNamespaceAndPath("bettercombat", "rider_fist");

    @Inject(method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("HEAD"), cancellable = true)
    private static void krm_onGetAttributesClient(ItemStack itemStack, CallbackInfoReturnable<WeaponAttributes> cir) {
        if (!itemStack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Check belt
        boolean hasBelt = false;
        try {
            ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
            hasBelt = !inv.getStackInSlot(0).isEmpty();
        } catch (Exception e) {
            return;
        }

        if (hasBelt) {
            Map<ResourceLocation, AttributesContainer> containers = WeaponRegistryAccessor.getContainers();
            if (containers != null) {
                AttributesContainer container = containers.get(RIDER_FIST_ID);
                if (container != null && container.attributes() != null) {
                    cir.setReturnValue(container.attributes());
                }
            }
        }
    }
}
