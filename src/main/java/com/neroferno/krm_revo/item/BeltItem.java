package com.neroferno.krm_revo.item;

import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.client.renderer.BeltArmorRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The KRM Driver Belt item with full GeckoLib 3D model support.
 *
 * idle animation and both renderers (handheld item + on-body armor layer)
 * are supplied per-instance via the constructor, so each Rider registers
 * its own BeltItem with its own look — nothing here is hardcoded to
 * Kuuga's driver_belt files anymore. This is what lets a second Driver
 * (e.g. Agito's arcle_driver) use its own geo/arcletexture/json instead of
 * inheriting Kuuga's.
 *
 * It acts as a Curio accessory.
 */
@SuppressWarnings("null")
public class BeltItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String idleAnimation;
    private final Supplier<BlockEntityWithoutLevelRenderer> rendererFactory;
    private final Supplier<BeltArmorRenderer> armorRendererFactory;

    /**
     * @param idleAnimation        GeckoLib animation name for this belt's idle loop,
     *                             e.g. "animation.driver_belt.idle" / "animation.arcle_driver.idle".
     * @param rendererFactory      supplies this belt's handheld/inventory item renderer, e.g.
     *                             {@code () -> new BeltItemRenderer(geo, texture, animation)}.
     *                             Called lazily each time the client asks for a renderer.
     * @param armorRendererFactory supplies this belt's on-body armor-layer renderer, e.g.
     *                             {@code () -> new BeltArmorRenderer(geo, texture, animation)}.
     *                             Used by BeltRenderLayer so the correct model is drawn on the
     *                             player regardless of which Rider's belt is equipped.
     */
    public BeltItem(String idleAnimation,
                     Supplier<BlockEntityWithoutLevelRenderer> rendererFactory,
                     Supplier<BeltArmorRenderer> armorRendererFactory) {
        super(new Item.Properties().stacksTo(1));
        this.idleAnimation = idleAnimation;
        this.rendererFactory = rendererFactory;
        this.armorRendererFactory = armorRendererFactory;
    }

    /**
     * Right-click with the belt in hand → equip it into the RIDER_INVENTORY slot (GUI belt slot).
     * Does NOT trigger transformation; that still requires the keybind while equipped.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        ItemStack beltStack = player.getItemInHand(hand);
        ItemStackHandler riderInv = player.getData(ModAttachments.RIDER_INVENTORY);

        // Only equip if the slot is empty
        if (riderInv.getStackInSlot(0).isEmpty()) {
            riderInv.setStackInSlot(0, beltStack.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            return InteractionResultHolder.consume(ItemStack.EMPTY);
        } else {
            // Slot already has a belt
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("krm_revo.belt.already_equipped"));
            }
            return InteractionResultHolder.fail(beltStack);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return rendererFactory.get();
            }
        });
    }

    /**
     * Used by BeltRenderLayer to draw whichever belt is actually equipped
     * with its own model/texture/animation, instead of a single hardcoded
     * renderer for every Rider's Driver.
     */
    public BeltArmorRenderer getArmorRenderer() {
        return armorRendererFactory.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 5, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(idleAnimation));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
