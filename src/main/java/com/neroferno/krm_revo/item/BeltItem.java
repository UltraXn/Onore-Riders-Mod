package com.neroferno.krm_revo.item;

import com.neroferno.krm_revo.attachment.ModAttachments;
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
 * FIX: idle animation and custom renderer used to be hardcoded on this
 * class, so every Rider's belt rendered with Kuuga's model/animation
 * regardless of which Rider it actually belonged to. Both are now supplied
 * per-instance via the constructor, so each Rider registers its own
 * BeltItem with its own look.
 *
 * It acts as a Curio accessory.
 */
@SuppressWarnings("null")
public class BeltItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String idleAnimation;
    private final Supplier<BlockEntityWithoutLevelRenderer> rendererFactory;

    /**
     * @param idleAnimation   GeckoLib animation name for this belt's idle loop,
     *                        e.g. "animation.driver_belt.idle".
     * @param rendererFactory supplies this belt's custom item renderer, e.g.
     *                        {@code BeltItemRenderer::new}. Called lazily
     *                        each time the client asks for a renderer, same
     *                        as the old hardcoded behavior.
     */
    public BeltItem(String idleAnimation, Supplier<BlockEntityWithoutLevelRenderer> rendererFactory) {
        super(new Item.Properties().stacksTo(1));
        this.idleAnimation = idleAnimation;
        this.rendererFactory = rendererFactory;
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
