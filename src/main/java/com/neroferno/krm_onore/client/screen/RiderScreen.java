package com.neroferno.krm_onore.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.gui.RiderEnergyOverlay;
import com.neroferno.krm_onore.item.ModItems;
import com.neroferno.krm_onore.menu.RiderMenu;
import com.neroferno.krm_onore.network.SelectFormPacket;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import com.neroferno.krm_onore.attachment.ModAttachments;

@SuppressWarnings("null")
public class RiderScreen extends AbstractContainerScreen<RiderMenu> {

    // Textures provided in assets/gui
    private static final ResourceLocation TEXTURE_MARCO =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/marco.png");
    private static final ResourceLocation BUTTON_IDLE =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/button_idle.png");
    private static final ResourceLocation BUTTON_HOVER =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/button_hover.png");
    private static final ResourceLocation GROWING_ICON =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/hud/growing_icon.png");
    private static final ResourceLocation MIGHTY_ICON =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/hud/mighty_icon.png");
    private static final ResourceLocation KUUGA_ICON =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/hud/kuuga_icon.png");
    private static final ResourceLocation TEXTURE_GROWING_IDLE =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/growing_button_idle.png");
    private static final ResourceLocation TEXTURE_GROWING_HOVER =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/growing_button_hover.png");
    private static final ResourceLocation TEXTURE_MIGHTY_IDLE =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/migthy_button_idle.png");
    private static final ResourceLocation TEXTURE_MIGHTY_HOVER =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/migthy_button_hover.png");

    public RiderScreen(RiderMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // Dimensions matching marco.png
        this.imageWidth = 387;
        this.imageHeight = 217;
    }

    @Override
    protected void init() {
        super.init();
        // Hide standard labels outside visible area
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draw the opaque frame first so its panels remain behind the interactive GUI content.
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE_MARCO, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        RenderSystem.disableBlend();

        int buttonX = x + 192;
        int buttonY = y + 28;
        ResourceLocation buttonTexture = pMouseX >= buttonX && pMouseX < buttonX + 75
                && pMouseY >= buttonY && pMouseY < buttonY + 19 ? BUTTON_HOVER : BUTTON_IDLE;
        guiGraphics.blit(buttonTexture, buttonX, buttonY, 0, 0, 75, 19, 75, 19);

        int leftButtonX = buttonX - 76;
        ResourceLocation leftButtonTexture = pMouseX >= leftButtonX && pMouseX < leftButtonX + 75
                && pMouseY >= buttonY && pMouseY < buttonY + 19 ? BUTTON_HOVER : BUTTON_IDLE;
        guiGraphics.blit(leftButtonTexture, leftButtonX, buttonY, 0, 0, 75, 19, 75, 19);
        guiGraphics.renderItem(ModItems.ARCLE_DRIVER.get().getDefaultInstance(), leftButtonX + 2, buttonY + 1);

        String driverName = "Arcle Driver";
        float driverNameScale = Math.min(1.0F, 49.0F / font.width(driverName));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftButtonX + 21, buttonY + 6, 0);
        guiGraphics.pose().scale(driverNameScale, driverNameScale, 1.0F);
        guiGraphics.drawString(font, driverName, 0, 0, 0xFFE6E7F0, false);
        guiGraphics.pose().popPose();

        if (this.minecraft != null && this.minecraft.player != null
                && TransformationHelper.isTransformed(this.minecraft.player)
                && this.minecraft.player.hasData(ModAttachments.RIDER_STATE)) {
            int form = this.minecraft.player.getData(ModAttachments.RIDER_STATE).getForm();
            if (form == 1 || form == 2) {
                ResourceLocation formIcon = form == 1 ? GROWING_ICON : MIGHTY_ICON;
                String formName = form == 1 ? "Growin Form" : "Mighty Form";
                int iconSize = 13;
                int contentGap = 3;
                int textWidth = font.width(formName);
                float textScale = Math.min(1.0F, 48.0F / textWidth);
                int contentWidth = iconSize + contentGap + (int) (textWidth * textScale);
                int contentX = buttonX + (75 - contentWidth) / 2;
                guiGraphics.blit(formIcon, contentX, buttonY + 3, iconSize, iconSize, 0, 0, 34, 32, 34, 32);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(contentX + iconSize + contentGap, buttonY + 6, 0);
                guiGraphics.pose().scale(textScale, textScale, 1.0F);
                guiGraphics.drawString(font, formName, 0, 0, 0xFFE6E7F0, false);
                guiGraphics.pose().popPose();
            }
        }

        String riderTitle = "KAMEN RIDER KUUGA";
        int iconWidth = 14;
        int iconHeight = 13;
        int iconGap = 2;
        int maxTitleGroupWidth = 100;
        int titleTextWidth = font.width(riderTitle);
        int fittedTextWidth = Math.min(titleTextWidth, maxTitleGroupWidth - iconWidth - iconGap);
        int titleGroupWidth = iconWidth + iconGap + fittedTextWidth;
        int titleGroupX = x + 54 - titleGroupWidth / 2;
        guiGraphics.blit(KUUGA_ICON, titleGroupX + 1, y + 5, iconWidth, iconHeight, 0, 0, 34, 32, 34, 32);

