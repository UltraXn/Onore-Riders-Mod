package com.neroferno.krm_onore.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_onore.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_onore.item.BeltItem;
import com.neroferno.krm_onore.item.ModItems;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
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

    // Core off -> growing fade, synced to kuuga_transform (ticks since the animation started on the client).
    // Starts at t = 0.5 s of kuuga_transform (0.5 s * 20 = 10 ticks).
    // Ajusta CORE_FADE_OFFSET_TICKS si el cambio no coincide exactamente con ese frame.
    //   - Si el core cambia TARDE  -> pon un valor negativo (ej. -2.0F).
    //   - Si el core cambia TEMPRANO -> pon un valor positivo.
    private static final float CORE_FADE_OFFSET_TICKS = 0.0F;
    private static final float CORE_FADE_START_TICKS = 10.0F + CORE_FADE_OFFSET_TICKS;
    private static final float CORE_FADE_DURATION_TICKS = 3.0F;
    // Pon en true para ver en el log el tiempo transcurrido de la animación (ventana alrededor del botón).
    private static final boolean CORE_FADE_DEBUG = false;
    // Extra ticks to wait for the server sync (isTransformed) before giving up on a pending transformation.
    private static final float CORE_PENDING_GRACE_TICKS = 40.0F;

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
            float growingProgress = getGrowingCoreProgress(player, transformed, partialTick);
            if (growingProgress >= 0.0F) {
                if (growingProgress < 1.0F) {
                    beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                            partialTick, packedLight, getCoreTexture("off"), 1.0F - growingProgress, true);
                }
                if (growingProgress > 0.0F) {
                    beltRenderer.renderWithTexture(player, beltStack, model, EquipmentSlot.LEGS, poseStack, buffer,
                            partialTick, packedLight, getCoreTexture(getRevealCoreName(player)), growingProgress, true);
                }
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

    /**
     * Growing core fade progress (0..1), or -1 when the Growing core logic does not apply.
     * While kuuga_transform is tracked, the fade is tied to the animation clock so it runs on the
     * "boton derecha" press, even before the server marks the player as transformed.
     */
    private static float getGrowingCoreProgress(AbstractClientPlayer player, boolean transformed, float partialTick) {
        if (!player.hasData(ModAttachments.RIDER_STATE)) return -1.0F;
        var state = player.getData(ModAttachments.RIDER_STATE);
        if (!SuitVisualRenderer.usesRevealAnimation(state.getForm())) return -1.0F;

        if (state.hasTransformAnimationStartTick()) {
            float animElapsed = player.tickCount + partialTick - state.getTransformAnimationStartTick();
            if (!transformed && animElapsed > state.getTransformAnimationTicks() + CORE_PENDING_GRACE_TICKS) {
                return -1.0F; // transformation never completed
            }
            if (CORE_FADE_DEBUG && animElapsed > 0.0F && animElapsed < 30.0F) {
                System.out.println("[CoreFade] elapsed=" + animElapsed + " (inicio fade=" + CORE_FADE_START_TICKS + ")");
            }
            // kuuga_transform_fast no tiene la pulsacion del boton: el core cambia al completarse la transformacion.
            float fadeStart = state.getTransformAnimationTicks() == TransformationHelper.TRANSFORM_ANIMATION_FAST_TICKS
                    ? state.getTransformAnimationTicks()
                    : CORE_FADE_START_TICKS;
            return Math.min(1.0F, Math.max(0.0F, (animElapsed - fadeStart) / CORE_FADE_DURATION_TICKS));
        }
        if (!transformed) return -1.0F;
        if (CORE_FADE_DEBUG) {
            // Si ves este log al final de la animación, el fade viene de esta rama (12 ticks tras isTransformed),
            // lo que significa que hasTransformAnimationStartTick() era false durante la animación.
            System.out.println("[CoreFade] RAMA FALLBACK (sin animationStartTick), transformed=true");
        }
        return getGrowingCoreTransitionProgress(player, partialTick);
    }

    /** Core al que llega el fade del henshin: la textura final debe coincidir con el formulario activo. */
    private static String getRevealCoreName(AbstractClientPlayer player) {
        return getCoreTextureNameForForm(player.hasData(ModAttachments.RIDER_STATE)
                ? player.getData(ModAttachments.RIDER_STATE).getForm()
                : 1);
    }

    private static float getGrowingCoreTransitionProgress(AbstractClientPlayer player, float partialTick) {
        if (!player.hasData(ModAttachments.RIDER_STATE) || !player.getData(ModAttachments.RIDER_STATE).hasTransformStartTick()) {
            return 1.0F;
        }
        long startTick = player.getData(ModAttachments.RIDER_STATE).getTransformStartTick();
        return Math.min(1.0F, Math.max(0.0F, (player.tickCount + partialTick - startTick) / 12.0F));
    }

    private static ResourceLocation getCoreTextureForPlayer(AbstractClientPlayer player) {
        String coreTexture = TransformationHelper.isTransformed(player)
                ? getCoreTextureNameForForm(player.hasData(ModAttachments.RIDER_STATE)
                        ? player.getData(ModAttachments.RIDER_STATE).getForm()
                        : 1)
                : "off";
        return getCoreTexture(coreTexture);
    }

    private static String getCoreTextureNameForForm(int form) {
        return switch (form) {
            case 1 -> "growing";
            case 2 -> "mighty";
            case 3 -> "dragon";
            case 4 -> "pegasus";
            case 5 -> "titan";
            default -> "off";
        };
    }

    private static ResourceLocation getCoreTexture(String form) {
        return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/item/arcle_core_" + form + ".png");
    }
}
