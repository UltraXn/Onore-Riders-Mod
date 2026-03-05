package com.neroferno.krm_revo.client.renderer;

import com.neroferno.krm_revo.item.BeltItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib item renderer for the Driver Belt.
 *
 * Uses DefaultedItemGeoModel which automatically resolves:
 *   - Model:     assets/krm_revo/geo/item/driver_belt.geo.json
 *   - Texture:   assets/krm_revo/textures/item/driver_belt.png
 *   - Animation: assets/krm_revo/animations/item/driver_belt.animation.json
 *
 * The renderer is registered as the BEWLR (Block Entity Without Level Renderer)
 * for the BeltItem. NeoForge calls this instead of the standard flat item sprite.
 */
public class BeltItemRenderer extends GeoItemRenderer<BeltItem> {

    public BeltItemRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath("krm_revo", "driver_belt")
        ));
    }
}

