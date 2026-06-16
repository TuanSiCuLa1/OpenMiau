package myau.ui.clickgui;

import myau.module.modules.render.ClickGUI;
import myau.ui.clickgui.components.Component;
import myau.ui.clickgui.components.impl.BindComponent;
import myau.ui.clickgui.components.impl.CategoryComponent;
import myau.ui.clickgui.components.impl.ModuleComponent;
import myau.ui.clickgui.components.impl.SearchBarComponent;
import myau.util.shader.BlurUtils;
import myau.util.Timer;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
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

    private ConfigWindow configWindow;

    private int actualScreenWidth;
    private int actualScreenHeight;
    private boolean pendingScaleRefresh;
    private long lastMS = System.currentTimeMillis();

    public ClickGui() {
        categories = new ArrayList<>();
        String[] values = new String[]{"Combat", "Target", "Movement", "Player", "Render", "Misc", "Latency", "Config", "Search", "Minigames"};

        float startX = 15;
        float marginX = 105;

        for (int i = 0; i < values.length; ++i) {
            CategoryComponent cc = new CategoryComponent(values[i]);
            cc.setX(startX + (i / 2) * marginX, false); 
            cc.setY(15 + (i % 2) * 60, false);          
            categories.add(cc);
        }
    }

    public void initMain() {
        (this.blurSmooth = this.backgroundFade = new Timer(500.0F)).start();
    }

    private void updateAutoLayout(float delta) {
        float startX = 15, startY = 15;
        float marginX = 105, marginY = 10;

        for (int col = 0; col < 15; col++) {
            final int currentCol = col;
            List<CategoryComponent> inCol = new ArrayList<>();
            for (CategoryComponent c : categories) {
                int cCol = Math.round((c.getX() - startX) / marginX);
                if (cCol == currentCol) inCol.add(c);
            }
            inCol.sort(Comparator.comparingDouble(CategoryComponent::getY));

            float currentY = startY;
            for (CategoryComponent c : inCol) {
                if (!c.dragging) {
                    c.setY(lerp(c.getY(), currentY, 0.015f * delta), false);
                } else {
                    currentY = c.getY();
                }
                currentY += (c.lastHeight - c.getY()) + marginY;
            }
        }
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
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

        if (configWindow == null) {
            configWindow = new ConfigWindow(actualScreenWidth - 350, actualScreenHeight - 250);
        } else {
            configWindow.refreshLocalConfigs();
        }
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

    @Override
    public void drawScreen(int x, int y, float p) {
        long currentMS = System.currentTimeMillis();
        float delta = currentMS - lastMS;
        lastMS = currentMS;
        if (delta > 50 || delta < 0) delta = 16;

        updateAutoLayout(delta);

        ClickGUI guiModule = (ClickGUI) myau.Myau.moduleManager.modules.get(ClickGUI.class);
        if (guiModule != null && guiModule.blur.getValue()) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(0, 0, this.width, this.height, 0.0f, true, Color.black);
            BlurUtils.blurEnd(2, 3.0f);
        }

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

        if (configWindow != null) {
            configWindow.drawWindow(x, y, delta);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (configWindow != null && configWindow.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }

        List<CategoryComponent> inputOrder = new ArrayList<>(categories);
        inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
        CategoryComponent topmostCategory = null;
        for (CategoryComponent category : inputOrder) {
            if (category.overRect(mouseX, mouseY)) {
                topmostCategory = category;
                break;
            }
        }

        if (topmostCategory != null) topmostCategory.markInteracted();

        if (mouseButton == 0) {
            for (CategoryComponent category : categories) category.overTitle(false);
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
                if (component.onClick(mouseX, mouseY, mouseButton)) break;
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        if (configWindow != null) configWindow.mouseReleased(x, y, button);

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

            if (configWindow != null) configWindow.onScroll(wheelInput, mouseX, mouseY);

            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput);
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (configWindow != null && configWindow.keyTyped(t, k)) return;

        boolean isBinding = binding();

        SearchBarComponent searchBar = null;
        CategoryComponent searchCategory = null;

        for (CategoryComponent category : categories) {
            if (category.category.equalsIgnoreCase("Search")) {
                searchCategory = category;
                if (!category.getModules().isEmpty() && category.getModules().get(0) instanceof SearchBarComponent) {
                    searchBar = (SearchBarComponent) category.getModules().get(0);
                }
                break;
            }
        }

        if (searchBar != null && searchCategory != null) {
            if (searchBar.focused) {
                if (k == Keyboard.KEY_ESCAPE) {
                    searchBar.focused = false;
                    return;
                }
            } else if (!isBinding && k != Keyboard.KEY_ESCAPE && k != Keyboard.KEY_RETURN && k != Keyboard.KEY_BACK) {
                if (String.valueOf(t).matches("[a-zA-Z0-9 ]")) {
                    if (!searchCategory.isOpened()) {
                        searchCategory.mouseClicked(true);
                    }
                    searchBar.focused = true;
                }
            }
        }

        if (k == Keyboard.KEY_ESCAPE) {
            if (!isBinding) {
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
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) {
                    for (Component component : ((ModuleComponent) m).settings) {
                        if (component instanceof BindComponent && ((BindComponent) component).isBinding) return true;
                    }
                }
            }
        }
        return false;
    }

    public void onSliderChange() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) ((ModuleComponent) m).onSliderChange();
            }
        }
    }

    public void requestScaleRefresh() { this.pendingScaleRefresh = true; }
    public static double getActiveRenderScale() { return 1.0D; }
}
