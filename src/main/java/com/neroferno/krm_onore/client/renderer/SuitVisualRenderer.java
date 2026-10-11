package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.SuitVisualItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.neroferno.krm_onore.attachment.ModAttachments;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SuitVisualRenderer extends GeoArmorRenderer<SuitVisualItem> {
    private static final float REVEAL_INTERVAL_TICKS = 3.0F;
    private static final float REVEAL_DURATION_TICKS = 3.0F;
    private Float growingRevealProgress;

    public SuitVisualRenderer() {
        super(new SuitModel());
        ((SuitModel) this.getGeoModel()).setRenderer(this);
    }

    public void setGrowingRevealProgress(float progress) {
        if (!this.isFirstPerson) {
            this.growingRevealProgress = progress;
        }
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
        model.getBone(boneName).ifPresent(bone -> bone.updateScale(1.0F, 1.0F, 1.0F));
    }

    private void applyGrowingReveal(GeoModel<SuitVisualItem> model) {
        if (this.isFirstPerson) {
            return;
        }
        if (this.growingRevealProgress == null) return;

        float elapsed = this.growingRevealProgress;
        if (elapsed >= REVEAL_INTERVAL_TICKS * 3.0F + REVEAL_DURATION_TICKS) {
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
        model.getBone(boneName).ifPresent(bone -> {
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

    public void setFirstPersonArm(net.minecraft.world.entity.HumanoidArm arm) {
        this.isFirstPerson = true;
        this.firstPersonArm = arm;
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

    private static void hideBone(GeoModel<SuitVisualItem> model, String boneName) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(true));
    }

    private static void showBone(GeoModel<SuitVisualItem> model, String boneName) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(false));
    }

    private static void showArmBone(GeoModel<SuitVisualItem> model, String boneName) {
        model.getBone(boneName).ifPresent(bone -> {
            bone.setHidden(false);
            bone.updateScale(1.0F, 1.0F, 1.0F);
        });
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
