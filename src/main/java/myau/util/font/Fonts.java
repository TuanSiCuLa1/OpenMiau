package myau.util.font;

import myau.util.font.impl.minecraft.MinecraftFontRenderer;
import myau.util.font.impl.rise.FontRenderer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public enum Fonts {
    MAIN("Rise %s", "ttf"),
    RAVEN("Sf-Regular", "ttf"), 
    MINECRAFT("Minecraft", () -> new MinecraftFontRenderer()),
    CUSTOM("", "ttf");

    Fonts(String name, String extension) {
        this.name = name;
        this.extention = extension;
    }

    Fonts(String name, Supplier<Font> get) {
        this.name = name;
        this.extention = "";
        this.font = get.get();
        this.get = get;
    }

    Supplier<Font> get;
    Font font;
    String name;
    final String extention;
    private final HashMap<Integer, Font> sizes = new HashMap<>();

    public Font get(int size) {
        return get(size, Weight.NONE);
    }

    public Font get() {
        if (get == null) return MINECRAFT.get();
        return get(0, Weight.NONE);
    }

    public Font get(int size, Weight weight) {
        if (get != null) {
            if (font == null) font = get.get();
            return font;
        }

        int key = Integer.parseInt(size + "" + weight.getNum());

        if (!sizes.containsKey(key)) {
            java.awt.Font awtFont = null;
            String location = "unknown";

            try {
                if (this == CUSTOM) {
                    location = this.name;
                    awtFont = new java.awt.Font(this.name, java.awt.Font.PLAIN, size);
                } 
                else if (name.contains(":")) {
                    location = name;
                    awtFont = FontUtil.getDiskResource(location, size);
                } 
                else {
                    for (String alias : weight.getAliases()) {
                        String fontName = String.format(name, alias).trim();
                        location = "keystrokesmod:fonts/" + fontName + "." + extention;
                        awtFont = FontUtil.getResource(location, size);

                        if (awtFont != null) break;
                    }
                    if (awtFont == null) {
                        String fallbackName = String.format(name, "").trim();
                        location = "keystrokesmod:fonts/" + fallbackName + "." + extention;
                        awtFont = FontUtil.getResource(location, size);
                    }
                }

                if (awtFont != null) {
                    sizes.put(key, new FontRenderer(awtFont, true, true, false));
                } else {
                    return MINECRAFT.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return MINECRAFT.get();
            }
        }

        return sizes.get(key);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ArrayList<String> getFontPaths() {
        ArrayList<String> fontPaths = new ArrayList<>();
        addFontPaths(fontPaths, "C:\\Windows\\Fonts");
        addFontPaths(fontPaths, System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Windows\\Fonts");
        return fontPaths;
    }

    private static void addFontPaths(ArrayList<String> fontPaths, String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".ttf")) {
                        fontPaths.add(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        addFontPaths(fontPaths, file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
