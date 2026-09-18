package com.neroferno.krm_revo.item;

import com.neroferno.krm_revo.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all KRM: REvolution items.
 * Register ModItems.ITEMS to the mod event bus in KRMRevoMod.
 */
@SuppressWarnings("null")
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("krm_revo");

    // â”€â”€â”€ Transformation Items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * The core transformation item. When equipped in the Curios 'belt' slot,
     * it grants access to the transformation UI (K key) and combat bonuses.
     *
     * Kuuga's idle animation and item renderer are now passed in explicitly
     * (BeltItem no longer hardcodes them), so when you add a second Rider's
     * Driver below, it can register its own animation + renderer here
     * instead of inheriting Kuuga's.
     */
    public static final DeferredItem<BeltItem> DRIVER_BELT =
            ITEMS.register("driver_belt", () -> new BeltItem(
                    "animation.driver_belt.idle",
                    com.neroferno.krm_revo.client.renderer.BeltItemRenderer::new
            ));

    public static final DeferredItem<SuitVisualItem> KUUGA_SUIT_VISUAL =
            ITEMS.register("kuuga_suit_visual", SuitVisualItem::new);

    // â”€â”€â”€ Armor Items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€



    public static final DeferredItem<BlockItem> RESEARCHER_DESK_ITEM =
            ITEMS.register("researcher_desk", () -> new BlockItem(ModBlocks.RESEARCHER_DESK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MESA_DE_TRABAJO_ITEM =
            ITEMS.register("mesa_de_trabajo", () -> new BlockItem(ModBlocks.MESA_DE_TRABAJO.get(), new Item.Properties()));
}

