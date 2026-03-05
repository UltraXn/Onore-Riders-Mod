package com.neroferno.krm_revo.event;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.item.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

/**
 * Handles the registration of custom trades for modded villager professions.
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public class VillagerTradeHandler {

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.KRM_RESEARCHER.get()) {
            
            // Level 1: Novice
            List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
            level1.add(new BasicItemListing(
                    new ItemStack(Items.PAPER, 24),
                    new ItemStack(Items.EMERALD, 1),
                    16, 2, 0.05f));
            
            level1.add(new BasicItemListing(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.GLASS_BOTTLE, 4),
                    12, 1, 0.05f));

            // Level 2: Apprentice
            List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
            level2.add(new BasicItemListing(
                    new ItemStack(Items.BOOK, 4),
                    new ItemStack(Items.EMERALD, 1),
                    12, 10, 0.05f));
            
            // Placeholder for Special Research Map (just an Empty Map for now)
            level2.add(new BasicItemListing(
                    new ItemStack(Items.EMERALD, 13),
                    new ItemStack(Items.MAP, 1),
                    12, 5, 0.05f));

            // Level 5: Master
            List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
            // High-tier trade for the Driver Belt
            level5.add(new BasicItemListing(
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.ECHO_SHARD, 1),
                    ModItems.DRIVER_BELT.get().getDefaultInstance(),
                    1, 30, 0.05f));
        }
    }
}

