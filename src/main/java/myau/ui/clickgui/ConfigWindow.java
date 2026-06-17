package myau.ui.clickgui;

import myau.Myau;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigWindow {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int TAB_LOCAL = 0;
    private static final int TAB_ONLINE = 1;
    private static final int TAB_USER = 2;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public float x, y, width, height;
    private boolean dragging;
    private float dragX, dragY;
    private float localScrollY, targetLocalScrollY;
    private float onlineScrollY, targetOnlineScrollY;
    private float userScrollY, targetUserScrollY;
    private int selectedTab = TAB_LOCAL;

    private final List<File> localConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> onlineConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> userConfigs = new ArrayList<>();
    private String onlineStatus = "Loading...";
    private String userStatus = "Loading...";
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
        this.width = 360;
        this.height = 250;
        refreshLocalConfigs();
        refreshOnlineConfigs();
        refreshUserConfigs();
    }

    public void refreshLocalConfigs() {
        localConfigs.clear();
        File configDir = new File("./config/Myau/");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) localConfigs.add(file);
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

    private void refreshUserConfigs() {
        userStatus = "Fetching user configs...";
        userConfigs.clear();
        EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = onlineClient.listUserConfigs();
                mc.addScheduledTask(() -> {
                    if (entries.isEmpty()) userStatus = "No user configs found.";
                    else {
                        userConfigs.addAll(entries);
                        userStatus = "";
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> userStatus = "Fetch failed!");
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
        userScrollY = MathUtil.lerp(userScrollY, targetUserScrollY, 0.015f * delta);

        RenderUtils.drawRoundedGradientOutlinedRectangle(x, y, x + width, y + height, 8,
                new Color(0, 0, 0, 150).getRGB(), new Color(81, 99, 149).getRGB(), new Color(97, 67, 133).getRGB());
        Font titleFont = Fonts.MINECRAFT.get(22);
        Font regularFont = Fonts.MINECRAFT.get(18);
        Font smallFont = Fonts.MINECRAFT.get(16);
        RenderUtils.drawRect(x, y + 20, x + width, y + 21, new Color(255, 255, 255, 50).getRGB());
        titleFont.draw("Config Manager", x + width / 2 - titleFont.width("Config Manager") / 2, y + 5, Color.WHITE.getRGB(), true);

        float tabY = y + 25;
        float tabWidth = (width - 20) / 3;
        drawTab("Local", x + 5, tabY, tabWidth, selectedTab == TAB_LOCAL, mouseX, mouseY, titleFont);
        drawTab("Online", x + 7 + tabWidth, tabY, tabWidth, selectedTab == TAB_ONLINE, mouseX, mouseY, titleFont);
        drawTab("MiauUser", x + 9 + tabWidth * 2, tabY, tabWidth, selectedTab == TAB_USER, mouseX, mouseY, titleFont);

        if (selectedTab == TAB_LOCAL) drawLocalTab(mouseX, mouseY, regularFont, smallFont);
        else if (selectedTab == TAB_ONLINE) drawRemoteTab(mouseX, mouseY, regularFont, smallFont, onlineConfigs, onlineStatus, onlineScrollY, false);
        else drawRemoteTab(mouseX, mouseY, regularFont, smallFont, userConfigs, userStatus, userScrollY, true);
    }

    private void drawTab(String text, float tabX, float tabY, float tabWidth, boolean selected, int mouseX, int mouseY, Font font) {
        boolean hovered = isHovered(mouseX, mouseY, tabX, tabY, tabWidth, 16);
        int color = selected ? new Color(255, 255, 255, 45).getRGB() : hovered ? new Color(255, 255, 255, 25).getRGB() : new Color(0, 0, 0, 80).getRGB();
        RenderUtils.drawRoundedRectangle(tabX, tabY, tabX + tabWidth, tabY + 16, 4, color);
        font.draw(text, tabX + tabWidth / 2 - font.width(text) / 2, tabY + 4, selected ? Color.WHITE.getRGB() : new Color(180, 180, 180).getRGB(), true);
    }

    private void drawLocalTab(int mouseX, int mouseY, Font regularFont, Font smallFont) {
        float startX = x + 8;
        float inputY = y + 48;
        float listY = inputY + 22;
        float listWidth = width - 16;
        RenderUtils.drawRect(startX, inputY, startX + listWidth, inputY + 15, new Color(0, 0, 0, 100).getRGB());
        String displayTxt = (typeText.length() == 0 && !isTyping) ? "Create new..." : typeText.toString() + (isTyping && System.currentTimeMillis() % 1000 < 500 ? "_" : "");
        regularFont.draw(displayTxt, startX + 4, inputY + 3, isTyping ? Color.WHITE.getRGB() : new Color(150, 150, 150).getRGB(), false);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x, listY, width, y + height - listY);
        float currentY = listY + localScrollY;
        for (File file : localConfigs) {
            String name = removeJsonExtension(file.getName());
            boolean hovered = mouseX >= startX && mouseX <= startX + listWidth && mouseY >= currentY && mouseY <= currentY + 25 && mouseY > listY && mouseY < y + height;
            int bgColor = hovered ? new Color(255, 255, 255, 30).getRGB() : new Color(0, 0, 0, 50).getRGB();
            RenderUtils.drawRoundedRectangle(startX, currentY, startX + listWidth, currentY + 25, 4, bgColor);
            regularFont.draw(name, startX + 4, currentY + 3, Color.WHITE.getRGB(), false);
            smallFont.draw("Last Used: " + formatLastUsed(file), startX + 4, currentY + 14, new Color(150, 150, 150).getRGB(), false);
            currentY += 28;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    private void drawRemoteTab(int mouseX, int mouseY, Font regularFont, Font smallFont, List<OnlineConfigEntry> entries, String status, float scrollY, boolean userConfig) {
        float startX = x + 8;
        float listY = y + 48;
        float listWidth = width - 16;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x, listY, width, y + height - listY);
        float currentY = listY + scrollY;
        if (!status.isEmpty()) {
            regularFont.draw(status, startX + 4, currentY + 5, new Color(200, 200, 200).getRGB(), false);
        } else {
            for (OnlineConfigEntry entry : entries) {
                boolean hovered = mouseX >= startX && mouseX <= startX + listWidth && mouseY >= currentY && mouseY <= currentY + 25 && mouseY > listY && mouseY < y + height;
                int bgColor = hovered ? new Color(255, 255, 255, 30).getRGB() : new Color(0, 0, 0, 50).getRGB();
                RenderUtils.drawRoundedRectangle(startX, currentY, startX + listWidth, currentY + 25, 4, bgColor);
                regularFont.draw(entry.getName(), startX + 4, currentY + 3, Color.WHITE.getRGB(), false);
                String meta = userConfig ? "by " + entry.getAuthor() + " | " + entry.getLoadCount() + " loads" : "by " + entry.getAuthor() + " | " + safe(entry.setting_type);
                smallFont.draw(meta, startX + 4, currentY + 14, new Color(150, 150, 150).getRGB(), false);
                currentY += 28;
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
        if (handleTabClick(mouseX, mouseY)) return true;
        if (selectedTab == TAB_LOCAL) return handleLocalClick(mouseX, mouseY, button);
        return handleRemoteClick(mouseX, mouseY, button);
    }

    private boolean handleTabClick(int mouseX, int mouseY) {
        float tabY = y + 25;
        float tabWidth = (width - 20) / 3;
        if (isHovered(mouseX, mouseY, x + 5, tabY, tabWidth, 16)) { selectedTab = TAB_LOCAL; isTyping = false; return true; }
        if (isHovered(mouseX, mouseY, x + 7 + tabWidth, tabY, tabWidth, 16)) { selectedTab = TAB_ONLINE; isTyping = false; return true; }
        if (isHovered(mouseX, mouseY, x + 9 + tabWidth * 2, tabY, tabWidth, 16)) { selectedTab = TAB_USER; isTyping = false; return true; }
        return false;
    }

    private boolean handleLocalClick(int mouseX, int mouseY, int button) {
        float startX = x + 8;
        float inputY = y + 48;
        float listY = inputY + 22;
        float listWidth = width - 16;
        if (isHovered(mouseX, mouseY, startX, inputY, listWidth, 15)) {
            isTyping = true;
            return true;
        }
        isTyping = false;
        if (mouseY > listY) {
            float currentY = listY + targetLocalScrollY;
            for (File file : localConfigs) {
                if (isHovered(mouseX, mouseY, startX, currentY, listWidth, 25)) {
                    String configName = removeJsonExtension(file.getName());
                    if (button == 0) {
                        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                            if (file.exists() && file.delete()) ChatUtil.display("Deleted config: &c" + configName);
                            refreshLocalConfigs();
                        } else {
                            new Config(configName, false).load();
                        }
                    } else if (button == 1) {
                        new Config(configName, false).save();
                        refreshLocalConfigs();
                    }
                    return true;
                }
                currentY += 28;
            }
        }
        return true;
    }

    private boolean handleRemoteClick(int mouseX, int mouseY, int button) {
        if (button != 0) return true;
        float startX = x + 8;
        float listY = y + 48;
        float listWidth = width - 16;
        List<OnlineConfigEntry> entries = selectedTab == TAB_ONLINE ? onlineConfigs : userConfigs;
        float scroll = selectedTab == TAB_ONLINE ? targetOnlineScrollY : targetUserScrollY;
        boolean userConfig = selectedTab == TAB_USER;
        float currentY = listY + scroll;
        for (OnlineConfigEntry entry : entries) {
            if (isHovered(mouseX, mouseY, startX, currentY, listWidth, 25)) {
                loadOnlineConfig(entry, userConfig);
                return true;
            }
            currentY += 28;
        }
        return true;
    }
    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    public void onScroll(int wheel, int mouseX, int mouseY) {
        if (!isHovered(mouseX, mouseY, x, y, width, height)) return;
        float scrollSpeed = 20f;
        if (selectedTab == TAB_LOCAL) {
            targetLocalScrollY += (wheel > 0 ? scrollSpeed : -scrollSpeed);
            float maxScroll = Math.max(0, (localConfigs.size() * 28) - (height - 70));
            targetLocalScrollY = Math.max(-maxScroll, Math.min(0, targetLocalScrollY));
        } else if (selectedTab == TAB_ONLINE) {
            targetOnlineScrollY += (wheel > 0 ? scrollSpeed : -scrollSpeed);
            float maxScroll = Math.max(0, (onlineConfigs.size() * 28) - (height - 48));
            targetOnlineScrollY = Math.max(-maxScroll, Math.min(0, targetOnlineScrollY));
        } else {
            targetUserScrollY += (wheel > 0 ? scrollSpeed : -scrollSpeed);
            float maxScroll = Math.max(0, (userConfigs.size() * 28) - (height - 48));
            targetUserScrollY = Math.max(-maxScroll, Math.min(0, targetUserScrollY));
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

    private void loadOnlineConfig(OnlineConfigEntry entry, boolean userConfig) {
        EXECUTOR.execute(() -> {
            try {
                String json = userConfig ? onlineClient.loadUserConfig(entry.getId()) : onlineClient.load(entry.getId());
                mc.addScheduledTask(() -> {
                    try {
                        showMetadata(entry, userConfig);
                        int applied = new OnlineConfigApplier().apply(json);
                        if (userConfig) {
                            ChatUtil.display("%sUser config loaded (&a&o%s&r) &7- applied %d setting(s)&r", entry.getName(), applied);
                        } else {
                            ChatUtil.display("%sOnline config loaded (&a&o%s&r) &7- applied %d setting(s)&r", entry.getName(), applied);
                        }
                    } catch (Exception e) {
                        ChatUtil.display(Myau.clientName + "Failed to load " + (userConfig ? "user" : "online") + " config: &c" + e.getMessage() + "&r");
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> ChatUtil.display(Myau.clientName + "Failed to load " + (userConfig ? "user" : "online") + " config: &c" + e.getMessage() + "&r"));
            }
        });
    }

    private void showMetadata(OnlineConfigEntry entry, boolean userConfig) {
        if (userConfig) {
            ChatUtil.display(Myau.clientName + "User config info:&r");
            ChatUtil.display("&fName: &a" + entry.getName() + "&r");
            ChatUtil.display("&fID: &b" + entry.getId() + "&r");
            ChatUtil.display("&fAuthor: &a" + entry.getAuthor() + "&r");
            ChatUtil.display("&fUpload time: &b" + safe(entry.date) + "&r");
            ChatUtil.display("&fLoads: &e" + entry.getLoadCount() + "&r");
            if (!entry.getVersion().isEmpty()) ChatUtil.display("&fVersion: &e" + entry.getVersion() + "&r");
            if (entry.description != null && !entry.description.trim().isEmpty()) ChatUtil.display("&fNote: &7" + entry.description + "&r");
            return;
        }
        ChatUtil.display(Myau.clientName + "Loading online config...&r");
        ChatUtil.display("&fName: &a" + entry.getName() + "&r");
        ChatUtil.display("&fUpload time: &b" + safe(entry.date) + "&r");
        ChatUtil.display("&fAuthor: &a" + entry.getAuthor() + "&r");
        ChatUtil.display("&fType: &b" + safe(entry.setting_type) + "&r");
        ChatUtil.display("&fStatus: &e" + safe(entry.status_type) + "&r");
        if (!entry.getVersion().isEmpty()) ChatUtil.display("&fVersion: &e" + entry.getVersion() + "&r");
        if (entry.description != null && !entry.description.trim().isEmpty()) ChatUtil.display("&fDescription: &7" + entry.description + "&r");
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isTyping() {
        return isTyping;
    }

    private String removeJsonExtension(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private String formatLastUsed(File file) {
        return file.exists() ? DATE_FORMAT.format(new Date(file.lastModified())) : "unknown";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
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