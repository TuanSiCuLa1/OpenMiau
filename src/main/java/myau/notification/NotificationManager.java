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
    private boolean initialized = false;
    public final myau.property.properties.DragProperty drag = new myau.property.properties.DragProperty("Notifications", new myau.util.vector.Vector2d(100, 100));

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
        myau.module.modules.render.HUD hud = (myau.module.modules.render.HUD) myau.Myau.moduleManager.modules.get(myau.module.modules.render.HUD.class);
        if (hud == null || !hud.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000f; 
        lastRenderTime = currentTime;
        
        if (deltaTime > 0.1f) deltaTime = 0.1f;
        
        final float animationSpeed = 12.0f; 
        
        final float paddingLeft = 8f;
        final float paddingRight = 8f;
        final float iconWidth = 16f;
        final float iconHeight = 16f;
        final float spacing = 7f;
        final float containerHeight = 28f;
        final float marginBottom = 10f;
        final float gap = 4f;

        if (!initialized) {
            drag.position.x = drag.targetPosition.x = sr.getScaledWidth() - 165;
            drag.position.y = drag.targetPosition.y = sr.getScaledHeight() - 15;
            initialized = true;
        }

        if (!(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat || mc.currentScreen instanceof myau.ui.clickgui.ClickGui)) {
            drag.position.x = drag.targetPosition.x;
            drag.position.y = drag.targetPosition.y;
        }

        drag.scale.x = 150;
        drag.scale.y = 40;

        float anchorX = (float) drag.position.x;
        float anchorY = (float) drag.position.y;

        float scaledWidth = sr.getScaledWidth();
        float scaledHeight = sr.getScaledHeight();
        
        synchronized (notifications) {
            int activeIndex = 0;
            Iterator<Notification> iterator = notifications.iterator();
            while (iterator.hasNext()) {
                Notification notif = iterator.next();
                
                float containerWidth = getContainerWidth(notif);

                float targetX;
                if (notif.hasExpired()) {
                    targetX = scaledWidth + 10;
                } else {
                    targetX = anchorX;
                }

                float currentTargetY;
                if (notif.hasExpired()) {
                    currentTargetY = notif.targetY;
                } else {
                    currentTargetY = anchorY - (activeIndex * (containerHeight + gap)) - containerHeight;
                    notif.targetY = currentTargetY;
                    activeIndex++;
                }

                if (notif.firstFrame) {
                    notif.x = scaledWidth + 10;
                    notif.y = currentTargetY;
                    notif.firstFrame = false;
                }

                notif.x += (targetX - notif.x) * animationSpeed * deltaTime;
                notif.y += (currentTargetY - notif.y) * animationSpeed * deltaTime;

                if (notif.hasExpired() && Math.abs(notif.x - targetX) < 1.0f) {
                    iterator.remove();
                }
            }

            if (hud.shaders.getValue()) {
                myau.util.shader.RenderSystem.renderBloom(() -> {
                    for (Notification notif : notifications) {
                        renderNotificationBloom(notif, containerHeight, iconWidth, iconHeight, paddingLeft, spacing);
                    }
                });
                
                if (hud.blurSettings.getValue()) {
                    myau.util.shader.RenderSystem.renderBlur(() -> {
                        for (Notification notif : notifications) {
                            float containerWidth = getContainerWidth(notif);
                            RoundedUtils.drawRound(notif.x, notif.y, containerWidth, containerHeight, 4f, false, Color.black);
                        }
                    });
                }
            }

            for (Notification notif : notifications) {
                renderNotificationNormal(notif, hud, containerHeight, iconWidth, iconHeight, paddingLeft, spacing);
            }
        }
    }

    private float getContainerWidth(Notification notif) {
        myau.util.font.Font boldFont = myau.util.font.Fonts.MAIN.get(14, myau.util.font.Weight.BOLD);
        myau.util.font.Font regFont = myau.util.font.Fonts.MAIN.get(12);
        int titleWidth = boldFont.width(notif.getTitle());
        int descWidth = regFont.width(notif.getDescription());
        float textWidth = Math.max(titleWidth, descWidth);
        return Math.max(150f, 8f + 16f + 7f + textWidth + 12f);
    }

    private void renderNotificationBloom(Notification notif, float containerHeight, float iconWidth, float iconHeight, float paddingLeft, float spacing) {
        float x = notif.x;
        float y = notif.y;
        float containerWidth = getContainerWidth(notif);

        int typeColor = notif.getType().getColor();
        int r = (typeColor >> 16) & 0xFF;
        int g = (typeColor >> 8) & 0xFF;
        int b = typeColor & 0xFF;

        int iconGlowColor = (80 << 24) | (r << 16) | (g << 8) | b;
        float iconY = y + (containerHeight - iconHeight) / 2f;
        RoundedUtils.drawRound(x + paddingLeft, iconY, iconWidth, iconHeight, 3f, true, iconGlowColor);

        float progress = Math.max(0, notif.getRemainingTime()) / (float) notif.getDuration();
        float barWidth = containerWidth * progress;
        if (barWidth > 0) {
            RoundedUtils.drawRound(x, y + containerHeight - 2, barWidth, 2, 1f, true, typeColor);
        }
        
        myau.util.font.Font boldFont = myau.util.font.Fonts.MAIN.get(14, myau.util.font.Weight.BOLD);
        myau.util.font.Font regFont = myau.util.font.Fonts.MAIN.get(12);
        float textX = x + paddingLeft + iconWidth + spacing;
        float totalTextHeight = boldFont.height() + regFont.height() + 2;
        float startTextY = y + (containerHeight - totalTextHeight) / 2f;

        boldFont.drawWithShadow(notif.getType().getIcon(), x + paddingLeft + (iconWidth - boldFont.width(notif.getType().getIcon())) / 2f, iconY + (iconHeight - boldFont.height()) / 2f, typeColor);
        boldFont.drawWithShadow(notif.getTitle(), textX, startTextY, -1);
    }

    private void renderNotificationNormal(Notification notif, myau.module.modules.render.HUD hud, float containerHeight, float iconWidth, float iconHeight, float paddingLeft, float spacing) {
        float x = notif.x;
        float y = notif.y;
        float containerWidth = getContainerWidth(notif);

        int shadowColor = new Color(0, 0, 0, 160).getRGB();
        if (!(hud.shaders.getValue() && hud.blurSettings.getValue())) {
            RoundedUtils.drawRound(x, y, containerWidth, containerHeight, 4f, true, shadowColor);
        }

        int bgColor = new Color(18, 18, 18, (hud.shaders.getValue() && hud.blurSettings.getValue()) ? 120 : 240).getRGB();
        RoundedUtils.drawRound(x, y, containerWidth, containerHeight, 4f, false, bgColor);

        int typeColor = notif.getType().getColor();
        int r = (typeColor >> 16) & 0xFF;
        int g = (typeColor >> 8) & 0xFF;
        int b = typeColor & 0xFF;

        float iconY = y + (containerHeight - iconHeight) / 2f;
        int iconBgColor = (40 << 24) | (r << 16) | (g << 8) | b;
        RoundedUtils.drawRound(x + paddingLeft, iconY, iconWidth, iconHeight, 3f, false, iconBgColor);

        myau.util.font.Font boldFont = myau.util.font.Fonts.MAIN.get(14, myau.util.font.Weight.BOLD);
        myau.util.font.Font regFont = myau.util.font.Fonts.MAIN.get(12);
        float textX = x + paddingLeft + iconWidth + spacing;
        
        boldFont.drawWithShadow(notif.getType().getIcon(), x + paddingLeft + (iconWidth - boldFont.width(notif.getType().getIcon())) / 2f, iconY + (iconHeight - boldFont.height()) / 2f, typeColor);

        float totalTextHeight = boldFont.height() + regFont.height() + 2;
        float startTextY = y + (containerHeight - totalTextHeight) / 2f;
        
        boldFont.drawWithShadow(notif.getTitle(), textX, startTextY, -1);
        regFont.drawWithShadow(notif.getDescription(), textX, startTextY + boldFont.height() + 2, 0xFFAAAAAA);

        float progress = Math.max(0, notif.getRemainingTime()) / (float) notif.getDuration();
        float barWidth = containerWidth * progress;
        if (barWidth > 0) {
            RoundedUtils.drawRound(x, y + containerHeight - 2, barWidth, 2, 1f, false, typeColor);
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
