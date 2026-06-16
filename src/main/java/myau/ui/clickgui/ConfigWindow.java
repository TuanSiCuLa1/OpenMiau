package myau.ui.clickgui;

import myau.config.Config;
import myau.config.online.OnlineConfigApplier;
import myau.config.online.OnlineConfigClient;
import myau.config.online.OnlineConfigEntry;
import myau.util.ChatUtil;
import myau.util.font.Font;
import myau.util.font.Fonts;
import myau.util.math.MathUtil;
import myau.util.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigWindow {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public float x, y, width, height;
    private boolean dragging;
    private float dragX, dragY;

    private float localScrollY, targetLocalScrollY;
    private float onlineScrollY, targetOnlineScrollY;

    private List<String> localConfigs = new ArrayList<>();
    private List<OnlineConfigEntry> onlineConfigs = new ArrayList<>();
    private String onlineStatus = "Loading...";

    private boolean isTyping = false;
    private final StringBuilder typeText = new StringBuilder();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ConfigWindowThread");
        t.setDaemon(true);
        return t;
    });
    private final OnlineConfigClient onlineClient = new OnlineConfigClient();

    public ConfigWindow(float x, float y) {
        this.x = x;
        this.y = y;
        this.width = 340;
        this.height = 240;
        refreshLocalConfigs();
        refreshOnlineConfigs();
    }

    public void refreshLocalConfigs() {
        localConfigs.clear();
        File configDir = new File("./config/Myau/");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    localConfigs.add(file.getName().replace(".json", ""));
                }
            }
        }
    }

    private void refreshOnlineConfigs() {
        onlineStatus = "Fetching configs...";
        onlineConfigs.clear();
        EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = onlineClient.list();
                mc.addScheduledTask(() -> {
                    if (entries.isEmpty()) onlineStatus = "No configs found.";
                    else {
                        onlineConfigs.addAll(entries);
                        onlineStatus = "";
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> onlineStatus = "Fetch failed!");
            }
        });
    }

    public void drawWindow(int mouseX, int mouseY, float delta) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        localScrollY = MathUtil.lerp(localScrollY, targetLocalScrollY, 0.015f * delta);
        onlineScrollY = MathUtil.lerp(onlineScrollY, targetOnlineScrollY, 0.015f * delta);

        RenderUtils.drawRoundedGradientOutlinedRectangle(x, y, x + width, y + height, 8,
                new Color(0, 0, 0, 150).getRGB(), new Color(81, 99, 149).getRGB(), new Color(97, 67, 133).getRGB());

        Font titleFont = Fonts.MINECRAFT.get(22);
        Font regularFont = Fonts.MINECRAFT.get(18);
        Font smallFont = Fonts.MINECRAFT.get(16);

        RenderUtils.drawRect(x, y + 20, x + width, y + 21, new Color(255, 255, 255, 50).getRGB());
        titleFont.draw("Config Manager", x + width / 2 - titleFont.width("Config Manager") / 2, y + 5, Color.WHITE.getRGB(), true);

        float halfWidth = width / 2;
        RenderUtils.drawRect(x + halfWidth, y + 21, x + halfWidth + 1, y + height, new Color(255, 255, 255, 50).getRGB());

        titleFont.draw("Local Configs", x + halfWidth / 2 - titleFont.width("Local Configs") / 2, y + 25, new Color(200, 200, 200).getRGB(), true);
        titleFont.draw("Online Configs", x + halfWidth + halfWidth / 2 - titleFont.width("Online Configs") / 2, y + 25, new Color(200, 200, 200).getRGB(), true);

        float localStartX = x + 5;
        float localStartY = y + 45;

        RenderUtils.drawRect(localStartX, localStartY, localStartX + halfWidth - 10, localStartY + 15, new Color(0, 0, 0, 100).getRGB());
        String displayTxt = (typeText.length() == 0 && !isTyping) ? "Create new..." : typeText.toString() + (isTyping && System.currentTimeMillis() % 1000 < 500 ? "_" : "");
        regularFont.draw(displayTxt, localStartX + 4, localStartY + 3, isTyping ? Color.WHITE.getRGB() : new Color(150, 150, 150).getRGB(), false);

        float localListY = localStartY + 20;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x, localListY, halfWidth, y + height - localListY);
        float currentLocalY = localListY + localScrollY;

        for (String cfg : localConfigs) {
            boolean isHovered = mouseX >= localStartX && mouseX <= localStartX + halfWidth - 10 && mouseY >= currentLocalY && mouseY <= currentLocalY + 25 && mouseY > localListY && mouseY < y + height;
            int bgColor = isHovered ? new Color(255, 255, 255, 30).getRGB() : new Color(0, 0, 0, 50).getRGB();
            RenderUtils.drawRoundedRectangle(localStartX, currentLocalY, localStartX + halfWidth - 10, currentLocalY + 25, 4, bgColor);

            regularFont.draw(cfg, localStartX + 4, currentLocalY + 3, Color.WHITE.getRGB(), false);
            smallFont.draw("L: Load | R: Save | Sh+L: Del", localStartX + 4, currentLocalY + 14, new Color(150, 150, 150).getRGB(), false);
            currentLocalY += 28;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        float onlineStartX = x + halfWidth + 5;
        float onlineStartY = y + 45;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x + halfWidth, onlineStartY, halfWidth, y + height - onlineStartY);
        float currentOnlineY = onlineStartY + onlineScrollY;

        if (!onlineStatus.isEmpty()) {
            regularFont.draw(onlineStatus, onlineStartX + 4, currentOnlineY + 5, new Color(200, 200, 200).getRGB(), false);
        } else {
            for (OnlineConfigEntry entry : onlineConfigs) {
                boolean isHovered = mouseX >= onlineStartX && mouseX <= onlineStartX + halfWidth - 10 && mouseY >= currentOnlineY && mouseY <= currentOnlineY + 25 && mouseY > onlineStartY && mouseY < y + height;
                int bgColor = isHovered ? new Color(255, 255, 255, 30).getRGB() : new Color(0, 0, 0, 50).getRGB();
                RenderUtils.drawRoundedRectangle(onlineStartX, currentOnlineY, onlineStartX + halfWidth - 10, currentOnlineY + 25, 4, bgColor);

                regularFont.draw(entry.getName(), onlineStartX + 4, currentOnlineY + 3, Color.WHITE.getRGB(), false);
                smallFont.draw("by " + entry.getAuthor() + " | " + entry.setting_type, onlineStartX + 4, currentOnlineY + 14, new Color(150, 150, 150).getRGB(), false);
                currentOnlineY += 28;
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY, x, y, width, height)) return false;

        if (mouseY <= y + 20) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return true;
        }

        float halfWidth = width / 2;
        float localStartX = x + 5;
        float localStartY = y + 45;

        if (isHovered(mouseX, mouseY, localStartX, localStartY, halfWidth - 10, 15)) {
            isTyping = true;
            return true;
        } else {
            isTyping = false;
        }

        float localListY = localStartY + 20;
        if (mouseX < x + halfWidth && mouseY > localListY) {
            float currentLocalY = localListY + targetLocalScrollY;
            for (String cfg : localConfigs) {
                if (isHovered(mouseX, mouseY, localStartX, currentLocalY, halfWidth - 10, 25)) {
                    if (button == 0) { 
                        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                            File file = new File("./config/Myau/", cfg + ".json");
                            if (file.exists() && file.delete()) ChatUtil.display("Deleted config: &c" + cfg);
                            refreshLocalConfigs();
                        } else {
                            new Config(cfg, false).load();
                        }
                    } else if (button == 1) { 
                        new Config(cfg, false).save();
                    }
                    return true;
                }
                currentLocalY += 28;
            }
        }

        float onlineStartX = x + halfWidth + 5;
        float onlineStartY = y + 45;
        if (mouseX > x + halfWidth && mouseY > onlineStartY) {
            float currentOnlineY = onlineStartY + targetOnlineScrollY;
            for (OnlineConfigEntry entry : onlineConfigs) {
                if (isHovered(mouseX, mouseY, onlineStartX, currentOnlineY, halfWidth - 10, 25) && button == 0) {
                    loadOnlineConfig(entry);
                    return true;
                }
                currentOnlineY += 28;
            }
        }
        return true;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    public void onScroll(int wheel, int mouseX, int mouseY) {
        if (!isHovered(mouseX, mouseY, x, y, width, height)) return;

        float scrollSpeed = 20f;
        if (mouseX < x + width / 2) {
            targetLocalScrollY += (wheel > 0 ? scrollSpeed : -scrollSpeed);
            float maxScroll = Math.max(0, (localConfigs.size() * 28) - (height - 65));
            targetLocalScrollY = Math.max(-maxScroll, Math.min(0, targetLocalScrollY));
        } else {
            targetOnlineScrollY += (wheel > 0 ? scrollSpeed : -scrollSpeed);
            float maxScroll = Math.max(0, (onlineConfigs.size() * 28) - (height - 45));
            targetOnlineScrollY = Math.max(-maxScroll, Math.min(0, targetOnlineScrollY));
        }
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (isTyping) {
            if (keyCode == Keyboard.KEY_ESCAPE) isTyping = false;
            else if (keyCode == Keyboard.KEY_RETURN) {
                if (typeText.length() > 0) {
                    new Config(typeText.toString(), true).save();
                    typeText.setLength(0);
                    isTyping = false;
                    refreshLocalConfigs();
                }
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (typeText.length() > 0) typeText.setLength(typeText.length() - 1);
            } else if (String.valueOf(typedChar).matches("[a-zA-Z0-9_-]") && typeText.length() < 16) {
                typeText.append(typedChar);
            }
            return true;
        }
        return false;
    }

    private void loadOnlineConfig(OnlineConfigEntry entry) {
        ChatUtil.display("Loading config: &e" + entry.getName() + "...");
        EXECUTOR.execute(() -> {
            try {
                String json = onlineClient.load(entry.getId());
                mc.addScheduledTask(() -> {
                    try {
                        int applied = new OnlineConfigApplier().apply(json);
                        ChatUtil.display("Online config loaded (&a&o%s&r) &7- applied %d setting(s)&r", entry.getName(), applied);
                    } catch (Exception e) {
                        ChatUtil.display("Failed to apply online config: &c" + e.getMessage() + "&r");
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> ChatUtil.display("Failed to fetch online config: &c" + e.getMessage() + "&r"));
            }
        });
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isTyping() {
        return isTyping;
    }

    private void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(mc);
        final double scale = sr.getScaleFactor();
        y = sr.getScaledHeight() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        GL11.glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }
}