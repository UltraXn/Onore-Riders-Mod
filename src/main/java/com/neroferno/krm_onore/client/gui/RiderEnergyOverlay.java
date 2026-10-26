package com.neroferno.krm_onore.client.gui;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderEnergyData;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.neroferno.krm_onore.KRMRevoMod;

/**
 * Client-side HUD overlay for the Rider Energy (Henshin Gauge) bar.
 * Positioned above the Vanilla Experience bar with a dynamic Driver icon.
 */
@SuppressWarnings({"null", "resource"})
public class RiderEnergyOverlay {
    private static final ResourceLocation GROWING_ICON =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/hud/growing_icon.png");
    private static final ResourceLocation MIGHTY_ICON =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/hud/mighty_icon.png");

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // Only display the overlay if a belt is equipped or the player is currently transformed
        boolean isTransformed = TransformationHelper.isTransformed(player);
        boolean hasBelt = TransformationHelper.isBeltEquipped(player);
        if (!isTransformed && !hasBelt) return;
        int form = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE).getForm() : 1;

        RiderEnergyData energyData = player.getData(ModAttachments.RIDER_ENERGY);
        float ratio = energyData.getPercentage(); // 0.0f to 1.0f

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Match the 81-pixel width of the vanilla hunger meter, including the form icon.
        int groupWidth = 81;
        int barHeight = 5;
        int iconWidth = 13;
        int iconHeight = 13;
        int iconGap = 2;
        int barWidth = groupWidth - iconWidth - iconGap;
        int groupX = screenWidth / 2 + 10;
        int iconX = groupX;
        int x = iconX + iconWidth + iconGap;
        int hungerBarY = screenHeight - 39;
        int iconY = hungerBarY - iconHeight - 2;
        int y = iconY + (iconHeight - barHeight) / 2;

        // 1. Render a form icon only while transformed; forms without a supplied icon stay blank.
        if (isTransformed && (form == 1 || form == 2)) {
            ResourceLocation formIcon = form == 1 ? GROWING_ICON : MIGHTY_ICON;
            guiGraphics.blit(formIcon, iconX, iconY, iconWidth, iconHeight, 0, 0, 34, 32, 34, 32);
        }

        // 2. Render Outer Metallic Frame for Energy Bar
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xEE12141A);
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF353945);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF0D0E12);

        // 3. Render Energy Fill Bar
        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            int primaryColor;
            int shineColor;
            int shadowColor;

            if (!isTransformed) {
                primaryColor = 0xFFE0E0E0; // White
                shineColor   = 0xFFFFFFFF;
                shadowColor  = 0xFF9E9E9E;
            } else if (form == 1) { // Growing Form
                primaryColor = 0xFFFF9800; // Orange
                shineColor   = 0xFFFFCC80;
                shadowColor  = 0xFFE65100;
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

        // Keep the percentage inside the group's width, above the icon and bar.
        String text = String.format("%.0f%%", ratio * 100.0f);
        int textX = groupX + groupWidth - mc.font.width(text);
        int textY = iconY - mc.font.lineHeight;
        guiGraphics.drawString(mc.font, text, textX, textY, 0xFFE0E0E0, true);
    }
}
