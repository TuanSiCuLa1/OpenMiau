package myau.ui.clickgui;

import myau.module.modules.render.ClickGUI;
import myau.ui.clickgui.components.Component;
import myau.ui.clickgui.components.impl.BindComponent;
import myau.ui.clickgui.components.impl.CategoryComponent;
import myau.ui.clickgui.components.impl.ModuleComponent;
import myau.ui.clickgui.components.impl.SearchBarComponent;
import myau.util.animation.AnimationTimer;
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
    public static float openingScale = 1.0f;
    private AnimationTimer backgroundFade;
    private AnimationTimer blurSmooth;
    private AnimationTimer scaleAnimation = new AnimationTimer(300.0F);
    private ScaledResolution sr;
    public static ArrayList<CategoryComponent> categories;

    private ConfigWindow configWindow;

    private int actualScreenWidth;
    private int actualScreenHeight;
    private boolean pendingScaleRefresh;
    private long lastMS = System.currentTimeMillis();
    private float openingAnimation = 0.0f;

    public ClickGui() {
        categories = new ArrayList<>();
        String[] values = new String[]{"Combat", "Target", "Movement", "Player", "Render", "Themes", "Misc", "Latency", "Search", "Minigames"};

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
        (this.blurSmooth = this.backgroundFade = new AnimationTimer(500.0F)).start();
        (this.blurSmooth = this.backgroundFade = new AnimationTimer(500.0F)).start();
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
        this.scaleAnimation.start();
        ClickGui.openingScale = 0.5f;
        this.sr = new ScaledResolution(mc);
        this.actualScreenWidth = this.sr.getScaledWidth();
        this.actualScreenHeight = this.sr.getScaledHeight();

        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenSize(this.width, this.height);
            categoryComponent.limitPositions();
            categoryComponent.reloadModules();
        }

        // Khởi tạo ConfigWindow nếu chưa có (Để góc dưới cùng bên phải)
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

        openingAnimation = Math.min(1.0f, lerp(openingAnimation, 1.0f, 0.02f * delta));

        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        float ease = 1.0f - (float) Math.pow(1.0f - openingAnimation, 3);
        float scaleFactor = 0.8f + (0.2f * ease);
        int scaledX = (int) (centerX + (x - centerX) / scaleFactor);
        int scaledY = (int) (centerY + (y - centerY) / scaleFactor);

        updateAutoLayout(delta);

        ClickGui.openingScale = this.scaleAnimation.getValueFloat(0.5f, 1.0f, 2);

        myau.module.modules.render.HUD hudModule = (myau.module.modules.render.HUD) myau.Myau.moduleManager.modules.get(myau.module.modules.render.HUD.class);
        ClickGUI guiModule = (ClickGUI) myau.Myau.moduleManager.modules.get(ClickGUI.class);
        
        boolean useBlur = (guiModule != null && guiModule.blur.getValue()) || (hudModule != null && hudModule.shaders.getValue());

        if (useBlur) {
            float passes = (hudModule != null && hudModule.blurSettings.getValue()) ? hudModule.blurCompression.getValue() : 5.0f;
            float radius = (hudModule != null && hudModule.blurSettings.getValue()) ? hudModule.blurRadius.getValue() : 25.0f;
            myau.util.shader.RenderSystem.renderBlur(radius, passes, () -> {
                RoundedUtils.drawRound(0, 0, this.width, this.height, 0.0f, true, Color.black);
            });
        }

        int bgColorAlpha = (int) (130 * this.scaleAnimation.getValueFloat(0.0f, 1.0f, 2));
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, bgColorAlpha).getRGB());

        List<CategoryComponent> renderOrder = getCategoriesInRenderOrder();
        CategoryComponent topmostUnderCursor = getTopmostUnderCursor(renderOrder, scaledX, scaledY);

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GL11.glScaled(scaleFactor, scaleFactor, 1.0);
        GL11.glTranslatef(-centerX, -centerY, 0);

        if (hudModule != null && hudModule.shaders.getValue()) {
            myau.util.shader.RenderSystem.renderBloom(hudModule.bloomRadius.getValue().floatValue(), hudModule.bloomCompression.getValue().floatValue(), () -> {
                for (CategoryComponent c : renderOrder) {
                    c.render(this.fontRendererObj);
                    c.mousePosition(scaledX, scaledY, c == topmostUnderCursor);

                    for (Component m : c.getModules()) {
                        m.drawScreen(scaledX, scaledY);
                    }
                }
                if (configWindow != null) {
                    configWindow.drawWindow(scaledX, scaledY, 0);
                }
            });
        }

        for (CategoryComponent c : renderOrder) {
            c.render(this.fontRendererObj);
            c.mousePosition(scaledX, scaledY, c == topmostUnderCursor);

            for (Component m : c.getModules()) {
                m.drawScreen(scaledX, scaledY);
            }
        }
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        // Vẽ Config Window
        if (configWindow != null) {
            configWindow.drawWindow(scaledX, scaledY, delta);
        }

        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        float ease = 1.0f - (float) Math.pow(1.0f - openingAnimation, 3);
        float scaleFactor = 0.8f + (0.2f * ease);
        int scaledX = (int) (centerX + (mouseX - centerX) / scaleFactor);
        int scaledY = (int) (centerY + (mouseY - centerY) / scaleFactor);

        if (configWindow != null && configWindow.mouseClicked(scaledX, scaledY, mouseButton)) {
            return;
        }

        List<CategoryComponent> inputOrder = new ArrayList<>(categories);
        inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
        CategoryComponent topmostCategory = null;
        for (CategoryComponent category : inputOrder) {
            if (category.overRect(scaledX, scaledY)) {
                topmostCategory = category;
                break;
            }
        }

        if (topmostCategory != null) topmostCategory.markInteracted();

        if (mouseButton == 0) {
            for (CategoryComponent category : categories) category.overTitle(false);
            if (topmostCategory != null && topmostCategory.draggable(scaledX, scaledY)) {
                topmostCategory.overTitle(true);
                topmostCategory.xx = scaledX - topmostCategory.getX();
                topmostCategory.yy = scaledY - topmostCategory.getY();
                topmostCategory.dragging = true;
            }
        }

        if (mouseButton == 1 && topmostCategory != null && topmostCategory.overTitle(scaledX, scaledY)) {
            topmostCategory.mouseClicked(!topmostCategory.isOpened());
        }

        if (topmostCategory != null && topmostCategory.isOpened() && !topmostCategory.getModules().isEmpty() && !topmostCategory.overTitle(scaledX, scaledY)) {
            for (Component component : topmostCategory.getModules()) {
                if (component.onClick(scaledX, scaledY, mouseButton)) break;
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        if (openingAnimation < 0.95f) return;

        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        float ease = 1.0f - (float) Math.pow(1.0f - openingAnimation, 3);
        float scaleFactor = 0.8f + (0.2f * ease);
        int scaledX = (int) (centerX + (x - centerX) / scaleFactor);
        int scaledY = (int) (centerY + (y - centerY) / scaleFactor);

        if (configWindow != null) configWindow.mouseReleased(scaledX, scaledY, button);

        if (button == 0) {
            for (CategoryComponent category : categories) {
                category.overTitle(false);
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.mouseReleased(scaledX, scaledY, button);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (openingAnimation < 0.95f) return;

        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            int mouseX = Mouse.getEventX() * this.width / mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;

            float centerX = this.width / 2.0f;
            float centerY = this.height / 2.0f;
            float ease = 1.0f - (float) Math.pow(1.0f - openingAnimation, 3);
            float scaleFactor = 0.8f + (0.2f * ease);
            int scaledX = (int) (centerX + (mouseX - centerX) / scaleFactor);
            int scaledY = (int) (centerY + (mouseY - centerY) / scaleFactor);

            if (configWindow != null) configWindow.onScroll(wheelInput, scaledX, scaledY);

            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput);
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        // 1. Chặn phím tắt nếu đang gõ chữ trong ConfigWindow
        if (configWindow != null && configWindow.keyTyped(t, k)) return;

        // Kiểm tra xem người dùng có đang cài Keybind cho module nào không
        boolean isBinding = binding();

        SearchBarComponent searchBar = null;
        CategoryComponent searchCategory = null;

        // Tìm Category Search và Component SearchBar
        for (CategoryComponent category : categories) {
            if (category.category.equalsIgnoreCase("Search")) {
                searchCategory = category;
                if (!category.getModules().isEmpty() && category.getModules().get(0) instanceof SearchBarComponent) {
                    searchBar = (SearchBarComponent) category.getModules().get(0);
                }
                break;
            }
        }

        // 2. Logic Xử lý phím cho Search
        if (searchBar != null && searchCategory != null) {
            if (searchBar.focused) {
                // Nếu đang gõ Search mà bấm ESC -> Thoát khỏi chế độ gõ (không đóng GUI)
                if (k == Keyboard.KEY_ESCAPE) {
                    searchBar.focused = false;
                    return;
                }
            } else if (!isBinding && k != Keyboard.KEY_ESCAPE && k != Keyboard.KEY_RETURN && k != Keyboard.KEY_BACK) {
                // TỰ ĐỘNG BẮT PHÍM (Giống Rise Client)
                // Nếu gõ chữ cái, số, hoặc khoảng trắng -> Tự động chuyển qua Search
                if (String.valueOf(t).matches("[a-zA-Z0-9 ]")) {
                    // Mở xổ Panel Search ra nếu nó đang bị thu gọn
                    if (!searchCategory.isOpened()) {
                        searchCategory.mouseClicked(true);
                    }
                    // Bật focus để nó nhận chữ ngay lập tức
                    searchBar.focused = true;
                    // (Phím vừa gõ sẽ được truyền tiếp xuống vòng lặp bên dưới để add vào ô Search)
                }
            }
        }

        // 3. Xử lý đóng ClickGUI bằng ESC
        if (k == Keyboard.KEY_ESCAPE) {
            if (!isBinding) {
                this.mc.displayGuiScreen(null);
                return;
            }
        }

        // 4. Truyền phím xuống cho toàn bộ các module/setting/searchbar
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