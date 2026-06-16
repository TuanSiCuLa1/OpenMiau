package myau.util.font;

public class FontManager {
    private static final String[] HUD_FONT_OPTIONS = new String[]{
            "Minecraft", "Rise", "Raven", "Arial", "Tahoma", "Impact" 
    };

    public static String[] getHudFontOptions() {
        return HUD_FONT_OPTIONS;
    }

    public static Font getHudRenderer(String fontName, float scale) {
        return getFontForName(fontName, Math.round(18 * scale));
    }

    public static Font getNametagRenderer(String text) {
        return getFontForName("Rise", 18); 
    }

    public static Font getClickGuiHeaderRenderer(String fontName) {
        return Fonts.MINECRAFT.get(24);
    }

    public static Font getClickGuiSettingRenderer(String fontName) {
        return Fonts.MINECRAFT.get(18);
    }

    private static Font getFontForName(String fontName, int size) {
        if (fontName == null || fontName.equalsIgnoreCase("Minecraft")) {
            return Fonts.MINECRAFT.get(Math.max(1, size));
            
        } else if (fontName.equalsIgnoreCase("Rise") || fontName.equalsIgnoreCase("Main")) {
            return Fonts.MAIN.get(Math.max(1, size));
            
        } else if (fontName.equalsIgnoreCase("Raven") || fontName.equalsIgnoreCase("Sf-Regular")) {
            return Fonts.RAVEN.get(Math.max(1, size));
            
        } else {
            Fonts.CUSTOM.setName(fontName);
            return Fonts.CUSTOM.get(Math.max(1, size));
        }
    }

    public static Font getFont(int size) {
        return Fonts.MINECRAFT.get(size);
    }

    public static Font getFont(String name, int size) {
        return getFontForName(name, size);
    }
}
