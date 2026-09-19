package com.neroferno.krm_onore.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_onore.item.BeltItem;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.IdentityHashMap;
import java.util.Map;

@SuppressWarnings({"null", "removal"})
public class BeltRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    // Cached per belt Item (not per-frame) so every render call doesn't
    // rebuild a fresh GeckoLib renderer for whichever Driver is equipped
    // (Kuuga's driver_belt, arcle_driver, etc.).
    private final Map<Item, BeltArmorRenderer> rendererCache = new IdentityHashMap<>();

    public BeltRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // 1. Only render if a belt is equipped in the custom inventory slots
        ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
        ItemStack beltStack = inv.getStackInSlot(0);

        if (beltStack.isEmpty() || !(beltStack.getItem() instanceof BeltItem beltItem)) return;

        poseStack.pushPose();

        // Sync player body rotations to the GeckoLib renderer
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        boolean transformed = TransformationHelper.isTransformed(player);
        if (transformed) {
            model.setAllVisible(true);
        }

        // Ask the equipped belt for its own armor renderer or fallback to default BeltArmorRenderer
        BeltArmorRenderer beltRenderer = rendererCache.computeIfAbsent(beltItem, item -> beltItem.getArmorRenderer());

        beltRenderer.prepForRender(player, beltStack, EquipmentSlot.LEGS, model);

        if (transformed) {
            model.setAllVisible(false);
        }

        ResourceLocation textureRes = BeltArmorRenderer.getBeltTextureForPlayer(player);
        net.minecraft.client.renderer.RenderType renderType = beltRenderer.getRenderType(
                beltItem,
                textureRes,
                buffer, partialTick);
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        beltRenderer.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
