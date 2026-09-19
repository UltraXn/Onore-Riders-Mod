package com.neroferno.krm_revo.item;

import com.neroferno.krm_revo.block.ModBlocks;
import com.neroferno.krm_revo.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_revo.client.renderer.BeltItemRenderer;
import net.minecraft.resources.ResourceLocation;
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

    // ─── Transformation Items ─────────────────────────────────────────────

    /**
     * The core transformation item. When equipped in the Curios 'belt' slot,
     * it grants access to the transformation UI (K key) and combat bonuses.
     *
     * Each Rider's belt now supplies its own geo/texture/animation files
     * (instead of BeltItem hardcoding Kuuga's), so a second Rider's Driver
     * can register its own look here without touching BeltItem/BeltArmorRenderer.
     */
    public static final DeferredItem<BeltItem> DRIVER_BELT =
            ITEMS.register("driver_belt", () -> new BeltItem(
                    "animation.driver_belt.idle",
                    () -> new BeltItemRenderer(
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/driver_belt_handheld.geo.json"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/driver_belt.png"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json")
                    ),
                    () -> new BeltArmorRenderer(
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/driver_belt.geo.json"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/driver_belt.png"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json")
                    )
            ));

    /**
     * Agito's Arcle Driver. Uses its own arcle_driver.geo, arcletexture and
     * arcle_driver.json instead of Kuuga's driver_belt files.
     *
     * Shape is currently a placeholder: same mesh as Kuuga's belt, just
     * retextured with arcletexture.png (no custom geometry yet). Split into
     * arcle_driver.geo.json (armor, armorBody pivot 24) and
     * arcle_driver_handheld.geo.json (item/inventory, armorBody pivot 10.2)
     * the same way driver_belt / driver_belt_handheld are split, so the
     * "invisible in hand / speck in inventory" bug from reusing the armor
     * geo as the handheld geo doesn't come back.
     */
    public static final DeferredItem<BeltItem> ARCLE_DRIVER =
            ITEMS.register("arcle_driver", () -> new BeltItem(
                    "animation.arcle_driver.idle",
                    () -> new BeltItemRenderer(
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/arcle_driver_handheld.geo.json"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcletexture.png"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/arcle_driver.animation.json")
                    ),
                    () -> new BeltArmorRenderer(
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/arcle_driver.geo.json"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcletexture.png"),
                            ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/arcle_driver.animation.json")
                    )
            ));

    public static final DeferredItem<SuitVisualItem> KUUGA_SUIT_VISUAL =
            ITEMS.register("kuuga_suit_visual", SuitVisualItem::new);

    // ─── Armor Items ───────────────────────────────────────────────────────

    public static final DeferredItem<BlockItem> RESEARCHER_DESK_ITEM =
            ITEMS.register("researcher_desk", () -> new BlockItem(ModBlocks.RESEARCHER_DESK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MESA_DE_TRABAJO_ITEM =
            ITEMS.register("mesa_de_trabajo", () -> new BlockItem(ModBlocks.MESA_DE_TRABAJO.get(), new Item.Properties()));
}
