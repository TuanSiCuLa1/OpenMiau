package myau.util.notification;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.util.font.Font;
import myau.util.font.Fonts;
import myau.util.font.Weight;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public static void show(String title, String description, NotificationType type) {
        notifications.add(new Notification(title, description, 2500f, type));
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();

        float currentY = screenHeight - 20;

        Font titleFont = Fonts.MAIN.get(16, Weight.SEMI_BOLD);
        Font descFont = Fonts.MAIN.get(14, Weight.NONE);

        for (Notification notif : notifications) {
            long currentTime = System.currentTimeMillis();
            float delta = (currentTime - notif.lastTime);
            notif.lastTime = currentTime;

            if (notif.timer > 0) {
                notif.timer -= delta;
                notif.alpha = Math.min(255f, notif.alpha + delta * 1.5f); 
            } else {
                notif.alpha -= delta * 1.5f; 
            }

            if (notif.alpha <= 0) {
                notifications.remove(notif);
                continue;
            }

            float titleWidth = titleFont.getStringWidth(notif.title);
            float descWidth = descFont.getStringWidth(notif.description);
            float width = Math.max(titleWidth, descWidth) + 20;
            float height = 28;

            if (notif.timer > 0) {
                notif.targetX = screenWidth - width - 10;
                notif.targetY = currentY - height;
                currentY -= (height + 5); 
            } else {
                notif.targetX = screenWidth - width - 10;
                notif.targetY = screenHeight + 10;
            }

            if (notif.x == 0 && notif.y == 0) {
                notif.x = screenWidth + 50;
                notif.y = notif.targetY;
            }

            float speed = Math.min(1.0f, delta * 0.015f);
            notif.x += (notif.targetX - notif.x) * speed;
            notif.y += (notif.targetY - notif.y) * speed;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            int alphaInt = (int) Math.max(0, Math.min(255, notif.alpha));
            int bgAlpha = (int) (alphaInt * (140f / 255f));
            int bgColor = new Color(0, 0, 0, bgAlpha).getRGB();

            RoundedUtils.drawRound(notif.x, notif.y, width, height, 4f, bgColor);

            float timeRatio = Math.max(0, notif.timer / notif.maxTime);
            float barWidth = width * timeRatio;
            int barColor = new Color(
                    (notif.type.getColor() >> 16) & 0xFF,
                    (notif.type.getColor() >> 8) & 0xFF,
                    notif.type.getColor() & 0xFF,
                    alphaInt
            ).getRGB();

            RoundedUtils.drawRound(notif.x, notif.y + height - 1.5f, barWidth, 1.5f, 1f, barColor);

            GlStateManager.enableTexture2D(); 

            int titleColor = new Color(255, 255, 255, alphaInt).getRGB();
            int descColor = new Color(170, 170, 170, alphaInt).getRGB();

            titleFont.drawWithShadow(notif.title, notif.x + 8, notif.y + 4, titleColor);
            descFont.drawWithShadow(notif.description, notif.x + 8, notif.y + 15, descColor);

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}




