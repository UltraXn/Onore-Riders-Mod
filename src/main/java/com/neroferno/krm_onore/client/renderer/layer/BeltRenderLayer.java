package com.neroferno.krm_onore.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_onore.item.BeltItem;
import com.neroferno.krm_onore.item.ModItems;
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
    // (currently the Arcle Driver).
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

        ResourceLocation textureRes = BeltArmorRenderer.getBeltTextureForPlayer(player, beltItem);
        beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                partialTick, packedLight, textureRes, 1.0F, false);

        if (beltItem == ModItems.ARCLE_DRIVER.get()) {
            poseStack.pushPose();
            // Micro-inflation to prevent Z-fighting between base belt mesh and translucent core texture overlay
            poseStack.scale(1.0015F, 1.0015F, 1.0015F);
            if (transformed && player.getPersistentData().getInt("krm_revo:form") == 1) {
                float progress = getGrowingCoreTransitionProgress(player, partialTick);
                if (progress < 1.0F) {
                    beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                            partialTick, packedLight, getCoreTexture("off"), 1.0F - progress, true);
                }
                beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                        partialTick, packedLight, getCoreTexture("growing"), progress, true);
            } else {
                ResourceLocation coreTexture = getCoreTextureForPlayer(player);
                beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                        partialTick, packedLight, coreTexture, 1.0F, true);
            }
            poseStack.popPose();
        }

        if (transformed) {
            model.setAllVisible(false);
        }

        poseStack.popPose();
    }

    private static float getGrowingCoreTransitionProgress(AbstractClientPlayer player, float partialTick) {
        if (!player.getPersistentData().getBoolean("krm_revo:has_transform_start_tick")) {
            return 1.0F;
        }
        long startTick = player.getPersistentData().getLong("krm_revo:transform_start_tick");
        return Math.min(1.0F, Math.max(0.0F, (player.tickCount + partialTick - startTick) / 12.0F));
    }

    private static ResourceLocation getCoreTextureForPlayer(AbstractClientPlayer player) {
        String coreTexture = "off";
        if (TransformationHelper.isTransformed(player)) {
            coreTexture = switch (player.getPersistentData().getInt("krm_revo:form")) {
                case 1 -> "growing";
                case 2 -> "mighty";
                case 3 -> "dragon";
                case 4 -> "pegasus";
                case 5 -> "titan";
                default -> "off";
            };
        }
        return getCoreTexture(coreTexture);
    }

    private static ResourceLocation getCoreTexture(String form) {
        return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcle_core_" + form + ".png");
    }
}
