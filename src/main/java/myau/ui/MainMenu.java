package myau.ui;

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

public class MainMenu extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String HEART = "\u2764";
    private static final int BUTTON_WIDTH = 178;
    private static final int BUTTON_HEIGHT = 34;

    private long openedAt;

    @Override
    public void initGui() {
        this.openedAt = System.currentTimeMillis();
        this.buttonList.clear();

        int panelW = Math.min(520, this.width - 36);
        int panelH = 214;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;
        int buttonX = panelX + panelW - BUTTON_WIDTH - 30;
        int buttonY = panelY + 42;

        this.buttonList.add(new MenuButton(0, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, "Singleplayer", "Play your worlds"));
        this.buttonList.add(new MenuButton(1, buttonX, buttonY + 43, BUTTON_WIDTH, BUTTON_HEIGHT, "Multiplayer", "Join servers"));
        this.buttonList.add(new MenuButton(2, buttonX, buttonY + 86, BUTTON_WIDTH, BUTTON_HEIGHT, "Settings", "Configure Minecraft"));
        this.buttonList.add(new MenuButton(3, buttonX, buttonY + 129, BUTTON_WIDTH, BUTTON_HEIGHT, "Exit", "Close Miau"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float time = (System.currentTimeMillis() - this.openedAt) / 1000.0F;
        this.drawScene(time, mouseX, mouseY);
        this.drawGlassPanel(time);
        this.drawHeroAccents(time);
        this.drawBranding(time);
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawFooter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
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
        this.fontRendererObj.drawStringWithShadow("discord.gg/ssFYeKx3Yb", leftX + 14, panelY + panelH - 45, 0xBFD6E7FF);

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
        this.fontRendererObj.drawStringWithShadow(made, 5, this.height - 13, 0xEFFFFFFF);
        this.fontRendererObj.drawStringWithShadow(HEART, 5 + this.fontRendererObj.getStringWidth("Made with "), this.height - 13, 0xFFFF4D6D);
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
        private final String description;
        private float hoverProgress;

        private MenuButton(int id, int x, int y, int width, int height, String text, String description) {
            super(id, x, y, width, height, text);
            this.description = description;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) return;
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            this.hoverProgress += ((this.hovered ? 1.0F : 0.0F) - this.hoverProgress) * 0.18F;

            int fill = blend(new Color(255, 255, 255, 24), new Color(90, 155, 255, 82), this.hoverProgress).getRGB();
            int stroke = blend(new Color(255, 255, 255, 54), new Color(167, 202, 255, 220), this.hoverProgress).getRGB();
            int glow = blend(new Color(0, 0, 0, 0), new Color(80, 145, 255, 48), this.hoverProgress).getRGB();

            if (this.hoverProgress > 0.02F) {
                drawRoundedRect(this.xPosition - 5, this.yPosition - 4, this.width + 10, this.height + 8, 17.0F, glow);
            }
            drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 14.0F, fill);
            drawRoundedOutline(this.xPosition, this.yPosition, this.width, this.height, 14.0F, stroke);

            int textColor = blend(new Color(232, 240, 255), Color.WHITE, this.hoverProgress).getRGB();
            mc.fontRendererObj.drawStringWithShadow(this.displayString, this.xPosition + 16, this.yPosition + 7, textColor);
            mc.fontRendererObj.drawStringWithShadow(this.description, this.xPosition + 16, this.yPosition + 18, 0x92FFFFFF);
            mc.fontRendererObj.drawStringWithShadow("›", this.xPosition + this.width - 18, this.yPosition + 10, 0xEFFFFFFF);
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
