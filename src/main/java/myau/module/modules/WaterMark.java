package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static int x = 4;
    public static int y = 4;

    public WaterMark() {
        super("WaterMark", true, false);
    }

    public static void setPosition(int x, int y) {
        WaterMark.x = x;
        WaterMark.y = y;
    }

    private int getStringWidth(String text) {
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void drawStringWithShadow(String text, float x, float y, int color) {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        mc.fontRendererObj.drawString(text, x, y, color, hud == null || hud.shadow.getValue());
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        renderExhibition();
    }

    private void renderExhibition() {
        int fps = Minecraft.getDebugFPS();
        int ping = 0;

        if (mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.sendQueue != null && mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
            ping = mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
        }

        String firstText = "M";
        String restText = "yau Client ";
        String fpsValue = fps + "FPS";
        String pingValue = ping + "ms";

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        float x = WaterMark.x;
        float y = WaterMark.y;

        GlStateManager.pushMatrix();
        long time = System.currentTimeMillis();
        int accentColor = hud != null ? hud.getColor(time).getRGB() : 0xFFFFFFFF;
        int whiteColor = 0xFFFFFFFF;
        int grayColor = 0xFFAAAAAA;

        drawStringWithShadow(firstText, x, y, accentColor);
        float currentX = x + getStringWidth(firstText);

        drawStringWithShadow(restText, currentX, y, whiteColor);
        currentX += getStringWidth(restText);

        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(fpsValue, currentX, y, whiteColor);
        currentX += getStringWidth(fpsValue);

        drawStringWithShadow("]", currentX, y, grayColor);
        currentX += getStringWidth("] ");

        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(pingValue, currentX, y, whiteColor);
        currentX += getStringWidth(pingValue);

        drawStringWithShadow("]", currentX, y, grayColor);
        GlStateManager.popMatrix();
    }
}
