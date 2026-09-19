package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.BeltItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib item renderer for a Driver Belt (inventory icon, hand, ground).
 *
 * Generalized (previously hardcoded to Kuuga's driver_belt files): now takes
 * the geo/texture/animation ResourceLocations as constructor params, so each
 * Rider's BeltItem can supply its own handheld model instead of every belt
 * rendering with Kuuga's mesh.
 *
 * Reminder from the original driver_belt fix: the ARMOR geo
 * (geo/item/*.geo.json) is offset for a player's waist and is NOT centered
 * on the origin, so it will render invisible/mis-scaled if reused directly
 * as the handheld/item geo. If arcle_driver.geo.json has the same problem,
 * export a second, origin-centered variant (e.g.
 * arcle_driver_handheld.geo.json) the same way driver_belt_handheld.geo.json
 * was done, and pass that here instead.
 */
public class BeltItemRenderer extends GeoItemRenderer<BeltItem> {

    public BeltItemRenderer(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
        super(new GeoModel<BeltItem>() {
            @Override
            public ResourceLocation getModelResource(BeltItem animatable) {
                return geoModel;
            }

            @Override
            public ResourceLocation getTextureResource(BeltItem animatable) {
                return texture;
            }

            @Override
            public ResourceLocation getAnimationResource(BeltItem animatable) {
                return animation;
            }
        });
    }
}
