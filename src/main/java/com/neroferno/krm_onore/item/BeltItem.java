package com.neroferno.krm_onore.item;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.BeltArmorRenderer;
import com.neroferno.krm_onore.client.renderer.BeltItemRenderer;
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
 */
@SuppressWarnings("null")
public class BeltItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String idleAnimation;
    private final Supplier<BlockEntityWithoutLevelRenderer> rendererFactory;
    private final Supplier<BeltArmorRenderer> armorRendererFactory;

    public BeltItem() {
        this("animation.driver_belt.idle",
             BeltItemRenderer::new,
             BeltArmorRenderer::new);
    }

    public BeltItem(String idleAnimation,
                     Supplier<BlockEntityWithoutLevelRenderer> rendererFactory,
                     Supplier<BeltArmorRenderer> armorRendererFactory) {
        super(new Item.Properties().stacksTo(1));
        this.idleAnimation = idleAnimation;
        this.rendererFactory = rendererFactory;
        this.armorRendererFactory = armorRendererFactory;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        ItemStack beltStack = player.getItemInHand(hand);
        ItemStackHandler riderInv = player.getData(ModAttachments.RIDER_INVENTORY);

        if (riderInv.getStackInSlot(0).isEmpty()) {
            riderInv.setStackInSlot(0, beltStack.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            return InteractionResultHolder.consume(ItemStack.EMPTY);
        } else {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("krm_revo.belt.already_equipped"));
            }
            return InteractionResultHolder.fail(beltStack);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = rendererFactory != null ? rendererFactory.get() : new BeltItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    public BeltArmorRenderer getArmorRenderer() {
        return armorRendererFactory != null ? armorRendererFactory.get() : new BeltArmorRenderer();
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
