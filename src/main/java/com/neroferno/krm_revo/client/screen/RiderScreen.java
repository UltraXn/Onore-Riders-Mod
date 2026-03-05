package com.neroferno.krm_revo.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.neroferno.krm_revo.menu.RiderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("null")
public class RiderScreen extends AbstractContainerScreen<RiderMenu> {
    public RiderScreen(RiderMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 320;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // "Unequip" button beneath the player avatar
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Quitarse el belt"),
                button -> {
                    // Packet will be sent here in the future
                })
                .bounds(x + 10, y + 150, 90, 20)
                .build());

        // RGB Buttons for Customization (Right Panel)
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Rojo"),
                button -> { })
                .bounds(x + 230, y + 25, 80, 20)
                .build());
                
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Verde"),
                button -> { })
                .bounds(x + 230, y + 50, 80, 20)
                .build());

        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Azul"),
                button -> { })
                .bounds(x + 230, y + 75, 80, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draw main background outline
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);

        // LEFT PANEL (Preview + Armor Slots)
        guiGraphics.fill(x + 5, y + 5, x + 105, y + 175, 0xFF8B8B8B);
        // CENTER PANEL (Skill Tree)
        guiGraphics.fill(x + 110, y + 5, x + 220, y + 175, 0xFF8B8B8B);
        // RIGHT PANEL (Customization and Stats)
        guiGraphics.fill(x + 225, y + 5, x + 315, y + 175, 0xFF8B8B8B);

        // Draw Slot Backgrounds
        int[] slotYs = {20, 38, 56, 74, 92, 115};
        for (int sy : slotYs) {
            int slotX = x + 9;
            int slotY = y + sy - 1;
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
        }

        // Draw Texts
        guiGraphics.drawCenteredString(font, Component.literal("Kuuga"), x + 165, y + 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.literal("Growing Mighty"), x + 165, y + 22, 0xFFFFD700);
        guiGraphics.drawCenteredString(font, Component.literal("Árbol de Habilidades"), x + 165, y + 40, 0xFFFFFFFF);
        guiGraphics.drawString(font, Component.literal("SP: 5"), x + 115, y + 160, 0xFF55FF55);

        guiGraphics.drawCenteredString(font, Component.literal("Colores"), x + 270, y + 10, 0xFFFFFFFF);
        
        guiGraphics.drawCenteredString(font, Component.literal("Estadísticas"), x + 270, y + 105, 0xFFFFFFFF);
        guiGraphics.drawString(font, Component.literal("Vida: 100"), x + 230, y + 120, 0xFFFF5555);
        guiGraphics.drawString(font, Component.literal("Ataque: 10"), x + 230, y + 132, 0xFF5555FF);
        guiGraphics.drawString(font, Component.literal("Defensa: 15"), x + 230, y + 144, 0xFF55BB55);
        guiGraphics.drawString(font, Component.literal("Velocidad: 5"), x + 230, y + 156, 0xFFFFFF55);

        // Render 3D Entity Preview
        if (this.minecraft != null && this.minecraft.player != null) {
            // Adjust bounded box to nicely fit player inside the left panel
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, x + 30, y + 15, x + 100, y + 145, 45, 0.0f, pMouseX, pMouseY, this.minecraft.player
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
