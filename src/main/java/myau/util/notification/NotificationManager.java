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

        // Bắt đầu vẽ từ dưới lên (cách đáy màn hình 20px)
        float currentY = screenHeight - 20;

        // Load custom fonts từ OpenMiau client
        Font titleFont = Fonts.MAIN.get(16, Weight.SEMI_BOLD);
        Font descFont = Fonts.MAIN.get(14, Weight.NONE);

        float hudOffsetX = 0;
        float hudOffsetY = 0;
        if (myau.Myau.moduleManager != null) {
            myau.module.modules.render.HUD hud = (myau.module.modules.render.HUD) myau.Myau.moduleManager.getModule(myau.module.modules.render.HUD.class);
            if (hud != null) {
                hudOffsetX = hud.notifX.getValue();
                hudOffsetY = hud.notifY.getValue();
            }
        }

        for (Notification notif : notifications) {
            long currentTime = System.currentTimeMillis();
            float delta = (currentTime - notif.lastTime);
            notif.lastTime = currentTime;

            // Xử lý đếm ngược thời gian và hiệu ứng làm mờ (Fade)
            if (notif.timer > 0) {
                notif.timer -= delta;
                notif.alpha = Math.min(255f, notif.alpha + delta * 1.5f); // Fade in mượt mà
            } else {
                notif.alpha -= delta * 1.5f; // Fade out khi hết thời gian
            }

            if (notif.alpha <= 0) {
                notifications.remove(notif);
                continue;
            }

            // Tính toán chiều rộng dựa trên văn bản
            float titleWidth = titleFont.getStringWidth(notif.title);
            float descWidth = descFont.getStringWidth(notif.description);
            float width = Math.max(titleWidth, descWidth) + 20;
            float height = 28;

            // Đặt tọa độ mục tiêu (Target Position)
            if (notif.timer > 0) {
                notif.targetX = screenWidth - width - 10 + hudOffsetX;
                notif.targetY = currentY - height + hudOffsetY;
                currentY -= (height + 5); // Đẩy thông báo tiếp theo lên trên (cách 5px)
            } else {
                notif.targetX = screenWidth - width - 10 + hudOffsetX;
                notif.targetY = screenHeight + 10 + hudOffsetY; // Trượt xuống dưới khỏi màn hình khi tắt
            }

            // Trượt từ bên phải màn hình vào khi mới xuất hiện
            if (notif.x == 0 && notif.y == 0) {
                notif.x = screenWidth + 50;
                notif.y = notif.targetY;
            }

            // Nội suy Ease-out động dùng Delta Time
            float speed = Math.min(1.0f, delta * 0.015f);
            notif.x += (notif.targetX - notif.x) * speed;
            notif.y += (notif.targetY - notif.y) * speed;

            // Render khung nền và thanh thời gian
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            int alphaInt = (int) Math.max(0, Math.min(255, notif.alpha));
            int bgAlpha = (int) (alphaInt * (140f / 255f));
            int bgColor = new Color(0, 0, 0, bgAlpha).getRGB();

            // 1. Vẽ khung nền trong suốt bo góc 4px
            RoundedUtils.drawRound(notif.x, notif.y, width, height, 4f, bgColor);

            // 2. Vẽ thanh chạy thời gian (1.5px) ở đáy, rút dần từ phải sang trái
            float timeRatio = Math.max(0, notif.timer / notif.maxTime);
            float barWidth = width * timeRatio;
            int barColor = new Color(
                    (notif.type.getColor() >> 16) & 0xFF,
                    (notif.type.getColor() >> 8) & 0xFF,
                    notif.type.getColor() & 0xFF,
                    alphaInt
            ).getRGB();

            RoundedUtils.drawRound(notif.x, notif.y + height - 1.5f, barWidth, 1.5f, 1f, barColor);

            GlStateManager.enableTexture2D(); // Bật lại Texture2D để hiển thị chữ

            // 3. Vẽ văn bản Title và Description
            int titleColor = new Color(255, 255, 255, alphaInt).getRGB();
            int descColor = new Color(170, 170, 170, alphaInt).getRGB();

            titleFont.drawWithShadow(notif.title, notif.x + 8, notif.y + 4, titleColor);
            descFont.drawWithShadow(notif.description, notif.x + 8, notif.y + 15, descColor);

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
