package myau.util.font;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FontUtil {

    public static java.awt.Font getResource(final String resource, final int size) {
        try {
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(resource)).getInputStream()).deriveFont((float) size);
        } catch (final FontFormatException | IOException ignored) {
            return null;
        }
    }

    public static java.awt.Font getDiskResource(final String resource, final int size) {
        try {
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File(resource)).deriveFont((float) size);
        } catch (final FontFormatException | IOException ignored) {
            return null;
        }
    }
}
