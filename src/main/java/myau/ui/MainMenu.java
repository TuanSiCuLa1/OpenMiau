package myau.ui;

import me.ksyz.accountmanager.utils.SystemUtils;
import myau.ClientInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;

public class MainMenu extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String HEART = "\u2764";
    private static final String DISCORD_TEXT = "Click to join our Discord server.";
    private static final String DISCORD_URL = "https://discord.gg/PKxAz6wbXb";
    private static final int MENU_BUTTON_WIDTH = 140;
    private static final int MENU_BUTTON_HEIGHT = 25;
    private static final int DISCORD_BUTTON_ID = 10;
    private static final int OK_BUTTON_ID = 11;

    private long openedAt;
    private boolean showingWelcome;
    private float discordHover;

    @Override
    public void initGui() {
        this.openedAt = System.currentTimeMillis();
        this.showingWelcome = false;
        this.buttonList.clear();

        int buttonX = this.width / 2 - MENU_BUTTON_WIDTH / 2;
        int buttonY = (int) (this.height / 2.0F - MENU_BUTTON_HEIGHT / 2.0F - 25.0F);
        this.buttonList.add(new MenuButton(0, buttonX, buttonY, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, "Singleplayer"));
        this.buttonList.add(new MenuButton(1, buttonX, buttonY + 30, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, "Multiplayer"));
        this.buttonList.add(new MenuButton(2, buttonX, buttonY + 60, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, "Settings"));
        this.buttonList.add(new MenuButton(3, buttonX, buttonY + 90, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, "Exit"));

        int dialogW = Math.min(360, this.width - 42);
        int dialogH = 154;
        int dialogX = this.width / 2 - dialogW / 2;
        int dialogY = this.height / 2 - dialogH / 2;
        int dialogButtonW = (dialogW - 50) / 2;
        int dialogButtonY = dialogY + dialogH - 45;
        this.buttonList.add(new MenuButton(DISCORD_BUTTON_ID, dialogX + 18, dialogButtonY, dialogButtonW, 30, "Discord"));
        this.buttonList.add(new MenuButton(OK_BUTTON_ID, dialogX + 32 + dialogButtonW, dialogButtonY, dialogButtonW, 30, "OK"));
        this.updateWelcomeButtons();
    }
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float time = (System.currentTimeMillis() - this.openedAt) / 1000.0F;
        this.updateHover(mouseX, mouseY);
        this.drawLettuceBackground(time, mouseX, mouseY);
        this.drawLettuceHeader(time);
        this.drawDiscordChip();
        this.drawFooter();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!this.showingWelcome && mouseButton == 0) {
            float discordWidth = 25.0F + this.discordHover * (this.fontRendererObj.getStringWidth(DISCORD_TEXT) + 10.0F);
            if (isHovering(8.0F, 8.0F, discordWidth, 25.0F, mouseX, mouseY)) {
                SystemUtils.openWebLink(URI.create(DISCORD_URL));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (this.showingWelcome) {
            if (button.id == DISCORD_BUTTON_ID) {
                SystemUtils.openWebLink(URI.create(DISCORD_URL));
                this.showingWelcome = false;
                this.updateWelcomeButtons();
            } else if (button.id == OK_BUTTON_ID) {
                this.showingWelcome = false;
                this.updateWelcomeButtons();
            }
            return;
        }

        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 1:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 2:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 3:
                mc.shutdown();
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateHover(int mouseX, int mouseY) {
        float discordWidth = 25.0F + this.discordHover * (this.fontRendererObj.getStringWidth(DISCORD_TEXT) + 10.0F);
        float discordTarget = isHovering(8.0F, 8.0F, discordWidth, 25.0F, mouseX, mouseY) ? 1.0F : 0.0F;
        this.discordHover += (discordTarget - this.discordHover) * 0.16F;
    }

    private void drawLettuceBackground(float time, int mouseX, int mouseY) {
        drawGradientRect(0, 0, this.width, this.height, 0xFF07080D, 0xFF11131B);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float parallaxX = (mouseX - this.width / 2.0F) * 0.01F;
        float parallaxY = (mouseY - this.height / 2.0F) * 0.008F;
        drawBlob(this.width * 0.25F + parallaxX + sin(time, 0.35F, 18.0F), this.height * 0.18F + parallaxY, 180.0F, new Color(106, 181, 253, 34));
        drawBlob(this.width * 0.78F - parallaxX + cos(time, 0.28F, 22.0F), this.height * 0.72F - parallaxY, 220.0F, new Color(111, 119, 253, 30));
        drawVignette();
        drawSubtleGrid(time);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawLettuceHeader(float time) {
        String title = "Miau";
        float scale = Math.max(4.0F, Math.min(5.2F, this.width / 150.0F));
        int textWidth = this.fontRendererObj.getStringWidth(title);
        int titleColor = blend(new Color(106, 181, 253), new Color(111, 119, 253), sin01(time * 1.4F)).getRGB();
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2.0F - textWidth * scale / 2.0F, this.height / 2.0F - 80.0F, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        this.fontRendererObj.drawStringWithShadow(title, 0, 0, titleColor);
        GlStateManager.popMatrix();
    }

    private void drawDiscordChip() {
        float width = 25.0F + this.discordHover * (this.fontRendererObj.getStringWidth(DISCORD_TEXT) + 10.0F);
        Color fill = blend(new Color(0, 0, 0, 100), new Color(0, 0, 0, 150), this.discordHover);
        drawRoundedRect(8, 8, width, 25, 12.0F, fill.getRGB());
        drawRoundedOutline(8, 8, width, 25, 12.0F, 0x20202020);
        this.fontRendererObj.drawStringWithShadow("D", 17, 16, 0xFFFFFFFF);
        if (this.discordHover > 0.05F) {
            drawScissoredText(DISCORD_TEXT, 35, 17, 0xFFFFFFFF, 35, 8, width - 35, 25);
        }
    }


    private void drawScene(float time, int mouseX, int mouseY) {
        drawGradientRect(0, 0, this.width, this.height, 0xFF070A18, 0xFF15112A);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        float parallaxX = (mouseX - this.width / 2.0F) * 0.018F;
        float parallaxY = (mouseY - this.height / 2.0F) * 0.014F;
        drawBlob(this.width * 0.20F + parallaxX + sin(time, 0.55F, 30.0F), this.height * 0.18F + parallaxY, 190.0F, new Color(35, 132, 255, 86));
        drawBlob(this.width * 0.82F - parallaxX + cos(time, 0.45F, 36.0F), this.height * 0.24F - parallaxY, 220.0F, new Color(172, 84, 255, 82));
        drawBlob(this.width * 0.52F + sin(time, 0.28F, 60.0F), this.height * 0.92F, 250.0F, new Color(255, 71, 145, 64));
        drawDiagonalBeam(time);
        drawGrid(time);
        drawParticles(time, mouseX, mouseY);

        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        drawGradientRect(0, 0, this.width, this.height / 3, 0x99000000, 0x00000000);
        drawGradientRect(0, this.height * 2 / 3, this.width, this.height, 0x00000000, 0xBB000000);
    }

    private void drawGlassPanel(float time) {
        int panelW = Math.min(560, this.width - 36);
        int panelH = 238;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;

        drawRoundedRect(panelX + 9, panelY + 11, panelW, panelH, 26.0F, 0x62000000);
        drawRoundedRect(panelX, panelY, panelW, panelH, 26.0F, 0x68101428);
        drawRoundedOutline(panelX, panelY, panelW, panelH, 26.0F, 0x80FFFFFF);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        color(0x22FFFFFF);
        GL11.glVertex2f(panelX + 1, panelY + 1);
        color(0x07FFFFFF);
        GL11.glVertex2f(panelX + panelW - 1, panelY + 1);
        color(0x00192A5C);
        GL11.glVertex2f(panelX + panelW - 1, panelY + panelH - 1);
        color(0x14192A5C);
        GL11.glVertex2f(panelX + 1, panelY + panelH - 1);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        int orbX = panelX + 74 + (int) sin(time, 0.75F, 7.0F);
        int orbY = panelY + 142 + (int) cos(time, 0.58F, 5.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        drawBlob(orbX, orbY, 58.0F, new Color(90, 170, 255, 54));
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawHeroAccents(float time) {
        int panelW = Math.min(560, this.width - 36);
        int panelH = 238;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;
        int leftX = panelX + 34;
        int topY = panelY + 36;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawBlob(leftX + 58 + sin(time, 0.8F, 4.0F), topY + 74 + cos(time, 0.7F, 3.0F), 78.0F, new Color(82, 145, 255, 54));
        drawBlob(leftX + 118 + cos(time, 0.55F, 5.0F), topY + 30, 56.0F, new Color(218, 112, 255, 45));
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        drawRoundedRect(leftX, panelY + panelH - 66, 204, 36, 18.0F, 0x20000000);
        drawRoundedOutline(leftX, panelY + panelH - 66, 204, 36, 18.0F, 0x44FFFFFF);
        this.fontRendererObj.drawStringWithShadow("Discord", leftX + 14, panelY + panelH - 57, 0xFFFFFFFF);
        this.fontRendererObj.drawStringWithShadow("discord.gg/PKxAz6wbXb", leftX + 14, panelY + panelH - 45, 0xBFD6E7FF);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINES);
        color(0x38FFFFFF);
        GL11.glVertex2f(panelX + panelW - 225, panelY + 28);
        GL11.glVertex2f(panelX + panelW - 225, panelY + panelH - 28);
        GL11.glEnd();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawBranding(float time) {
        int panelW = Math.min(560, this.width - 36);
        int panelH = 238;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;
        int leftX = panelX + 34;
        int topY = panelY + 38;

        String title = "Miau";
        GlStateManager.pushMatrix();
        GlStateManager.translate(leftX, topY + 20, 0.0F);
        GlStateManager.scale(2.85F, 2.85F, 1.0F);
        int titleColor = blend(new Color(107, 181, 255), new Color(219, 118, 255), sin01(time * 1.25F)).getRGB();
        this.fontRendererObj.drawStringWithShadow(title, 0, 0, titleColor);
        GlStateManager.popMatrix();

        this.fontRendererObj.drawStringWithShadow(ClientInfo.getDisplayVersion(), leftX + 2, topY + 76, 0xE6F3FAFF);
    }

    private void drawFooter() {
        String made = "Made with     by ksyz, Miau Project, idle.";
        this.fontRendererObj.drawStringWithShadow(made, 1, this.height - 10, 0xFFFFFFFF);
        this.fontRendererObj.drawStringWithShadow(HEART, 1 + this.fontRendererObj.getStringWidth("Made with "), this.height - 10, 0xFFFF0000);
        String version = ClientInfo.getDisplayVersion();
        this.fontRendererObj.drawStringWithShadow(version, this.width - this.fontRendererObj.getStringWidth(version) - 2, this.height - 10, 0x99FFFFFF);
    }
    private void drawWelcomeDialog() {
        drawRect(0, 0, this.width, this.height, 0xA8000000);

        int dialogW = Math.min(360, this.width - 42);
        int dialogH = 154;
        int dialogX = this.width / 2 - dialogW / 2;
        int dialogY = this.height / 2 - dialogH / 2;

        drawRoundedRect(dialogX + 7, dialogY + 9, dialogW, dialogH, 18.0F, 0x72000000);
        drawRoundedRect(dialogX, dialogY, dialogW, dialogH, 18.0F, 0xEE101628);
        drawRoundedOutline(dialogX, dialogY, dialogW, dialogH, 18.0F, 0x95B7D9FF);

        this.fontRendererObj.drawStringWithShadow("Welcome to Miau Client", dialogX + 20, dialogY + 22, 0xFFFFFFFF);
        this.fontRendererObj.drawStringWithShadow("Cam on ban da su dung client.", dialogX + 20, dialogY + 46, 0xDDEAF4FF);
        this.fontRendererObj.drawStringWithShadow("Tham gia Discord de nhan thong bao va ho tro.", dialogX + 20, dialogY + 60, 0xBFD6E7FF);
        this.fontRendererObj.drawStringWithShadow("discord.gg/PKxAz6wbXb", dialogX + 20, dialogY + 78, 0xFF86B9FF);
    }

    private void updateWelcomeButtons() {
        for (GuiButton button : this.buttonList) {
            boolean welcomeButton = button.id == DISCORD_BUTTON_ID || button.id == OK_BUTTON_ID;
            button.visible = this.showingWelcome == welcomeButton;
            button.enabled = button.visible;
        }
    }

    private void drawDiagonalBeam(float time) {
        GL11.glBegin(GL11.GL_QUADS);
        color(0x00258BFF);
        GL11.glVertex2f(this.width * 0.10F, 0);
        color(0x33258BFF);
        GL11.glVertex2f(this.width * 0.34F + sin(time, 0.35F, 30.0F), 0);
        color(0x004C1D95);
        GL11.glVertex2f(this.width * 0.74F, this.height);
        color(0x224C1D95);
        GL11.glVertex2f(this.width * 0.50F + sin(time, 0.35F, 30.0F), this.height);
        GL11.glEnd();
    }

    private void drawGrid(float time) {
        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINES);
        color(0x12FFFFFF);
        int spacing = 28;
        int offset = (int) ((time * 10.0F) % spacing);
        for (int x = -spacing + offset; x < this.width + spacing; x += spacing) {
            GL11.glVertex2f(x, this.height * 0.58F);
            GL11.glVertex2f(x + 60, this.height);
        }
        for (int y = (int) (this.height * 0.58F); y < this.height; y += spacing) {
            GL11.glVertex2f(0, y);
            GL11.glVertex2f(this.width, y);
        }
        GL11.glEnd();
    }

    private void drawParticles(float time, int mouseX, int mouseY) {
        GL11.glPointSize(1.7F);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < 95; i++) {
            float x = (i * 67 % Math.max(this.width, 1)) + (mouseX - this.width / 2.0F) * 0.006F;
            float y = (i * 43 % Math.max(this.height, 1)) + (mouseY - this.height / 2.0F) * 0.005F;
            float alpha = 0.16F + 0.34F * sin01(time + i * 0.31F);
            GL11.glColor4f(0.82F, 0.91F, 1.0F, alpha);
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();
    }

    private void drawVignette() {
        drawGradientRect(0, 0, this.width, this.height / 4, 0xAA000000, 0x00000000);
        drawGradientRect(0, this.height * 3 / 4, this.width, this.height, 0x00000000, 0xAA000000);
        drawRect(0, 0, 18, this.height, 0x2A000000);
        drawRect(this.width - 18, 0, this.width, this.height, 0x2A000000);
    }

    private void drawSubtleGrid(float time) {
        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINES);
        color(0x09FFFFFF);
        int spacing = 34;
        int offset = (int) ((time * 7.0F) % spacing);
        for (int x = -spacing + offset; x < this.width + spacing; x += spacing) {
            GL11.glVertex2f(x, 0);
            GL11.glVertex2f(x + 80, this.height);
        }
        for (int y = -spacing + offset; y < this.height + spacing; y += spacing) {
            GL11.glVertex2f(0, y);
            GL11.glVertex2f(this.width, y + 26);
        }
        GL11.glEnd();
    }

    private void drawScissoredText(String text, int x, int y, int color, float clipX, float clipY, float clipWidth, float clipHeight) {
        int scaleFactor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (clipX * scaleFactor), (int) ((this.height - clipY - clipHeight) * scaleFactor), (int) (clipWidth * scaleFactor), (int) (clipHeight * scaleFactor));
        this.fontRendererObj.drawStringWithShadow(text, x, y, color);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        this.fontRendererObj.drawStringWithShadow(text, x - this.fontRendererObj.getStringWidth(text) / 2.0F, y, color);
    }

    private static boolean isHovering(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    private static void drawBlob(float centerX, float centerY, float radius, Color color) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        GL11.glVertex2f(centerX, centerY);
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.0F);
        for (int i = 0; i <= 72; i++) {
            double angle = Math.PI * 2.0D * i / 72.0D;
            GL11.glVertex2d(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private static float sin(float time, float speed, float scale) {
        return (float) Math.sin(time * speed) * scale;
    }

    private static float cos(float time, float speed, float scale) {
        return (float) Math.cos(time * speed) * scale;
    }

    private static float sin01(float value) {
        return (float) Math.sin(value) * 0.5F + 0.5F;
    }

    private static Color blend(Color first, Color second, float ratio) {
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        int r = (int) (first.getRed() + (second.getRed() - first.getRed()) * ratio);
        int g = (int) (first.getGreen() + (second.getGreen() - first.getGreen()) * ratio);
        int b = (int) (first.getBlue() + (second.getBlue() - first.getBlue()) * ratio);
        int a = (int) (first.getAlpha() + (second.getAlpha() - first.getAlpha()) * ratio);
        return new Color(r, g, b, a);
    }

    private static void color(int color) {
        GL11.glColor4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
    }

    private static class MenuButton extends GuiButton {
        private float hoverProgress;

        private MenuButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) return;
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            this.hoverProgress += ((this.hovered ? 1.0F : 0.0F) - this.hoverProgress) * 0.16F;

            Color rectColor = new Color(35, 37, 43, 102);
            rectColor = blend(rectColor, brighter(rectColor, 0.4F), this.hoverProgress);
            drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 12.0F, rectColor.getRGB());
            drawRoundedOutline(this.xPosition, this.yPosition, this.width, this.height, 12.0F, 0x641E1E1E);

            int textColor = blend(Color.WHITE, new Color(210, 222, 255), this.hoverProgress).getRGB();
            mc.fontRendererObj.drawStringWithShadow(this.displayString, this.xPosition + this.width / 2.0F - mc.fontRendererObj.getStringWidth(this.displayString) / 2.0F, this.yPosition + this.height / 2.0F - 4.0F, textColor);
        }

        private static Color brighter(Color color, float factor) {
            int i = (int) (1.0F / (1.0F - factor));
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            if (r == 0 && g == 0 && b == 0) return new Color(i, i, i, color.getAlpha());
            if (r > 0 && r < i) r = i;
            if (g > 0 && g < i) g = i;
            if (b > 0 && b < i) b = i;
            return new Color(Math.min((int) (r / factor), 255), Math.min((int) (g / factor), 255), Math.min((int) (b / factor), 255), color.getAlpha());
        }
    }
    private static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        drawRounded(x, y, width, height, radius, color, true);
    }

    private static void drawRoundedOutline(float x, float y, float width, float height, float radius, int color) {
        drawRounded(x, y, width, height, radius, color, false);
    }

    private static void drawRounded(float x, float y, float width, float height, float radius, int color, boolean fill) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(color);
        GL11.glLineWidth(1.3F);
        GL11.glBegin(fill ? GL11.GL_TRIANGLE_FAN : GL11.GL_LINE_LOOP);
        if (fill) {
            GL11.glVertex2f(x + width / 2.0F, y + height / 2.0F);
        }
        for (int corner = 0; corner < 4; corner++) {
            float cx = x + (corner == 1 || corner == 2 ? width - radius : radius);
            float cy = y + (corner >= 2 ? height - radius : radius);
            int start = corner * 90;
            for (int i = 0; i <= 18; i++) {
                double angle = Math.toRadians(start + i * 5);
                GL11.glVertex2d(cx + Math.sin(angle) * radius, cy - Math.cos(angle) * radius);
            }
        }
        GL11.glEnd();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}
