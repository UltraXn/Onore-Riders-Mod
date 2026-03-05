package com.neroferno.krm_revo.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_revo.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_revo.item.ModItems;
import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.neroferno.krm_revo.item.SuitVisualItem;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
        this.renderer.prepForRender(player, dummySuitStack, EquipmentSlot.CHEST, this.getParentModel());
        
        RenderType renderType = this.renderer.getRenderType((SuitVisualItem) dummySuitStack.getItem(), 
                                                            ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png"), 
                                                            bufferSource, partialTick);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Render the model immediately
        this.renderer.renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
