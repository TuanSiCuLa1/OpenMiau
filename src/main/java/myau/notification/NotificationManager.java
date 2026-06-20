package myau.notification;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private final List<Notification> notifications = new ArrayList<>();
    private long lastRenderTime = System.currentTimeMillis();

    public NotificationBuilder builder(NotificationType type) {
        return new NotificationBuilder(type, this);
    }

    public void add(Notification notification) {
        synchronized (notifications) {
            notifications.add(notification);
        }
        System.out.println("[Notification] " + notification.getTitle() + " - " + notification.getDescription());
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        render(sr);
    }

    public void render(ScaledResolution sr) {
        // Strict HUD Module State Dependency
        myau.module.modules.render.HUD hud = (myau.module.modules.render.HUD) myau.Myau.moduleManager.modules.get(myau.module.modules.render.HUD.class);
        if (hud == null || !hud.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        
        // Hyper-Smooth Delta-Time Physics
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000f; 
        lastRenderTime = currentTime;
        
        // Prevent huge jumps on lag spikes
        if (deltaTime > 0.1f) deltaTime = 0.1f;
        
        final float animationSpeed = 12.0f; // Smooth-Step exponential decay rate
        
        final float paddingLeft = 10f;
        final float paddingRight = 10f;
        final float iconWidth = 16f;
        final float iconHeight = 16f;
        final float spacing = 8f;
        final float containerHeight = 32f;
        final float marginBottom = 10f;
        final float gap = 5f;

        float scaledWidth = sr.getScaledWidth();
        float scaledHeight = sr.getScaledHeight();
        
        synchronized (notifications) {
            int activeIndex = 0;
            Iterator<Notification> iterator = notifications.iterator();
            while (iterator.hasNext()) {
                Notification notif = iterator.next();
                
                int titleWidth = mc.fontRendererObj.getStringWidth(notif.getTitle());
                int descWidth = mc.fontRendererObj.getStringWidth(notif.getDescription());
                float textWidth = Math.max(titleWidth, descWidth);
                float containerWidth = paddingLeft + iconWidth + spacing + textWidth + paddingRight;

                // X-Axis (Slide In/Out) Target
                float targetX;
                if (notif.hasExpired()) {
                    targetX = scaledWidth + 10; // slide out
                } else {
                    targetX = scaledWidth - containerWidth - 5;
                }

                // Y-Axis (Stacking Logic) Target
                float currentTargetY;
                if (notif.hasExpired()) {
                    currentTargetY = notif.targetY; // stay on its way out
                } else {
                    currentTargetY = scaledHeight - marginBottom - (activeIndex * (containerHeight + gap)) - containerHeight;
                    notif.targetY = currentTargetY;
                    activeIndex++;
                }

                if (notif.firstFrame) {
                    notif.x = scaledWidth + 10;
                    notif.y = currentTargetY;
                    notif.firstFrame = false;
                }

                // Hyper-Smooth Delta-Time Independent Easing Interpolation
                notif.x += (targetX - notif.x) * animationSpeed * deltaTime;
                notif.y += (currentTargetY - notif.y) * animationSpeed * deltaTime;

                if (notif.hasExpired() && Math.abs(notif.x - targetX) < 1.0f) {
                    iterator.remove();
                    continue;
                }

                float x = notif.x;
                float y = notif.y;

                int shadowColor = new Color(0, 0, 0, 160).getRGB();
                RoundedUtils.drawRound(x, y, containerWidth, containerHeight, 4f, true, shadowColor);

                int bgColor = new Color(18, 18, 18, 240).getRGB();
                RoundedUtils.drawRound(x, y, containerWidth, containerHeight, 4f, false, bgColor);

                int typeColor = notif.getType().getColor();
                int r = (typeColor >> 16) & 0xFF;
                int g = (typeColor >> 8) & 0xFF;
                int b = typeColor & 0xFF;

                int iconGlowColor = (80 << 24) | (r << 16) | (g << 8) | b;
                float iconY = y + (containerHeight - iconHeight) / 2f;
                RoundedUtils.drawRound(x + paddingLeft, iconY, iconWidth, iconHeight, 3f, true, iconGlowColor);

                int iconBgColor = (40 << 24) | (r << 16) | (g << 8) | b;
                RoundedUtils.drawRound(x + paddingLeft, iconY, iconWidth, iconHeight, 3f, false, iconBgColor);

                float textX = x + paddingLeft + iconWidth + spacing;
                
                mc.fontRendererObj.drawStringWithShadow(notif.getType().getIcon(), x + paddingLeft + (iconWidth - mc.fontRendererObj.getStringWidth(notif.getType().getIcon())) / 2f, iconY + (iconHeight - mc.fontRendererObj.FONT_HEIGHT) / 2f, typeColor);

                float totalTextHeight = mc.fontRendererObj.FONT_HEIGHT * 2 + 2;
                float startTextY = y + (containerHeight - totalTextHeight) / 2f;
                
                mc.fontRendererObj.drawStringWithShadow(notif.getTitle(), textX, startTextY, -1);
                mc.fontRendererObj.drawStringWithShadow(notif.getDescription(), textX, startTextY + mc.fontRendererObj.FONT_HEIGHT + 2, 0xFFAAAAAA);


                float progress = Math.max(0, notif.getRemainingTime()) / (float) notif.getDuration();
                float barWidth = containerWidth * progress;
                if (barWidth > 0) {
                    RoundedUtils.drawRound(x, y + containerHeight - 2, barWidth, 2, 1f, false, typeColor);
                }
            }
        }
    }

    public static class NotificationBuilder {
        private final NotificationType type;
        private final NotificationManager manager;
        private String title = "";
        private String description = "";
        private int duration = 2000;

        public NotificationBuilder(NotificationType type, NotificationManager manager) {
            this.type = type;
            this.manager = manager;
        }

        public NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public NotificationBuilder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public void buildAndPublish() {
            manager.add(new Notification(type, title, description, duration));
        }
    }
}
