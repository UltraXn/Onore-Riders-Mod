package com.neroferno.krm_revo.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_revo.item.BeltItem;
import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.IdentityHashMap;
import java.util.Map;

@SuppressWarnings({"null", "removal", "deprecation"})
public class BeltRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    // Cached per belt Item (not per-frame) so every render call doesn't
    // rebuild a fresh GeckoLib renderer for whichever Driver is equipped
    // (Kuuga's driver_belt, Agito's arcle_driver, etc.).
    private final Map<Item, BeltArmorRenderer> rendererCache = new IdentityHashMap<>();

    public BeltRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // 1. Only render if a belt is equipped in the custom inventory slots
        ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
        ItemStack beltStack = inv.getStackInSlot(0);

        // Generalized: any registered Driver belt (not just Kuuga's
        // driver_belt) should render here, so this now checks the item
        // type rather than a single hardcoded item instance.
        if (beltStack.isEmpty() || !(beltStack.getItem() instanceof BeltItem beltItem)) return;

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

        // Ask the equipped belt for its own armor renderer instead of
        // always drawing Kuuga's driver_belt model.
        BeltArmorRenderer beltRenderer = rendererCache.computeIfAbsent(beltItem, item -> beltItem.getArmorRenderer());

        // Let GeckoLib handle the rest of the rendering natively
        beltRenderer.prepForRender(player, beltStack, EquipmentSlot.LEGS, model);
        net.minecraft.client.renderer.RenderType renderType = beltRenderer.getRenderType(
                beltItem,
                beltRenderer.getTextureLocation(beltItem),
                buffer, partialTick);
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        beltRenderer.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
