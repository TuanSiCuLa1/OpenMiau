package myau.util.render;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ColorUtils {
    public static void glColor(int color) {
        float alpha = (float) (color >> 24 & 255) / 255F;
        float red = (float) (color >> 16 & 255) / 255F;
        float green = (float) (color >> 8 & 255) / 255F;
        float blue = (float) (color & 255) / 255F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int[] getRGBA(int color) {
        return new int[]{
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF,
                (color >> 24) & 0xFF
        };
    }
}
