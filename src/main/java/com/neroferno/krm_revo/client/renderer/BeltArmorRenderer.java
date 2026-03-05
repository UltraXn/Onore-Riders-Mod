package com.neroferno.krm_revo.client.renderer;

import com.neroferno.krm_revo.item.BeltItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BeltArmorRenderer extends GeoArmorRenderer<BeltItem> {
    public BeltArmorRenderer() {
        super(new GeoModel<BeltItem>() {
            @Override
            public ResourceLocation getModelResource(BeltItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/driver_belt.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(BeltItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/driver_belt.png");
            }

            @Override
            public ResourceLocation getAnimationResource(BeltItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json");
            }
        });
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }
}
