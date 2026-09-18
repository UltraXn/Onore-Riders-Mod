package com.neroferno.krm_onore.block;

import com.neroferno.krm_onore.menu.RiderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The KRM Rider Workbench.
 * Right-clicking opens the Rider equipment/skill-tree GUI.
 */
@SuppressWarnings("null")
public class MesaDeTrabajoBlock extends Block {

    public MesaDeTrabajoBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new RiderMenu(containerId, playerInventory, null),
                    Component.translatable("gui.krm_revo.rider_inventory")
            ));
        }

        return InteractionResult.CONSUME;
    }
}
