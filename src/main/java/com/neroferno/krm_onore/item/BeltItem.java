package com.neroferno.krm_onore.item;

import com.neroferno.krm_onore.attachment.ModAttachments;
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

/**
 * The KRM Driver Belt item with full GeckoLib 3D model support.
 *
 * It acts as a Curio accessory.
 */
@SuppressWarnings("null")
public class BeltItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BeltItem() {
        super(new Item.Properties().stacksTo(1));
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
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new com.neroferno.krm_onore.client.renderer.BeltItemRenderer();
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 5, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.driver_belt.idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}