package com.neroferno.krm_revo.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.menu.RiderMenu;
import com.neroferno.krm_revo.network.SelectFormPacket;
import com.neroferno.krm_revo.network.TransformationHelper;
import com.neroferno.krm_revo.network.UnequipBeltPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

@SuppressWarnings("null")
public class RiderScreen extends AbstractContainerScreen<RiderMenu> {

    // Textures provided in assets/gui
    private static final ResourceLocation TEXTURE_MARCO =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/marco.png");
    private static final ResourceLocation TEXTURE_PERSONAJE =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/personaje_display.png");
    private static final ResourceLocation TEXTURE_SKILLS =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/skills_display.png");
    private static final ResourceLocation TEXTURE_STATS =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "textures/gui/stats_display.png");
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
        this.imageWidth = 349;
        this.imageHeight = 205;
    }

    @Override
    protected void init() {
        super.init();
        // Hide standard labels outside visible area
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // "Unequip Belt" button situated in the right-bottom action compartment
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.krm_revo.skill_tree.unequip"),
                button -> {
                    PacketDistributor.sendToServer(new UnequipBeltPacket());
                    this.onClose();
                })
                .bounds(x + 272, y + 160, 60, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 1. Render Backgrounds inside each compartment

        // Left-Top Panel (Character preview backdrop): x=15..79, y=16..115 (w=65, h=100)
        guiGraphics.blit(TEXTURE_PERSONAJE, x + 15, y + 16, 0, 0, 65, 100, 73, 100);

        // Left-Bottom Panel (Slots compartment): x=15..79, y=120..191 (w=65, h=72)
        guiGraphics.fill(x + 15, y + 120, x + 79, y + 191, 0xFF242424);

        // Center Panel (Skill Tree backdrop): x=90..257, y=17..191 (w=168, h=175)
        guiGraphics.blit(TEXTURE_SKILLS, x + 90, y + 17, 0, 0, 168, 175, 175, 180);

        // Right-Top Panel (Stats backdrop): x=269..333, y=17..100 (w=65, h=84)
        guiGraphics.blit(TEXTURE_STATS, x + 269, y + 17, 0, 0, 65, 84, 79, 85);

        // Right-Bottom Panel (Action & Status area): x=269..333, y=105..191 (w=65, h=86)
        guiGraphics.fill(x + 269, y + 105, x + 333, y + 191, 0xFF242424);

        // 2. Draw Slot Outlines in the bottom-left compartment (2x3 grid)
        int[][] slots = { {22, 125}, {22, 145}, {22, 165}, {52, 125}, {52, 145}, {52, 165} };
        for (int[] slot : slots) {
            int sx = x + slot[0] - 1;
            int sy = y + slot[1] - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF4F4F4F);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF181818);
        }

        // Slot icons/subtitles for clarity
        guiGraphics.drawCenteredString(font, "§8CASCO", x + 30, y + 116, 0x888888);
        guiGraphics.drawCenteredString(font, "§8ARMS", x + 30, y + 184, 0x888888);
        guiGraphics.drawCenteredString(font, "§8BELT", x + 61, y + 184, 0x888888);

        // 3. Render 3D Player Avatar Preview in the top-left compartment
        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics, x + 18, y + 18, x + 76, y + 112, 38, 0.0f, pMouseX, pMouseY, this.minecraft.player
            );
        }

        // 4. Render Form Selection Buttons on the Top Bar
        int currentForm = (this.minecraft != null && this.minecraft.player != null)
                ? this.minecraft.player.getPersistentData().getInt("krm_revo:form") : 0;

        boolean isGrowingHovered = pMouseX >= x + 111 && pMouseX < x + 111 + 34 && pMouseY >= y + 3 && pMouseY < y + 3 + 13;
        boolean isMightyHovered = pMouseX >= x + 145 && pMouseX < x + 145 + 26 && pMouseY >= y + 3 && pMouseY < y + 3 + 14;

        ResourceLocation growingTex = (isGrowingHovered || currentForm == 1) ? TEXTURE_GROWING_HOVER : TEXTURE_GROWING_IDLE;
        ResourceLocation mightyTex = (isMightyHovered || currentForm == 0) ? TEXTURE_MIGHTY_HOVER : TEXTURE_MIGHTY_IDLE;

        guiGraphics.blit(growingTex, x + 111, y + 3, 0, 0, 34, 13, 34, 13);
        guiGraphics.blit(mightyTex, x + 145, y + 3, 0, 0, 26, 14, 26, 14);

        // Active indicator ring around the selected button
        if (currentForm == 1) {
            guiGraphics.fill(x + 111, y + 14, x + 111 + 34, y + 15, 0xFFFFFFFF);
        } else {
            guiGraphics.fill(x + 145, y + 15, x + 145 + 26, y + 16, 0xFFFF3333);
        }

        // 5. Render Central Panel (Skill Tree & Abilities)
        if (currentForm == 1) {
            guiGraphics.drawCenteredString(font, "§f§lKUUGA — GROWING", x + 174, y + 23, 0xFFFFFF);
            guiGraphics.drawCenteredString(font, "§7[ Forma Blanca Inicial ]", x + 174, y + 34, 0xAAAAAA);
        } else {
            guiGraphics.drawCenteredString(font, "§c§lKUUGA — MIGHTY", x + 174, y + 23, 0xFF4444);
            guiGraphics.drawCenteredString(font, "§6[ Dominio de Fuego / Rojo ]", x + 174, y + 34, 0xFFAA00);
        }

        // Ability Card 1: Rider Kick
        renderSkillCard(guiGraphics, x + 97, y + 48, 154, 35,
                "§e⚡ Rider Kick §7[Tecla V]",
                "Salto de altitud y remate explosivo.",
                "§aDesbloqueado §7| Enfriamiento: 10s");

        // Ability Card 2: Rider Melee Combos
        renderSkillCard(guiGraphics, x + 97, y + 88, 154, 35,
                "§c🥊 Combos Unarmed §7[Sin arma]",
                "Cadena de 5 ataques físicos y patadas.",
                currentForm == 1 ? "§7Daño: §f6.0 §7| Empuje moderado" : "§7Daño: §c12.0 §7| Impacto ígneo");

        // Ability Card 3: Arcle Power
        renderSkillCard(guiGraphics, x + 97, y + 128, 154, 35,
                "§b🛡 Poder de Arcle §7[Pasiva]",
                "Inmunidad a daño de caída en patadas.",
                "§aActivo con Cinturón");

        // Center Footer Stats
        guiGraphics.drawString(font, "§a★ SP: §f5", x + 99, y + 172, 0xFFFFFF);
        String levelStr = "§eNivel Kuuga: §f1";
        guiGraphics.drawString(font, levelStr, x + 250 - font.width(levelStr), y + 172, 0xFFFFFF);

        // 6. Render Right-Top Panel (Stats)
        guiGraphics.drawCenteredString(font, "§6§lSTATS", x + 301, y + 22, 0xFFFF55);
        guiGraphics.drawString(font, "§c❤ HP: §f100", x + 274, y + 37, 0xFFFFFF);
        guiGraphics.drawString(font, currentForm == 1 ? "§e⚔ ATK: §f6" : "§e⚔ ATK: §f12", x + 274, y + 51, 0xFFFFFF);
        guiGraphics.drawString(font, "§9🛡 DEF: §f15", x + 274, y + 65, 0xFFFFFF);
        guiGraphics.drawString(font, "§a⚡ SPD: §f+20%", x + 274, y + 79, 0xFFFFFF);

        // 7. Render Right-Bottom Panel (Status)
        guiGraphics.drawCenteredString(font, "§e§lESTADO", x + 301, y + 112, 0xFFFFAA);
        boolean isTransformed = this.minecraft != null && this.minecraft.player != null
                && TransformationHelper.isTransformed(this.minecraft.player);
        if (isTransformed) {
            guiGraphics.drawCenteredString(font, "§aTRANSFORMADO", x + 301, y + 126, 0x55FF55);
            guiGraphics.drawCenteredString(font, currentForm == 1 ? "§fGROWING" : "§cMIGHTY", x + 301, y + 138, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(font, "§7HUMANO CIVIL", x + 301, y + 126, 0xAAAAAA);
            guiGraphics.drawCenteredString(font, "§8Presiona K", x + 301, y + 138, 0x888888);
        }

        // 8. FINALLY, Blit the metallic frame MARCO.PNG on top of all panels
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE_MARCO, x, y, 0, 0, 349, 205, 349, 205);
        RenderSystem.disableBlend();
    }

    private void renderSkillCard(GuiGraphics g, int cx, int cy, int cw, int ch, String title, String desc, String footer) {
        g.fill(cx, cy, cx + cw, cy + ch, 0xFF1C1C1C);
        g.fill(cx, cy, cx + cw, cy + 1, 0xFF383838);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0xFF101010);
        g.fill(cx, cy, cx + 1, cy + ch, 0xFF383838);
        g.fill(cx + cw - 1, cy, cx + cw, cy + ch, 0xFF101010);

        g.drawString(font, title, cx + 5, cy + 4, 0xFFFFFF);
        g.drawString(font, "§7" + desc, cx + 5, cy + 14, 0xAAAAAA);
        g.drawString(font, footer, cx + 5, cy + 24, 0x888888);
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

        // Tooltips for Form Selection Tab Buttons
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (mouseX >= x + 111 && mouseX < x + 111 + 34 && mouseY >= y + 3 && mouseY < y + 3 + 13) {
            guiGraphics.renderTooltip(font, Component.literal("Forma Growing (Blanco)"), mouseX, mouseY);
        } else if (mouseX >= x + 145 && mouseX < x + 145 + 26 && mouseY >= y + 3 && mouseY < y + 3 + 14) {
            guiGraphics.renderTooltip(font, Component.literal("Forma Mighty (Rojo)"), mouseX, mouseY);
        }
    }
}
