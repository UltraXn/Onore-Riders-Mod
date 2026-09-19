package com.neroferno.krm_revo.client.renderer;

import com.neroferno.krm_revo.item.BeltItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor-layer renderer that draws a Driver Belt on the player's body.
 *
 * Generalized (previously hardcoded to Kuuga's driver_belt files): now takes
 * the geo/texture/animation ResourceLocations as constructor params, so
 * BeltRenderLayer can render whichever belt is actually equipped (Kuuga's
 * driver_belt, Agito's arcle_driver, etc.) instead of always drawing Kuuga's.
 */
public class BeltArmorRenderer extends GeoArmorRenderer<BeltItem> {

    public BeltArmorRenderer(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
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

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }
}
