package com.neroferno.krm_onore.menu;

import com.neroferno.krm_onore.KRMRevoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KRMRevoMod.MODID);

    /** 
     * RiderMenu extends ModularUIContainerMenu, so the factory signature must 
     * match: (MenuType, int windowId, Inventory, FriendlyByteBuf).
     */
    public static final DeferredHolder<MenuType<?>, MenuType<RiderMenu>> RIDER_MENU =
            MENUS.register("rider_menu", () -> IMenuTypeExtension.create(
                    (windowId, playerInventory, buf) -> new RiderMenu(windowId, playerInventory, buf)
            ));
}
