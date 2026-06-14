package myau.ui.clickgui;

import myau.ui.clickgui.components.Component;
import myau.ui.clickgui.components.impl.BindComponent;
import myau.ui.clickgui.components.impl.CategoryComponent;
import myau.ui.clickgui.components.impl.ModuleComponent;
import myau.ui.clickgui.components.impl.OnlineConfigComponent;
import myau.config.online.OnlineConfigEntry;
import myau.util.Timer;
import myau.module.modules.render.GuiModule;
import myau.util.shader.BlurUtils;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClickGui extends GuiScreen {
    private Timer backgroundFade;
    private Timer blurSmooth;
    private ScaledResolution sr;
    public static ArrayList<CategoryComponent> categories;
    private int actualScreenWidth;
    private int actualScreenHeight;
    private static final java.util.concurrent.ExecutorService ONLINE_CONFIG_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "OpenMiau OnlConfigs");
        thread.setDaemon(true);
        return thread;
    });
    private final myau.config.online.OnlineConfigClient onlineConfigClient = new myau.config.online.OnlineConfigClient();
    private CategoryComponent onlineConfigCategory;
    private static boolean isNotFirstOpen;
    private boolean pendingScaleRefresh;
    public ClickGui() {
        categories = new ArrayList<>();
        int xOffset = 5;
        String[] values = new String[]{"Combat", "Movement", "Render", "Player", "Misc", "Latency", "Minigames", "Target", "OnlineConfig"};

        for (int i = 0; i < values.length; ++i) {
            String c = values[i];
            CategoryComponent categoryComponent = new CategoryComponent(c);
            categoryComponent.setY(5, false);
            categoryComponent.setX(xOffset, false);
            categories.add(categoryComponent);
            xOffset += 98; // width is 92, leaving a gap of 6 pixels between categories
        }
        for (CategoryComponent c : categories) {
            if (c.category.equalsIgnoreCase("OnlineConfig")) {
                this.onlineConfigCategory = c;
                break;
            }
        }
    }

    public void initMain() {
        (this.blurSmooth = this.backgroundFade = new Timer(500.0F)).start();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.sr = new ScaledResolution(mc);
        this.actualScreenWidth = this.sr.getScaledWidth();
        this.actualScreenHeight = this.sr.getScaledHeight();

        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenSize(this.width, this.height);
            categoryComponent.limitPositions();
            categoryComponent.reloadModules();
        }
        this.refreshOnlineConfigs();
    }

    private List<CategoryComponent> getCategoriesInRenderOrder() {
        List<CategoryComponent> renderOrder = new ArrayList<>(categories);
        renderOrder.sort(Comparator.comparingLong(c -> c.lastInteractedTime));
        return renderOrder;
    }

    private CategoryComponent getTopmostUnderCursor(List<CategoryComponent> renderOrder, int x, int y) {
        for (int i = renderOrder.size() - 1; i >= 0; i--) {
            if (renderOrder.get(i).overRect(x, y)) {
                return renderOrder.get(i);
            }
        }
        return null;
    }

    public void drawScreen(int x, int y, float p) {
        // Draw background blur
        GuiModule guiModule = (GuiModule) myau.Myau.moduleManager.modules.get(GuiModule.class);
        if (guiModule != null && guiModule.blur.getValue()) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(0, 0, this.width, this.height, 0.0f, true, Color.black);
            BlurUtils.blurEnd(2, 3.0f); // smooth blur radius
        }

        // Draw dark background overlay
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 130).getRGB());

        List<CategoryComponent> renderOrder = getCategoriesInRenderOrder();
        CategoryComponent topmostUnderCursor = getTopmostUnderCursor(renderOrder, x, y);
        for (CategoryComponent c : renderOrder) {
            c.render(this.fontRendererObj);
            c.mousePosition(x, y, c == topmostUnderCursor);

            for (Component m : c.getModules()) {
                m.drawScreen(x, y);
            }
        }

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        List<CategoryComponent> inputOrder = new ArrayList<>(categories);
        inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
        CategoryComponent topmostCategory = null;
        for (CategoryComponent category : inputOrder) {
            if (category.overRect(mouseX, mouseY)) {
                topmostCategory = category;
                break;
            }
        }

        if (topmostCategory != null) {
            topmostCategory.markInteracted();
        }

        if (mouseButton == 0) {
            for (CategoryComponent category : categories) {
                category.overTitle(false);
            }
            if (topmostCategory != null && topmostCategory.draggable(mouseX, mouseY)) {
                topmostCategory.overTitle(true);
                topmostCategory.xx = mouseX - topmostCategory.getX();
                topmostCategory.yy = mouseY - topmostCategory.getY();
                topmostCategory.dragging = true;
            }
        }

        if (mouseButton == 1 && topmostCategory != null && topmostCategory.overTitle(mouseX, mouseY)) {
            topmostCategory.mouseClicked(!topmostCategory.isOpened());
        }

        if (topmostCategory != null && topmostCategory.isOpened() && !topmostCategory.getModules().isEmpty() && !topmostCategory.overTitle(mouseX, mouseY)) {
            for (Component component : topmostCategory.getModules()) {
                if (component.onClick(mouseX, mouseY, mouseButton)) {
                    break;
                }
            }
        }
    }

    public void mouseReleased(int x, int y, int button) {
        if (button == 0) {
            for (CategoryComponent category : categories) {
                category.overTitle(false);
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.mouseReleased(x, y, button);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            int mouseX = Mouse.getEventX() * this.width / mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
            
            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput, mouseX, mouseY);
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (k == Keyboard.KEY_ESCAPE) {
            if (!binding()) {
                this.mc.displayGuiScreen(null);
                return;
            }
        }

        for (CategoryComponent category : categories) {
            if (category.isOpened() && !category.getModules().isEmpty()) {
                for (Component module : category.getModules()) {
                    module.keyTyped(t, k);
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        for (CategoryComponent c : categories) {
            c.dragging = false;
            c.onGuiClosed();
            for (Component m : c.getModules()) {
                m.onGuiClosed();
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) {
                    for (Component component : ((ModuleComponent) m).settings) {
                        if (component instanceof BindComponent && ((BindComponent) component).isBinding) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void onSliderChange() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) {
                    ((ModuleComponent) m).onSliderChange();
                }
            }
        }
    }

    private void refreshOnlineConfigs() {
        this.setOnlineConfigStatus("Loading...", "fetching configs", null);
        ONLINE_CONFIG_EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = java.util.Collections
                        .unmodifiableList(new java.util.ArrayList<>(this.onlineConfigClient.list()));
                mc.addScheduledTask(() -> this.setOnlineConfigEntries(entries));
            } catch (Exception e) {
                mc.addScheduledTask(() -> this.setOnlineConfigStatus("Fetch failed", e.getMessage(), null));
            }
        });
    }

    private void setOnlineConfigEntries(List<OnlineConfigEntry> entries) {
        if (entries.isEmpty()) {
            this.setOnlineConfigStatus("No configs", "online list is empty", null);
            return;
        }
        ArrayList<Component> components = new ArrayList<>();
        float offset = 16f;
        for (OnlineConfigEntry entry : entries) {
            String subtitle = "by " + entry.getAuthor() + " | " + safe(entry.setting_type);
            if (!entry.getVersion().isEmpty()) {
                subtitle += " • v" + entry.getVersion();
            }
            components.add(new OnlineConfigComponent(this.onlineConfigCategory, offset, entry.getName(), subtitle,
                    () -> this.loadOnlineConfig(entry)));
            offset += 24f;
        }
        this.onlineConfigCategory.modules.clear();
        this.onlineConfigCategory.modules.addAll(components);
        this.onlineConfigCategory.updateHeight();
    }

    private void setOnlineConfigStatus(String title, String subtitle, Runnable action) {
        if (this.onlineConfigCategory == null)
            return;
        ArrayList<Component> components = new ArrayList<>();
        components.add(new OnlineConfigComponent(this.onlineConfigCategory, 16f, title, subtitle, action));
        this.onlineConfigCategory.modules.clear();
        this.onlineConfigCategory.modules.addAll(components);
        this.onlineConfigCategory.updateHeight();
    }

    private void loadOnlineConfig(OnlineConfigEntry entry) {
        this.setOnlineConfigStatus("Loading " + entry.getName(), "please wait", null);
        ONLINE_CONFIG_EXECUTOR.execute(() -> {
            try {
                String json = this.onlineConfigClient.load(entry.getId());
                mc.addScheduledTask(() -> {
                    try {
                        int applied = new myau.config.online.OnlineConfigApplier().apply(json);
                        myau.util.ChatUtil.sendFormatted(
                                String.format("%sOnline config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                                        myau.Myau.clientName, entry.getName(), applied));
                        this.refreshOnlineConfigs();
                    } catch (Exception e) {
                        myau.util.ChatUtil.sendFormatted(
                                myau.Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r");
                        this.refreshOnlineConfigs();
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> {
                    myau.util.ChatUtil.sendFormatted(
                            myau.Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r");
                    this.refreshOnlineConfigs();
                });
            }
        });
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }

    public void requestScaleRefresh() {
        this.pendingScaleRefresh = true;
    }

    public static double getActiveRenderScale() {
        return 1.0D;
    }
}
