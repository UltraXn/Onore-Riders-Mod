package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.SuitVisualItem;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.neroferno.krm_onore.attachment.ModAttachments;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.cache.object.GeoQuad;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SuitVisualRenderer extends GeoArmorRenderer<SuitVisualItem> {
    // Revelado del henshin completo: empieza al terminar kuuga_transform y acaba junto con el sonido
    // (inicio del sonido + duracion del sonido - duracion de la animacion) = 84 ticks.
    public static final float GROWING_REVEAL_TOTAL_TICKS =
            TransformationHelper.TRANSFORM_SOUND_START_TICKS
                    + TransformationHelper.TRANSFORM_SOUND_TICKS
                    - TransformationHelper.TRANSFORM_ANIMATION_TICKS;
    // Reparto del revelado en 4 partes (cuerpo, brazos, piernas, cabeza): 3 intervalos + 1 duracion = total.
    // Misma proporcion 4:5 que el revelado corto original (intervalo 4, duracion 5, total 17).
    private static final float REVEAL_INTERVAL_TICKS = GROWING_REVEAL_TOTAL_TICKS * (4.0F / 17.0F);
    private static final float REVEAL_DURATION_TICKS = GROWING_REVEAL_TOTAL_TICKS * (5.0F / 17.0F);
    // Revelado corto original, para kuuga_transform_fast y transformaciones sin animacion (sin sonido).
    private static final float SHORT_REVEAL_TOTAL_TICKS = 17.0F;
    private static final float SHORT_REVEAL_SCALE = GROWING_REVEAL_TOTAL_TICKS / SHORT_REVEAL_TOTAL_TICKS;
    private Float growingRevealProgress;

    /** Formas cuyo traje aparece parte por parte (Growing = 1, Mighty = 2). */
    public static boolean usesRevealAnimation(int form) {
        return form == 1 || form == 2;
    }

    public SuitVisualRenderer() {
        super(new SuitModel());
        ((SuitModel) this.getGeoModel()).setRenderer(this);
    }

    public void setGrowingRevealProgress(float progress) {
        if (!this.isFirstPerson) {
            this.growingRevealProgress = progress;
        }
    }

    public static float getGrowingRevealElapsedTicks(net.minecraft.world.entity.player.Player player, float partialTick) {
        if (!player.hasData(ModAttachments.RIDER_STATE)) {
            return GROWING_REVEAL_TOTAL_TICKS;
        }

        var stateData = player.getData(ModAttachments.RIDER_STATE);
        float currentTick = player.tickCount + partialTick;
        // Devuelve ticks en la linea de tiempo larga (GROWING_REVEAL_TOTAL_TICKS). Los casos sin sonido
        // (fast / sin animacion) se reescalan para que su revelado siga durando 17 ticks reales.
        if (stateData.hasTransformAnimationStartTick()) {
            float elapsed = Math.max(0.0F, currentTick - stateData.getTransformAnimationStartTick()
                    - stateData.getTransformAnimationTicks());
            return stateData.getTransformAnimationTicks() == TransformationHelper.TRANSFORM_ANIMATION_FAST_TICKS
                    ? elapsed * SHORT_REVEAL_SCALE
                    : elapsed;
        }
        if (stateData.hasTransformStartTick()) {
            return Math.max(0.0F, currentTick - stateData.getTransformStartTick()) * SHORT_REVEAL_SCALE;
        }
        return GROWING_REVEAL_TOTAL_TICKS;
    }

    /**
     * True when the vanilla player body must be hidden: the player is transformed and, for the
     * Growing form, the part-by-part suit reveal has already finished.
     */
    public static boolean shouldHideVanillaBody(net.minecraft.world.entity.player.Player player, float partialTick) {
        if (!TransformationHelper.isTransformed(player)) {
            return false;
        }
        if (!player.hasData(ModAttachments.RIDER_STATE)
                || !usesRevealAnimation(player.getData(ModAttachments.RIDER_STATE).getForm())) {
            return true;
        }
        return getGrowingRevealElapsedTicks(player, partialTick) >= GROWING_REVEAL_TOTAL_TICKS;
    }

    public void clearGrowingRevealProgress() {
        this.growingRevealProgress = null;
        resetAllBoneScales();
    }

    public void resetAllBoneScales() {
        GeoModel<SuitVisualItem> model = this.getGeoModel();
        if (model != null) {
            resetBone(model, "armorBody");
            resetBone(model, "armorLeftArm");
            resetBone(model, "armorRightArm");
            resetBone(model, "armorLeftLeg");
            resetBone(model, "armorRightLeg");
            resetBone(model, "armorHead");
        }
    }

    private static void resetBone(GeoModel<SuitVisualItem> model, String boneName) {
        getArmorBone(model, boneName).ifPresent(bone -> bone.updateScale(1.0F, 1.0F, 1.0F));
    }

    private void applyGrowingReveal(GeoModel<SuitVisualItem> model) {
        if (this.isFirstPerson) {
            return;
        }
        if (this.growingRevealProgress == null) return;

        float elapsed = this.growingRevealProgress;
        if (elapsed >= GROWING_REVEAL_TOTAL_TICKS) {
            clearGrowingRevealProgress();
            return;
        }

        applyBoneReveal(model, "armorBody", elapsed, 0.0F);
        applyBoneReveal(model, "armorLeftArm", elapsed, REVEAL_INTERVAL_TICKS);
        applyBoneReveal(model, "armorRightArm", elapsed, REVEAL_INTERVAL_TICKS);
        applyBoneReveal(model, "armorLeftLeg", elapsed, REVEAL_INTERVAL_TICKS * 2.0F);
        applyBoneReveal(model, "armorRightLeg", elapsed, REVEAL_INTERVAL_TICKS * 2.0F);
        applyBoneReveal(model, "armorHead", elapsed, REVEAL_INTERVAL_TICKS * 3.0F);
    }

    private static void applyBoneReveal(GeoModel<SuitVisualItem> model, String boneName, float elapsed, float start) {
        getArmorBone(model, boneName).ifPresent(bone -> {
            if (elapsed < start) {
                bone.setHidden(true);
                bone.updateScale(1.0F, 1.0F, 1.0F);
                return;
            }

            bone.setHidden(false);
            float scale = Math.min(1.0F, Math.max(0.01F, (elapsed - start) / REVEAL_DURATION_TICKS));
            bone.updateScale(scale, scale, scale);
        });
    }

    private boolean isFirstPerson = false;
    private net.minecraft.world.entity.HumanoidArm firstPersonArm = net.minecraft.world.entity.HumanoidArm.RIGHT;

    // Si es true, TODAS las caras del brazo en primera persona usan la misma normal, asi que el
    // sombreado direccional es identico en cada pieza (placa, munequera, antebrazo...).
    private boolean firstPersonFlatLight = false;

    public void setFirstPersonArm(net.minecraft.world.entity.HumanoidArm arm) {
        this.isFirstPerson = true;
        this.firstPersonArm = arm;
    }

    public void setFirstPersonFlatLight(boolean flat) {
        this.firstPersonFlatLight = flat;
    }

    /**
     * GeckoLib calcula la normal de cada cara a partir del cubo y la manda al shader. Aqui se unifica
     * en el propio renderer (no en un VertexConsumer envuelto), asi que funciona aunque GeckoLib
     * vuelva a pedir su propio buffer al renderizar.
     */
    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                     int packedOverlay, int packedLight, int colour) {
        if (this.isFirstPerson && this.firstPersonFlatLight) {
            normal.set(0.0F, 1.0F, 0.0F);
        }
        super.createVerticesOfQuad(quad, poseState, normal, buffer, packedOverlay, packedLight, colour);
    }

    public void clearFirstPerson() {
        this.isFirstPerson = false;
        resetAllBoneScales();
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        GeoModel<SuitVisualItem> model = this.getGeoModel();
        if (this.isFirstPerson) {
            this.setAllVisible(false);
            if (model != null) {
                hideBone(model, "armorHead");
                hideBone(model, "armorBody");
                hideBone(model, "armorLeftLeg");
                hideBone(model, "armorRightLeg");
                if (this.firstPersonArm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                    hideBone(model, "armorLeftArm");
                    showArmBone(model, "armorRightArm");
                } else {
                    hideBone(model, "armorRightArm");
                    showArmBone(model, "armorLeftArm");
                }
            }
        } else {
            this.setAllVisible(true);
            if (model != null) {
                showBone(model, "armorHead");
                showBone(model, "armorBody");
                showBone(model, "armorLeftLeg");
                showBone(model, "armorRightLeg");
                showBone(model, "armorLeftArm");
                showBone(model, "armorRightArm");
            }
        }
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getHeadBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorHead").orElse(null);
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getBodyBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorBody").orElse(null);
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getRightArmBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorRightArm").orElse(null);
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getLeftArmBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorLeftArm").orElse(null);
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getRightLegBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorRightLeg").orElse(null);
    }

    @Override
    public software.bernie.geckolib.cache.object.GeoBone getLeftLegBone(GeoModel<SuitVisualItem> model) {
        return getArmorBone(model, "armorLeftLeg").orElse(null);
    }

    private static void hideBone(GeoModel<SuitVisualItem> model, String boneName) {
        getArmorBone(model, boneName).ifPresent(bone -> bone.setHidden(true));
    }

    private static void showBone(GeoModel<SuitVisualItem> model, String boneName) {
        getArmorBone(model, boneName).ifPresent(bone -> bone.setHidden(false));
    }

    private static void showArmBone(GeoModel<SuitVisualItem> model, String boneName) {
        getArmorBone(model, boneName).ifPresent(bone -> {
            bone.setHidden(false);
            bone.updateScale(1.0F, 1.0F, 1.0F);
        });
    }

    private static java.util.Optional<software.bernie.geckolib.cache.object.GeoBone> getArmorBone(
            GeoModel<SuitVisualItem> model, String boneName) {
        String growingFormBoneName = switch (boneName) {
            case "armorHead" -> "Head";
            case "armorBody" -> "Body";
            case "armorLeftArm" -> "LeftArm";
            case "armorRightArm" -> "RightArm";
            case "armorLeftLeg" -> "LeftLeg";
            case "armorRightLeg" -> "RightLeg";
            default -> boneName;
        };
        return model.getBone(boneName).or(() -> model.getBone(growingFormBoneName));
    }

    private static class SuitModel extends GeoModel<SuitVisualItem> {
        private SuitVisualRenderer renderer;

        public void setRenderer(SuitVisualRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public ResourceLocation getModelResource(SuitVisualItem animatable) {
            if (this.renderer != null) {
                net.minecraft.world.entity.Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    int form = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE).getForm() : 1;
                    if (form == 1) {
                        return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/armor/growing_suit.geo.json");
                    }
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "geo/armor/kuuga_suit.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SuitVisualItem animatable) {
            if (this.renderer != null) {
                net.minecraft.world.entity.Entity entity = this.renderer.getCurrentEntity();
                if (entity instanceof Player player) {
                    int form = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE).getForm() : 1;
                    if (form == 1) {
                        return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/growing_suit.png");
                    }
                }
            }
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");
        }

        @Override
        public ResourceLocation getAnimationResource(SuitVisualItem animatable) {
            return ResourceLocation.fromNamespaceAndPath("krm_revo", "animations/armor/kuuga_suit.animation.json");
        }

        @Override
        public void setCustomAnimations(SuitVisualItem animatable, long instanceId, AnimationState<SuitVisualItem> animationState) {
            if (this.renderer != null) {
                this.renderer.applyGrowingReveal(this);
            }
        }
    }
}