        float titleScale = (float) fittedTextWidth / titleTextWidth;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(titleGroupX + iconWidth + iconGap, y + 11, 0);
        guiGraphics.pose().scale(titleScale, titleScale, 1.0F);
        guiGraphics.drawString(font, riderTitle, 0, 0, 0xFFE6E7F0, false);
        guiGraphics.pose().popPose();

        // 3. Render 3D Player Avatar Preview in the top-left compartment
        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics, x + 28, y + 18, x + 86, y + 162, 54, 0.0f, pMouseX, pMouseY, this.minecraft.player
            );
        }

        // Show the active Rider form in the frame's lower-left inset; keep it empty when civil.
        if (this.minecraft != null && this.minecraft.player != null) {
            var player = this.minecraft.player;
            boolean isTransformed = TransformationHelper.isTransformed(player);
            int form = player.hasData(ModAttachments.RIDER_STATE)
                    ? player.getData(ModAttachments.RIDER_STATE).getForm()
                    : 1;

            if (isTransformed && (form == 1 || form == 2)) {
                ResourceLocation formIcon = form == 1 ? GROWING_ICON : MIGHTY_ICON;
                guiGraphics.blit(formIcon, x + 1, y + 145, 0, 0, 34, 32, 34, 32);

                String formName = form == 1 ? "Growin Form" : "Mighty Form";
                int formNameWidth = font.width(formName);
                float formNameScale = Math.min(1.2F, 58.0F / formNameWidth);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + 40, y + 170, 0);
                guiGraphics.pose().scale(formNameScale, formNameScale, 1.0F);
                guiGraphics.drawString(font, formName, 0, 0, 0xFFE6E7F0, false);
                guiGraphics.pose().popPose();
            }

            var energyData = player.getData(ModAttachments.RIDER_ENERGY);
            guiGraphics.drawString(font, "Energia Amadam", x + 10, y + 188, 0xFFE6E7F0, false);
            RiderEnergyOverlay.renderEnergyBar(
                    guiGraphics, x + 10, y + 202, 63, 6,
                    energyData.getPercentage(), isTransformed, form, 5
            );

            String energyAmount = Math.round(energyData.getPercentage() * 100.0F) + "%";
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 77, y + 201, 0);
            guiGraphics.pose().scale(1.0F, 1.0F, 1.0F);
            guiGraphics.drawString(font, energyAmount, 0, 0, 0xFFE6E7F0, false);
            guiGraphics.pose().popPose();
        }

        // 4. Render Form Selection Buttons on the Top Bar
        int currentForm = (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasData(ModAttachments.RIDER_STATE))
                ? this.minecraft.player.getData(ModAttachments.RIDER_STATE).getForm() : 0;

        boolean isGrowingHovered = pMouseX >= x + 111 && pMouseX < x + 111 + 34 && pMouseY >= y + 3 && pMouseY < y + 3 + 13;
        boolean isMightyHovered = pMouseX >= x + 145 && pMouseX < x + 145 + 26 && pMouseY >= y + 3 && pMouseY < y + 3 + 14;

        ResourceLocation growingTex = (isGrowingHovered || currentForm == 1) ? TEXTURE_GROWING_HOVER : TEXTURE_GROWING_IDLE;
        ResourceLocation mightyTex = (isMightyHovered || currentForm != 1) ? TEXTURE_MIGHTY_HOVER : TEXTURE_MIGHTY_IDLE;

        guiGraphics.blit(growingTex, x + 111, y + 3, 0, 0, 34, 13, 34, 13);
        guiGraphics.blit(mightyTex, x + 145, y + 3, 0, 0, 26, 14, 26, 14);

        // Active indicator ring around the selected button
        if (currentForm == 1) {
            guiGraphics.fill(x + 111, y + 14, x + 111 + 34, y + 15, 0xFFFFFFFF);
        } else {
            guiGraphics.fill(x + 145, y + 15, x + 145 + 26, y + 16, 0xFFFF3333);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Growing Form Tab Button (x+111, y+3, w=34, h=13)
        if (mouseX >= x + 111 && mouseX < x + 111 + 34 && mouseY >= y + 3 && mouseY < y + 3 + 13) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new SelectFormPacket(1));
            return true;
        }

        // Mighty Form Tab Button (x+145, y+3, w=26, h=14)
        if (mouseX >= x + 145 && mouseX < x + 145 + 26 && mouseY >= y + 3 && mouseY < y + 3 + 14) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new SelectFormPacket(0));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltips for interactive areas
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Form selection tabs
        if (mouseX >= x + 111 && mouseX < x + 111 + 34 && mouseY >= y + 3 && mouseY < y + 3 + 13) {
            guiGraphics.renderTooltip(font, Component.literal("§fKuuga — Forma Growing §7(Blanco Inicial)"), mouseX, mouseY);
        } else if (mouseX >= x + 145 && mouseX < x + 145 + 26 && mouseY >= y + 3 && mouseY < y + 3 + 14) {
            guiGraphics.renderTooltip(font, Component.literal("§cKuuga — Forma Mighty §7(Poder del Fuego)"), mouseX, mouseY);
        }
    }
}
