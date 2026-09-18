package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.SuitVisualItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SuitVisualRenderer extends GeoArmorRenderer<SuitVisualItem> {
    public SuitVisualRenderer() {
        super(new SuitModel());
        ((SuitModel) this.getGeoModel()).setRenderer(this);
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }

    private static class SuitModel extends GeoModel<SuitVisualItem> {
        private SuitVisualRenderer renderer;

        public void setRenderer(SuitVisualRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public ResourceLocation getModelResource(SuitVisualItem animatable) {
            if (this.renderer != null) {
                net.minecraft.world.entity.Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    int form = player.getPersistentData().getInt("krm_revo:form");
                    if (form == 1) {
                        return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/armor/growing_suit.geo.json");
                    }
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/armor/kuuga_suit.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SuitVisualItem animatable) {
            if (this.renderer != null) {
                net.minecraft.world.entity.Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    int form = player.getPersistentData().getInt("krm_revo:form");
                    if (form == 1) {
                        return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/growing_suit.png");
                    }
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");
        }

        @Override
        public ResourceLocation getAnimationResource(SuitVisualItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/armor/kuuga_suit.animation.json");
        }
    }
}
