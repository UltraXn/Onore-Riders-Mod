package com.neroferno.krm_onore.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.neroferno.krm_onore.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_onore.item.ModItems;
import com.neroferno.krm_onore.item.SuitVisualItem;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings({"null", "removal"})
public class SuitRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    
    private final SuitVisualRenderer renderer;
    private ItemStack dummySuitStack;

    public SuitRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderLayerParent) {
        super(renderLayerParent);
        this.renderer = new SuitVisualRenderer();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!TransformationHelper.isTransformed(player)) return;

        if (this.dummySuitStack == null) {
            this.dummySuitStack = new ItemStack(ModItems.KUUGA_SUIT_VISUAL.get());
        }

        // Prepare the GeckoLib renderer to sync rotations with the vanilla player model skeleton
        PlayerModel<AbstractClientPlayer> parentModel = this.getParentModel();
        parentModel.setAllVisible(true);
        this.renderer.prepForRender(player, dummySuitStack, EquipmentSlot.CHEST, parentModel);
        
        int form = player.getPersistentData().getInt("krm_revo:form");
        if (form == 1) {
            long transformStartTick = player.getPersistentData().getLong("krm_revo:transform_start_tick");
            if (!player.getPersistentData().getBoolean("krm_revo:has_transform_start_tick")) {
                transformStartTick = player.tickCount;
                player.getPersistentData().putLong("krm_revo:transform_start_tick", transformStartTick);
                player.getPersistentData().putBoolean("krm_revo:has_transform_start_tick", true);
            }
            float elapsedTicks = (float) (player.tickCount + partialTick - transformStartTick);
            if (elapsedTicks < 12.0F) {
                this.renderer.setGrowingRevealProgress(elapsedTicks);
                // Keep the limbs visible until each armor bone has scaled into place to prevent flickering gaps
                parentModel.setAllVisible(true);
                if (elapsedTicks >= 0.0F) parentModel.body.visible = false;
                if (elapsedTicks >= 3.0F) {
                    parentModel.leftArm.visible = false;
                    parentModel.rightArm.visible = false;
                    parentModel.leftSleeve.visible = false;
                    parentModel.rightSleeve.visible = false;
                }
                if (elapsedTicks >= 6.0F) {
                    parentModel.leftLeg.visible = false;
                    parentModel.rightLeg.visible = false;
                    parentModel.leftPants.visible = false;
                    parentModel.rightPants.visible = false;
                }
                if (elapsedTicks >= 9.0F) {
                    parentModel.head.visible = false;
                    parentModel.hat.visible = false;
                }
            } else {
                this.renderer.clearGrowingRevealProgress();
                parentModel.setAllVisible(false);
            }
        } else {
            this.renderer.clearGrowingRevealProgress();
            parentModel.setAllVisible(false);
        }

        ResourceLocation textureRes = form == 1 
            ? ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/growing_suit.png") 
            : ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");

        RenderType renderType = this.renderer.getRenderType((SuitVisualItem) dummySuitStack.getItem(), 
                                                            textureRes, 
                                                            bufferSource, partialTick);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Render the model immediately
        this.renderer.renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
