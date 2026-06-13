package myau.util.font;

public class FontManager {
    // Đây là danh sách các Font sẽ hiện trong ClickGUI để người chơi chọn
    private static final String[] HUD_FONT_OPTIONS = new String[]{
            "Minecraft", "Rise", "Raven", "Arial", "Tahoma", "Impact" 
    };

    public static String[] getHudFontOptions() {
        return HUD_FONT_OPTIONS;
    }

    // ========================================================
    // KHU VỰC HUD (Cho phép chọn Font cũ, mới, hoặc Custom)
    // ========================================================
    public static Font getHudRenderer(String fontName, float scale) {
        return getFontForName(fontName, Math.round(18 * scale));
    }

    public static Font getNametagRenderer(String text) {
        return getFontForName("Rise", 18); // Cố định Nametag là font Rise cho đẹp
    }

    // ========================================================
    // KHU VỰC CLICKGUI (Vẫn ép dùng Minecraft Pixel để không bị lệch khung)
    // ========================================================
    public static Font getClickGuiHeaderRenderer(String fontName) {
        return Fonts.MINECRAFT.get(24);
    }

    public static Font getClickGuiSettingRenderer(String fontName) {
        return Fonts.MINECRAFT.get(18);
    }

    // ========================================================
    // LÕI ĐIỀU PHỐI (CORE ROUTING)
    // ========================================================
    private static Font getFontForName(String fontName, int size) {
        if (fontName == null || fontName.equalsIgnoreCase("Minecraft")) {
            return Fonts.MINECRAFT.get(Math.max(1, size));
            
        } else if (fontName.equalsIgnoreCase("Rise") || fontName.equalsIgnoreCase("Main")) {
            return Fonts.MAIN.get(Math.max(1, size));
            
        } else if (fontName.equalsIgnoreCase("Raven") || fontName.equalsIgnoreCase("Sf-Regular")) {
            return Fonts.RAVEN.get(Math.max(1, size));
            
        } else {
            // TÍNH NĂNG CUSTOM FONT: Nếu tên không phải 3 cái trên (VD: "Arial", "Tahoma")
            // Hệ thống sẽ lấy trực tiếp Font đó từ Windows của người chơi.
            Fonts.CUSTOM.setName(fontName);
            return Fonts.CUSTOM.get(Math.max(1, size));
        }
    }

    // Các hàm tương thích ngược với code cũ
    public static Font getFont(int size) {
        return Fonts.MINECRAFT.get(size);
    }

    public static Font getFont(String name, int size) {
        return getFontForName(name, size);
    }
}
