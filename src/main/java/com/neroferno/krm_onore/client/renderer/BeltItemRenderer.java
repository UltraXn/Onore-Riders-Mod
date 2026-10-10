package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.BeltItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib item renderer for the Arcle Driver (inventory icon, hand, ground).
 *
 * Takes model, texture, and animation resources as constructor parameters.
 *
 * Reminder: the ARMOR geo
 * (geo/item/*.geo.json) is offset for a player's waist and is NOT centered
 * on the origin, so it will render invisible/mis-scaled if reused directly
 * as the handheld/item geo. If arcle_driver.geo.json has the same problem,
 * export a second, origin-centered variant (e.g.
 * arcle_driver_handheld.geo.json) the same way a centered handheld model
 * was done, and pass that here instead.
 */
public class BeltItemRenderer extends GeoItemRenderer<BeltItem> {
    private final ResourceLocation overlayTexture;

    public BeltItemRenderer() {
        this(
            ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/arcle_driver_handheld.geo.json"),
            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcletexture.png"),
            ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json"),
            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcle_core_off.png")
        );
    }

    public BeltItemRenderer(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
        this(geoModel, texture, animation, null);
    }

    public BeltItemRenderer(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation,
                            ResourceLocation overlayTexture) {
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
        this.overlayTexture = overlayTexture;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, BeltItem animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);

        if (this.overlayTexture != null) {
            RenderType overlayRenderType = getRenderType(animatable, this.overlayTexture, bufferSource, partialTick);
            super.actuallyRender(poseStack, animatable, model, overlayRenderType, bufferSource, null, isReRender,
                    partialTick, packedLight, packedOverlay, colour);
        }
    }
}
