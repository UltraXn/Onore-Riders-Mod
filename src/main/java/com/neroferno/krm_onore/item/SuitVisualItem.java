package com.neroferno.krm_onore.item;

import com.neroferno.krm_onore.combat.RiderFistTiming;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A hidden dummy item that acts as the Animatable for the Traje (Suit) visuals.
 * The SuitRenderLayer uses this to power the GeoArmorRenderer natively.
 */
@SuppressWarnings("null")
public class SuitVisualItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SuitVisualItem() {
        super(new Item.Properties());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "suit", RiderFistTiming.DEFAULT_ANIMATION_BLEND_TICKS, state -> PlayState.STOP)
                .triggerableAnim("punch", RawAnimation.begin().thenPlay("animation.punch"))
                .triggerableAnim("punch2", RawAnimation.begin().thenPlay("animation.punch2"))
                .triggerableAnim("punch1L", RawAnimation.begin().thenPlay("animation.punch1L"))
                .triggerableAnim("punch2L", RawAnimation.begin().thenPlay("animation.punch2L"))
                .triggerableAnim("punch3L", RawAnimation.begin().thenPlay("animation.punch3L"))
                .triggerableAnim("punch1H", RawAnimation.begin().thenPlay("animation.punch1H"))
                .triggerableAnim("punch2H", RawAnimation.begin().thenPlay("animation.punch2H"))
                .triggerableAnim("punch3H", RawAnimation.begin().thenPlay("animation.punch3H"))
                .triggerableAnim("punch1H_fp", RawAnimation.begin().thenPlay("animation.punch1H_fp"))
                .triggerableAnim("punch2H_fp", RawAnimation.begin().thenPlay("animation.punch2H_fp"))
                .triggerableAnim("punch3H_fp", RawAnimation.begin().thenPlay("animation.punch3H_fp"))
                .triggerableAnim("kick", RawAnimation.begin().thenPlay("animation.kick"))
                .triggerableAnim("especial1_1", RawAnimation.begin().thenPlayAndHold("animation.especial1_1"))
                .triggerableAnim("especial1_2", RawAnimation.begin().thenPlay("animation.especial1_2"))
                .triggerableAnim("especial1_loop", RawAnimation.begin().thenLoop("animation.especial1_loop"))
                .triggerableAnim("especial1_end", RawAnimation.begin().thenPlay("animation.especial1_end"))
                .triggerableAnim("especial1_cancel", RawAnimation.begin().thenPlay("animation.especial1_cancel"))
                .triggerableAnim("especial2_1", RawAnimation.begin().thenPlay("animation.especial2_1"))
                .triggerableAnim("especial2_2", RawAnimation.begin().thenPlay("animation.especial2_2"))
                .triggerableAnim("especial2_cancel", RawAnimation.begin().thenPlay("animation.especial2_cancel"))
                .triggerableAnim("especial3", RawAnimation.begin().thenPlay("animation.especial3"))
                .triggerableAnim("block_hold", RawAnimation.begin().thenPlayAndHold("animation.block_hold"))
                .triggerableAnim("block_release", RawAnimation.begin().thenPlay("animation.block_release")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
