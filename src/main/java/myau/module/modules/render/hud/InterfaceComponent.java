package myau.module.modules.render.hud;

import lombok.Getter;
import lombok.Setter;
import myau.module.Module;
import myau.module.modules.render.HUD;
import myau.util.vector.Vector2d;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public final class InterfaceComponent {

    public Module module;
    public Vector2d position = new Vector2d(5000, 0), targetPosition = new Vector2d(5000, 0);
    public float animationTime;
    public String tag = "";
    public float nameWidth = 0, tagWidth;
    @Setter
    @Getter
    public Color color = Color.WHITE;
    public String translatedName = "";
    public boolean hidden = false;

    public String displayName = "";
    public String displayTag = "";
    public boolean hasTag;

    public float getTotalWidth() {
        return nameWidth + tagWidth;
    }

    public InterfaceComponent(final Module module) {
        this.module = module;
    }

    public boolean shouldDisplay(HUD hudInstance) {
        String name = this.module.getName().toLowerCase();
        if (name.equals("clickgui") || name.equals("gui") || name.equals("hud")) {
            return false;
        }

        switch (hudInstance.modulesToShow.getValue()) {
            case 0:
                return true;

            case 1:
                String category = this.module.getCategory();
                return category == null || !category.equalsIgnoreCase("render");

            case 2:
                return this.module.getKey() != 0 && this.module.getKey() != Keyboard.KEY_NONE;

            default:
                return true;
        }
    }

}