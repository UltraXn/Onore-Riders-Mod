package com.neroferno.krm_revo.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.item.ModItems;
import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.neroferno.krm_revo.client.renderer.BeltArmorRenderer;

@SuppressWarnings({"null", "removal", "deprecation"})
public class BeltRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final BeltArmorRenderer beltRenderer;

    public BeltRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
        this.beltRenderer = new BeltArmorRenderer();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // 1. Only render if the belt is equipped in the custom inventory slots
        ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
        ItemStack beltStack = inv.getStackInSlot(0);

        if (beltStack.isEmpty() || !beltStack.is(ModItems.DRIVER_BELT.get())) return;

        // 2. We don't render the belt layer if the full suit is already active
        // (because the full suit model already has the belt built in, or we might want to render just the belt if untransformed)
        // Wait, does the Suit model have the belt?
        // Let's render it unless we're transformed, or render it always if needed.
        // For now, let's render it always. It's safe to assume the belt is worn.
        if (TransformationHelper.isTransformed(player)) {
            // Optional: return if the suit model contains the belt already.
            // return;
        }

        poseStack.pushPose();

        // Sync player body rotations to the GeckoLib renderer
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();

        // Let GeckoLib handle the rest of the rendering natively
        this.beltRenderer.prepForRender(player, beltStack, EquipmentSlot.LEGS, model);
        net.minecraft.client.renderer.RenderType renderType = this.beltRenderer.getRenderType(
                (com.neroferno.krm_revo.item.BeltItem) beltStack.getItem(),
                this.beltRenderer.getTextureLocation((com.neroferno.krm_revo.item.BeltItem) beltStack.getItem()),
                buffer, partialTick);
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        this.beltRenderer.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
