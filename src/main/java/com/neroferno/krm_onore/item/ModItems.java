package com.neroferno.krm_onore.item;

import com.neroferno.krm_onore.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all Onore Rider items.
 * Register ModItems.ITEMS to the mod event bus in KRMRevoMod.
 */
@SuppressWarnings("null")
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("krm_revo");

    // â”€â”€â”€ Transformation Items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Server-side handler for the K-key transformation request.
     * Uses the custom NBT belt-equipped flag.
     * it grants access to the transformation UI (K key) and combat bonuses.
     */
    public static final DeferredItem<BeltItem> DRIVER_BELT =
            ITEMS.register("driver_belt", BeltItem::new);

    public static final DeferredItem<SuitVisualItem> KUUGA_SUIT_VISUAL =
            ITEMS.register("kuuga_suit_visual", SuitVisualItem::new);

    // â”€â”€â”€ Armor Items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€



    public static final DeferredItem<BlockItem> RESEARCHER_DESK_ITEM =
            ITEMS.register("researcher_desk", () -> new BlockItem(ModBlocks.RESEARCHER_DESK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MESA_DE_TRABAJO_ITEM =
            ITEMS.register("mesa_de_trabajo", () -> new BlockItem(ModBlocks.MESA_DE_TRABAJO.get(), new Item.Properties()));
}

