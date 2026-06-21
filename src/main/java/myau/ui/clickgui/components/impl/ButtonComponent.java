package myau.ui.clickgui.components.impl;

import myau.ui.clickgui.components.Component;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.font.Font;
import myau.util.font.Fonts;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ButtonComponent extends Component {
    private static final int ENABLED_COLOR = new Color(20, 255, 0).getRGB();

    private Module mod;
    public BooleanProperty property;
    private ModuleComponent moduleComponent;

    public float o;
    public float x;
    private float y;
    public float xOffset;

    public ButtonComponent(Module mod, BooleanProperty op, ModuleComponent b, float o) {
        this.mod = mod;
        this.property = op;
        this.moduleComponent = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.yPos;
        this.o = o;
    }

    public void render() {
        Font renderer = Fonts.MINECRAFT.get(18); 
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        renderer.draw((this.property.getValue() ? "[+]  " : "[-]  ") + this.property.getName(), (float) ((this.moduleComponent.categoryComponent.getX() + 4) * 2) + xOffset, (float) ((this.moduleComponent.categoryComponent.getY() + this.o + 4) * 2), this.property.getValue() ? ENABLED_COLOR : -1, false);
        GL11.glScaled(1, 1, 1);
        GL11.glPopMatrix();
    }

    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.property.isVisible();
    }

    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    public boolean onClick(int x, int y, int b) {
        if (this.i(x, y) && b == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            this.property.setValue(!this.property.getValue());
            this.moduleComponent.reloadSettings();
        }
        return false;
    }

    public boolean i(int x, int y) {
        return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y && y < this.y + 11;
    }
}
