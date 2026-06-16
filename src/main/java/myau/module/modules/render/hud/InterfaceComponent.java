package myau.module.modules.render.hud;

import lombok.Setter;
import myau.module.Module;
import myau.module.modules.render.HUD;
import myau.util.Themes;
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
    public Color color = Color.WHITE; // Đã bỏ @Getter ở đây

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

    // 👉 100% RISE 6 LOGIC: Tự động lấy màu Gradient dựa theo toạ độ Y của Component
    public Color getColor() {
        // Lấy theme hiện tại
        Themes theme = Themes.getCurrentTheme();
        HUD hud = (HUD) myau.Myau.moduleManager.modules.get(HUD.class);

        // Kiểm tra xem HUD đang chọn mode màu gì (Static hay Fade)
        if (hud != null && hud.colorAnimation.getValue() == 1) { // 1 = FADE
            // Ép cứng X = 0, chỉ cho Y chạy để tạo đường sóng (Wave) trượt dọc hoàn hảo
            return theme.getAccentColor(new Vector2d(0, this.position.getY()));
        } else if (hud != null && hud.colorAnimation.getValue() == 2) { // 2 = RAINBOW
            return myau.util.ColorUtil.rainbow((int) (this.position.getY() * 2 + System.currentTimeMillis() / 10));
        }

        // STATIC (Mặc định)
        return theme.getFirstColor();
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