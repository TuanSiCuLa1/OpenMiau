package myau.management;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.modules.render.HUD;
import myau.module.modules.render.Scoreboard;
import myau.module.modules.render.TargetHUD;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.Color;

public class DragManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasMouseDown = false;
    private boolean draggingTargetHUD = false;
    private boolean draggingScoreboard = false;
    private boolean draggingNotification = false;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!(mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof myau.ui.clickgui.ClickGui)) {
            wasMouseDown = false;
            draggingTargetHUD = false;
            draggingScoreboard = false;
            draggingNotification = false;
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean justClicked = isMouseDown && !wasMouseDown;
        wasMouseDown = isMouseDown;

        if (!isMouseDown) {
            draggingTargetHUD = false;
            draggingScoreboard = false;
            draggingNotification = false;
        }

        TargetHUD targetHUD = (TargetHUD) Myau.moduleManager.getModule(TargetHUD.class);
        Scoreboard scoreboard = (Scoreboard) Myau.moduleManager.getModule(Scoreboard.class);
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);

        // 1. TargetHUD Dragging
        if (targetHUD != null && targetHUD.isEnabled()) {
            TargetHUD.TargetHUDBounds bounds = targetHUD.getBounds(sr);
            // Draw visual editor box
            RenderUtil.enableRenderState();
            RenderUtil.drawOutlineRect(
                    bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                    1.5f, new Color(0, 0, 0, 80).getRGB(), new Color(255, 255, 255, 180).getRGB()
            );
            RenderUtil.disableRenderState();
            mc.fontRendererObj.drawStringWithShadow("TargetHUD", bounds.x + 2, bounds.y + 2, 0xFFFFFFFF);

            if (justClicked && mouseX >= bounds.x && mouseX <= bounds.x + bounds.width && mouseY >= bounds.y && mouseY <= bounds.y + bounds.height) {
                draggingTargetHUD = true;
                dragOffsetX = targetHUD.offX.getValue() - mouseX;
                dragOffsetY = targetHUD.offY.getValue() - mouseY;
            }

            if (draggingTargetHUD) {
                targetHUD.offX.setValue((int) (mouseX + dragOffsetX));
                targetHUD.offY.setValue((int) (mouseY + dragOffsetY));
            }
        }

        // 2. Scoreboard Dragging
        if (scoreboard != null && scoreboard.isEnabled()) {
            Scoreboard.ScoreboardBounds bounds = scoreboard.getBounds(sr);
            // Check if there is no active scoreboard in the world, render dummy
            boolean hasScoreboard = false;
            if (mc.theWorld != null && mc.theWorld.getScoreboard() != null && mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1) != null) {
                hasScoreboard = true;
            }

            RenderUtil.enableRenderState();
            if (!hasScoreboard) {
                // Render dummy scoreboard container so player can see it
                RenderUtil.drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, new Color(0, 0, 0, 80).getRGB());
            }
            RenderUtil.drawOutlineRect(
                    bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                    1.5f, new Color(0, 0, 0, 40).getRGB(), new Color(255, 255, 255, 180).getRGB()
            );
            RenderUtil.disableRenderState();
            mc.fontRendererObj.drawStringWithShadow("Scoreboard", bounds.x + 2, bounds.y + 2, 0xFFFFFFFF);

            if (justClicked && mouseX >= bounds.x && mouseX <= bounds.x + bounds.width && mouseY >= bounds.y && mouseY <= bounds.y + bounds.height) {
                draggingScoreboard = true;
                dragOffsetX = scoreboard.offX.getValue() - mouseX;
                dragOffsetY = scoreboard.offY.getValue() - mouseY;
            }

            if (draggingScoreboard) {
                scoreboard.offX.setValue((int) (mouseX + dragOffsetX));
                scoreboard.offY.setValue((int) (mouseY + dragOffsetY));
            }
        }

        // 3. Notification Dragging
        if (hud != null) {
            float width = 120;
            float height = 28;
            float notifX = sr.getScaledWidth() - width - 10 + hud.notifX.getValue();
            float notifY = sr.getScaledHeight() - 20 - height + hud.notifY.getValue();

            RenderUtil.enableRenderState();
            RenderUtil.drawRect(notifX, notifY, notifX + width, notifY + height, new Color(0, 0, 0, 80).getRGB());
            RenderUtil.drawOutlineRect(
                    notifX, notifY, notifX + width, notifY + height,
                    1.5f, new Color(0, 0, 0, 40).getRGB(), new Color(255, 255, 255, 180).getRGB()
            );
            RenderUtil.disableRenderState();
            mc.fontRendererObj.drawStringWithShadow("Notifications", notifX + 2, notifY + 2, 0xFFFFFFFF);

            if (justClicked && mouseX >= notifX && mouseX <= notifX + width && mouseY >= notifY && mouseY <= notifY + height) {
                draggingNotification = true;
                dragOffsetX = hud.notifX.getValue() - mouseX;
                dragOffsetY = hud.notifY.getValue() - mouseY;
            }

            if (draggingNotification) {
                hud.notifX.setValue((int) (mouseX + dragOffsetX));
                hud.notifY.setValue((int) (mouseY + dragOffsetY));
            }
        }
    }
}
