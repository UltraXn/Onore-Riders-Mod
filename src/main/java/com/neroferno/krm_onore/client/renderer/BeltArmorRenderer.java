package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.BeltItem;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BeltArmorRenderer extends GeoArmorRenderer<BeltItem> {
    public BeltArmorRenderer() {
        super(new BeltModel());
        ((BeltModel) this.getGeoModel()).setRenderer(this);
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }

    public static ResourceLocation getBeltTextureForPlayer(Player player) {
        if (!TransformationHelper.isTransformed(player)) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_off.png");
        }
        int form = player.getPersistentData().getInt("krm_revo:form");
        return switch (form) {
            case 1 -> ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_growing.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_dragon.png");
            case 3 -> ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_pegasus.png");
            case 4 -> ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_titan.png");
            default -> ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/driver_belt.png");
        };
    }

    private static class BeltModel extends GeoModel<BeltItem> {
        private BeltArmorRenderer renderer;

        public void setRenderer(BeltArmorRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public ResourceLocation getModelResource(BeltItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/driver_belt.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(BeltItem animatable) {
            if (this.renderer != null) {
                Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    return getBeltTextureForPlayer(player);
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/belt_off.png");
        }

        @Override
        public ResourceLocation getAnimationResource(BeltItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json");
        }
    }
}
