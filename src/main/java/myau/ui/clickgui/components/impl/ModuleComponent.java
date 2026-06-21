package myau.ui.clickgui.components.impl;

import myau.ui.clickgui.ClickGui;
import myau.ui.clickgui.components.Component;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;
import myau.util.render.RenderUtil;
import myau.util.animation.AnimationTimer;
import myau.util.font.Font;
import myau.util.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModuleComponent extends Component {
    public Module mod;
    public CategoryComponent categoryComponent;
    public float yPos;
    public ArrayList<Component> settings;
    public boolean isOpened;
    private boolean hovering;
    private AnimationTimer hoverTimer;
    private boolean hoverStarted;
    private AnimationTimer smoothTimer;
    private float smoothingY = 16f;
    private float animationStartY = 16f;
    private float animationTargetY = 16f;

    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer(16);
    private static final int ORIGINAL_HOVER_ALPHA = 120;
    private static final int HOVER_COLOR = new Color(0, 0, 0, ORIGINAL_HOVER_ALPHA).getRGB();
    private static final int ENABLED_COLOR = new Color(24, 154, 255).getRGB();
    private static final int DISABLED_COLOR = new Color(192, 192, 192).getRGB();
    private final boolean categoryManager;

    public ModuleComponent(Module mod, CategoryComponent p, float yPos) {
        this(mod, p, yPos, false);
    }

    public ModuleComponent(Module mod, CategoryComponent p, float yPos, boolean categoryManager) {
        this.mod = mod;
        this.categoryComponent = p;
        this.yPos = yPos;
        this.settings = new ArrayList<>();
        this.categoryManager = categoryManager;
        this.isOpened = categoryManager;
        float collapsedHeight = getCollapsedHeight();
        this.smoothingY = collapsedHeight;
        this.animationStartY = collapsedHeight;
        this.animationTargetY = collapsedHeight;
        rebuildSettingsList();
    }

    private void rebuildSettingsList() {
        this.settings = new ArrayList<>();
        float y = yPos + getSettingStartOffset();
        if (mod != null) {
            ArrayList<Property<?>> props = myau.Myau.propertyManager.properties.get(mod.getClass());
            if (props != null) {
                for (Property<?> v : props) {
                    if (!v.isVisible()) {
                        continue;
                    }
                    if (v instanceof BooleanProperty) {
                        ButtonComponent c = new ButtonComponent(mod, (BooleanProperty) v, this, y);
                        this.settings.add(c);
                        y += 12;
                    }
                    else if (v instanceof FloatProperty || v instanceof IntProperty || v instanceof PercentProperty || v instanceof ModeProperty) {
                        SliderComponent s = new SliderComponent(v, this, y);
                        this.settings.add(s);
                        y += 12;
                    }
                    else if (v instanceof ColorProperty) {
                        ColorComponent cc = new ColorComponent((ColorProperty) v, this, y);
                        this.settings.add(cc);
                        y += 12;
                    }
                }
            }
        }
        if (!categoryManager) {
            this.settings.add(new BindComponent(this, y));
        }
    }

    public void reloadSettings() {
        boolean wasOpened = this.isOpened;
        Map<Property<?>, Boolean> sliderHeldStates = new HashMap<>();
        Map<Property<?>, Boolean> colorExpandedStates = new HashMap<>();

        for (Component component : this.settings) {
            if (component instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent) component;
                sliderHeldStates.put(sliderComponent.property, sliderComponent.heldDown);
            }
            else if (component instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent) component;
                colorExpandedStates.put(colorComponent.property, colorComponent.expanded);
            }
        }

        rebuildSettingsList();
        for (Component component : this.settings) {
            if (component instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent) component;
                Boolean wasHeldDown = sliderHeldStates.get(sliderComponent.property);
                if (wasHeldDown != null) {
                    sliderComponent.heldDown = wasHeldDown;
                }
            }
            else if (component instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent) component;
                Boolean wasExpanded = colorExpandedStates.get(colorComponent.property);
                if (wasExpanded != null) {
                    colorComponent.restoreExpandedState(wasExpanded);
                }
            }
        }
        restoreOpenState(wasOpened);
        updateSettingPositions();
    }

    public void restoreOpenState(boolean opened) {
        this.isOpened = categoryManager || opened;
        this.smoothTimer = null;
        float height = this.isOpened ? getHeightF() : getCollapsedHeight();
        this.smoothingY = height;
        this.animationStartY = height;
        this.animationTargetY = height;
    }

    public void updateAnimationState() {
        if (smoothTimer != null) {
            if (System.currentTimeMillis() - smoothTimer.last >= 280) {
                smoothTimer = null;
                smoothingY = animationTargetY;
                animationStartY = animationTargetY;
            } else {
                smoothingY = smoothTimer.getValueFloat(animationStartY, animationTargetY, 1);
                if (smoothingY == animationTargetY) {
                    smoothTimer = null;
                    animationStartY = animationTargetY;
                }
            }
        }
    }

    public void updateHeight(float newY) {
        this.yPos = newY;
        float y = this.yPos + getCollapsedHeight();
        for (Component co : this.settings) {
            if (!isVisibleBase(co)) {
                continue;
            }
            co.updateHeight(y);
            if (co instanceof SliderComponent) {
                ((SliderComponent) co).xOffset = 0;
            } else if (co instanceof ButtonComponent) {
                ((ButtonComponent) co).xOffset = 0;
            } else if (co instanceof BindComponent) {
                ((BindComponent) co).xOffset = 0;
            } else if (co instanceof ColorComponent) {
                ((ColorComponent) co).xOffset = 0;
            }
            y += getBaseComponentHeightF(co);
        }
    }

    public void render() {
        if (hasModuleHeader() && (hovering || hoverTimer != null)) {
            double hoverAlpha = (hovering && hoverTimer != null) ? hoverTimer.getValueFloat(0, ORIGINAL_HOVER_ALPHA, 1) : (hoverTimer != null && !hovering) ? ORIGINAL_HOVER_ALPHA - hoverTimer.getValueFloat(0, ORIGINAL_HOVER_ALPHA, 1) : ORIGINAL_HOVER_ALPHA;
            if (hoverAlpha == 0) {
                hoverTimer = null;
            }
            RenderUtil.drawRoundedRectangle(this.categoryComponent.getX(), this.categoryComponent.getY() + yPos, this.categoryComponent.getX() + this.categoryComponent.getWidth(), this.categoryComponent.getY() + 16 + this.yPos, 8, RenderUtil.setAlpha(HOVER_COLOR, hoverAlpha / 255.0));
        }
        int button_rgb = this.mod != null && this.mod.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;

        Font titleRenderer = Fonts.MINECRAFT.get(24);

        if (hasModuleHeader()) {
            float textX = this.categoryComponent.getX() + this.categoryComponent.getWidth() / 2.0f - titleRenderer.width(this.mod.getName()) / 2.0f;
            float textY = this.categoryComponent.getY() + this.yPos + 4;
            titleRenderer.draw(this.mod.getName(), textX, textY, button_rgb, true);
        }
        boolean scissorRequired = smoothTimer != null;
        if (scissorRequired) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int scale = sr.getScaleFactor();
            double guiScale = ClickGui.getActiveRenderScale();
            float scrollOffset = this.categoryComponent.getModuleY() - this.categoryComponent.getY();
            int scissorX = (int) Math.floor((this.categoryComponent.getX() - 2) * guiScale * scale);
            int scissorY = (int) Math.floor((sr.getScaledHeight() - ((this.categoryComponent.getY() + this.yPos + smoothingY + scrollOffset) * guiScale)) * scale);
            int scissorW = (int) Math.ceil((this.categoryComponent.getWidth() + 4) * guiScale * scale);
            int scissorH = (int) Math.ceil(smoothingY * guiScale * scale);
            pushScissor(scissorX, scissorY, scissorW, scissorH);
        }

        if (this.isOpened || smoothTimer != null) {
            renderSettings();
        }

        if (scissorRequired) {
            popScissor();
        }
    }

    private void renderSettings() {
        for (Component c : settings) {
            if (isVisibleBase(c)) {
                c.render();
            }
        }
    }

    /** Float height used for all layout/scroll decisions. */
    @Override
    public float getHeightF() {
        if (smoothTimer != null) {
            return smoothingY;
        }
        if (!this.isOpened) {
            return getCollapsedHeight();
        }
        float h = getCollapsedHeight();
        for (Component c : this.settings) {
            h += getAnimatedComponentHeightF(c);
        }
        return h;
    }

    @Override
    public int getHeight() {
        return Math.round(getHeightF());
    }

    public void onSliderChange() {
        for (Component c : this.settings) {
            if (c instanceof SliderComponent) {
                ((SliderComponent) c).onSliderChange();
            }
        }
    }

    public float getScrollExtentHeightF() {
        if (isOpened || (smoothTimer != null && animationTargetY > 16f)) {
            float h = getCollapsedHeight();
            for (Component c : settings) {
                if (!isVisibleBase(c)) continue;
                h += getBaseComponentHeightF(c);
            }
            return h;
        }
        return getHeightF();
    }

    public void drawScreen(int x, int y) {
        for (Component c : this.settings) {
            c.drawScreen(x, y);
        }
        if (hasModuleHeader() && overModuleName(x, y) && this.categoryComponent.opened) {
            hovering = true;
            if (hoverTimer == null) {
                (hoverTimer = new AnimationTimer(75)).start();
                hoverStarted = true;
            }
        }
        else {
            if (hovering && hoverStarted) {
                (hoverTimer = new AnimationTimer(75)).start();
            }
            hoverStarted = false;
            hovering = false;
        }
    }

    public boolean onClick(int x, int y, int mouse) {
        if (hasModuleHeader() && this.overModuleName(x, y) && mouse == 0) {
            this.mod.toggle();
            return true;
        }

        if (hasModuleHeader() && this.overModuleName(x, y) && mouse == 1) {
            float currentHeight = smoothTimer != null ? smoothingY : (isOpened ? getHeightF() : 16f);
            this.animationStartY = currentHeight;
            this.isOpened = !this.isOpened;
            float targetHeight;
            if (this.isOpened) {
                float h = getCollapsedHeight();
                for (Component c : this.settings) {
                    h += getAnimatedComponentHeightF(c);
                }
                targetHeight = h;
            } else {
                targetHeight = getCollapsedHeight();
            }
            this.animationTargetY = targetHeight;
            (this.smoothTimer = new AnimationTimer(250)).start();
            return true;
        }

        for (Component settingComponent : this.settings) {
            if (settingComponent.onClick(x, y, mouse)) {
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(int x, int y, int m) {
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }
    }

    public void keyTyped(char t, int k) {
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    public void onScroll(int scroll) {
        for (Component component : this.settings) {
            component.onScroll(scroll);
        }
    }

    public void onGuiClosed() {
        for (Component c : this.settings) {
            c.onGuiClosed();
        }
        smoothTimer = null;
        hoverTimer = null;
        float finalHeight = isOpened ? getHeightF() : getCollapsedHeight();
        smoothingY = finalHeight;
        animationStartY = finalHeight;
        animationTargetY = finalHeight;
    }

    public boolean overModuleName(int x, int y) {
        if (!hasModuleHeader()) {
            return false;
        }
        return x > this.categoryComponent.getX() && x < this.categoryComponent.getX() + this.categoryComponent.getWidth() && y > this.categoryComponent.getModuleY() + this.yPos && y < this.categoryComponent.getModuleY() + 16 + this.yPos;
    }

    public void updateSettingPositions() {
        this.categoryComponent.updateHeight();
    }

    public boolean isVisible(Component component) {
        return isVisibleBase(component);
    }

    private float getBaseComponentHeightF(Component component) {
        if (component instanceof SliderComponent) {
            return 16f;
        }
        if (component instanceof ColorComponent) {
            ColorComponent cc = (ColorComponent) component;
            float progress = cc.getAnimationProgress();
            return 12f + (cc.getExpandedHeight() - 12f) * progress;
        }
        return 12f;
    }

    private float getAnimatedComponentHeightF(Component component) {
        if (!isVisibleBase(component)) {
            return 0f;
        }
        return getBaseComponentHeightF(component);
    }

    private static final int MAX_SCISSOR_DEPTH = 4;
    private final int[][] scissorStack = new int[MAX_SCISSOR_DEPTH][5];
    private int scissorDepth = 0;

    private void pushScissor(int x, int y, int w, int h) {
        boolean wasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] saved = scissorStack[scissorDepth++];
        if (wasEnabled) {
            SCISSOR_BOX.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BOX);
            saved[0] = 1;
            saved[1] = SCISSOR_BOX.get(0);
            saved[2] = SCISSOR_BOX.get(1);
            saved[3] = SCISSOR_BOX.get(2);
            saved[4] = SCISSOR_BOX.get(3);
            int ix = Math.max(saved[1], x);
            int iy = Math.max(saved[2], y);
            int iw = Math.max(0, Math.min(saved[1] + saved[3], x + w) - ix);
            int ih = Math.max(0, Math.min(saved[2] + saved[4], y + h) - iy);
            GL11.glScissor(ix, iy, iw, ih);
        } else {
            saved[0] = 0;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x, y, w, h);
        }
    }

    private void popScissor() {
        int[] saved = scissorStack[--scissorDepth];
        if (saved[0] == 1) {
            GL11.glScissor(saved[1], saved[2], saved[3], saved[4]);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private boolean isVisibleBase(Component component) {
        return component.isBaseVisible();
    }

    private boolean hasModuleHeader() {
        return !categoryManager;
    }

    private float getCollapsedHeight() {
        return hasModuleHeader() ? 16f : 0f;
    }

    private float getSettingStartOffset() {
        return hasModuleHeader() ? 12f : 0f;
    }
}
