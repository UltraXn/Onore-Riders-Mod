package com.neroferno.krm_onore.client.gui;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderEnergyData;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Client-side HUD overlay for the Rider Energy (Henshin Gauge) bar.
 * Positioned above the Vanilla Experience bar with a dynamic Driver icon.
 */
@SuppressWarnings({"null", "resource"})
public class RiderEnergyOverlay {

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // Only display the overlay if a belt is equipped or the player is currently transformed
        boolean isTransformed = TransformationHelper.isTransformed(player);
        boolean hasBelt = TransformationHelper.isBeltEquipped(player);
        if (!isTransformed && !hasBelt) return;

        RiderEnergyData energyData = player.getData(ModAttachments.RIDER_ENERGY);
        float ratio = energyData.getPercentage(); // 0.0f to 1.0f

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Dimensions of the Rider Energy Bar
        int barWidth = 120;
        int barHeight = 5;

        // Position directly above experience bar:
        // Vanilla exp bar is at screenHeight - 29 (height 5)
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight - 40;

        // Adjust if player has experience level number rendered
        if (player.experienceLevel > 0) {
            y = screenHeight - 44;
        }

        // Driver Icon dimensions and position:
        int iconSize = 16;
        int iconX = x - iconSize - 4;
        int iconY = y - (iconSize - barHeight) / 2;

        // Retrieve equipped belt item
        ItemStack beltStack = ItemStack.EMPTY;
        if (player.hasData(ModAttachments.RIDER_INVENTORY)) {
            ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
            beltStack = inv.getStackInSlot(0);
        }

        // 1. Render Driver Icon or fallback crest
        if (!beltStack.isEmpty()) {
            guiGraphics.renderItem(beltStack, iconX, iconY);
        } else {
            guiGraphics.fill(iconX + 2, iconY + 2, iconX + 14, iconY + 14, 0xDD202028);
            guiGraphics.fill(iconX + 4, iconY + 4, iconX + 12, iconY + 12, 0xFFE5B020);
        }

        // 2. Render Outer Metallic Frame for Energy Bar
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xEE12141A);
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF353945);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF0D0E12);

        // 3. Render Energy Fill Bar
        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            int form = player.getPersistentData().getInt("krm_revo:form");

            int primaryColor;
            int shineColor;
            int shadowColor;

            if (!isTransformed) {
                primaryColor = 0xFFFFC107; // Tech Gold
                shineColor   = 0xFFFFE082;
                shadowColor  = 0xFFB78103;
            } else if (form == 1) { // Growing Form
                primaryColor = 0xFFE0E0E0; // Silver White
                shineColor   = 0xFFFFFFFF;
                shadowColor  = 0xFF9E9E9E;
            } else { // Mighty Form (default)
                primaryColor = 0xFFE52521; // Crimson Red
                shineColor   = 0xFFFF6E40;
                shadowColor  = 0xFF990000;
            }

            // Low energy alert pulse (when energy < 20%)
            if (ratio < 0.20f) {
                long time = System.currentTimeMillis();
                if ((time / 250) % 2 == 0) {
                    primaryColor = 0xFFFF1744; // Flashing hazard red
                    shineColor   = 0xFFFF8A80;
                    shadowColor  = 0xFFB71C1C;
                }
            }

            // Top highlight line (1px)
            guiGraphics.fill(x, y, x + fillWidth, y + 1, shineColor);
            // Middle core fill
            guiGraphics.fill(x, y + 1, x + fillWidth, y + barHeight - 1, primaryColor);
            // Bottom shading line (1px)
            guiGraphics.fill(x, y + barHeight - 1, x + fillWidth, y + barHeight, shadowColor);
        }

        // 4. Subtle Energy Ticks / Dividers (every 25%)
        for (int i = 1; i <= 3; i++) {
            int tickX = x + (barWidth * i / 4);
            guiGraphics.fill(tickX, y, tickX + 1, y + barHeight, 0x66000000);
        }

        // 5. Clean Energy Percentage Text
        String text = String.format("%.0f%%", ratio * 100.0f);
        int textX = x + barWidth + 4;
        int textY = y - 2;

        guiGraphics.drawString(mc.font, text, textX, textY, 0xFFE0E0E0, true);
    }
}
