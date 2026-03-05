package com.neroferno.krm_revo.block;

import com.neroferno.krm_revo.KRMRevoMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all blocks for the mod.
 */
@SuppressWarnings("null")
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KRMRevoMod.MODID);

    // The workstation for the KRM Researcher villager profession
    public static final DeferredBlock<Block> RESEARCHER_DESK = registerBlock("researcher_desk",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // The KRM Rider workbench — right-click opens the Rider equipment/skill-tree GUI
    public static final DeferredBlock<MesaDeTrabajoBlock> MESA_DE_TRABAJO = registerBlock("mesa_de_trabajo",
            () -> new MesaDeTrabajoBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, java.util.function.Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

