package com.neroferno.krm_onore.client.renderer;

import com.neroferno.krm_onore.item.SuitVisualItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
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
        this.growingRevealProgress = progress;
    }

    public void clearGrowingRevealProgress() {
        this.growingRevealProgress = null;
    }

    private void applyGrowingReveal(GeoModel<SuitVisualItem> model) {
        if (this.growingRevealProgress == null) return;

        float elapsed = this.growingRevealProgress;
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
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot currentSlot) {
        if (this.isFirstPerson) {
            this.setAllVisible(false);
            if (this.firstPersonArm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                if (this.rightArm != null) this.rightArm.setHidden(false);
            } else {
                if (this.leftArm != null) this.leftArm.setHidden(false);
            }
        } else {
            this.setAllVisible(true);
        }
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
                    int form = player.getPersistentData().getInt("krm_revo:form");
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
                    int form = player.getPersistentData().getInt("krm_revo:form");
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
