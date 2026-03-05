package com.neroferno.krm_revo.client.renderer;

import com.neroferno.krm_revo.item.SuitVisualItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SuitVisualRenderer extends GeoArmorRenderer<SuitVisualItem> {
    public SuitVisualRenderer() {
        super(new GeoModel<SuitVisualItem>() {
            @Override
            public ResourceLocation getModelResource(SuitVisualItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/armor/kuuga_suit.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SuitVisualItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SuitVisualItem animatable) {
                return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/armor/kuuga_suit.animation.json"); // Provide safe fallback
            }
        });
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }
}
