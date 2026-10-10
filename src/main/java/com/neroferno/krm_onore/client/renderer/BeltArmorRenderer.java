package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.BeltItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BeltArmorRenderer extends GeoArmorRenderer<BeltItem> {
    private ResourceLocation renderTextureOverride;
    private boolean translucentPass;
    private float renderAlpha = 1.0F;

    public BeltArmorRenderer() {
        super(new BeltModel());
        ((BeltModel) this.getGeoModel()).setRenderer(this);
    }

    public void renderWithTexture(Entity entity, ItemStack stack, HumanoidModel<?> playerModel,
                                  EquipmentSlot slot, PoseStack poseStack, MultiBufferSource buffer,
                                  float partialTick, int packedLight, ResourceLocation texture,
                                  float alpha, boolean translucent) {
        this.renderTextureOverride = texture;
        this.renderAlpha = alpha;
        this.translucentPass = translucent;
        try {
            prepForRender(entity, stack, slot, playerModel);
            RenderType renderType = getRenderType((BeltItem) stack.getItem(), texture, buffer, partialTick);
            VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
            renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        } finally {
            this.renderTextureOverride = null;
            this.renderAlpha = 1.0F;
            this.translucentPass = false;
        }
    }

    @Override
    public RenderType getRenderType(BeltItem animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return this.translucentPass ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(BeltItem animatable, float partialTick, int packedLight) {
        return this.translucentPass ? Color.ofRGBA(1.0F, 1.0F, 1.0F, this.renderAlpha) : Color.WHITE;
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        this.setAllVisible(true);
    }

    public static ResourceLocation getBeltTextureForPlayer(Player player) {
        return getBeltTextureForPlayer(player, null);
    }

    public static ResourceLocation getBeltTextureForPlayer(Player player, BeltItem beltItem) {
        return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcletexture.png");
    }

    private static class BeltModel extends GeoModel<BeltItem> {
        private BeltArmorRenderer renderer;

        public void setRenderer(BeltArmorRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public ResourceLocation getModelResource(BeltItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/item/arcle_driver.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(BeltItem animatable) {
            if (this.renderer != null && this.renderer.renderTextureOverride != null) {
                return this.renderer.renderTextureOverride;
            }
            if (this.renderer != null) {
                Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    return getBeltTextureForPlayer(player, animatable);
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcletexture.png");
        }

        @Override
        public ResourceLocation getAnimationResource(BeltItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/item/driver_belt.animation.json");
        }

        @Override
        public void setCustomAnimations(BeltItem animatable, long instanceId, AnimationState<BeltItem> animationState) {
            this.getBone("steve").ifPresent(bone -> {
                bone.setHidden(true);
                bone.setChildrenHidden(true);
            });
        }
    }
}
