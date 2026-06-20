package myau.util.font.impl.rise;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public class FontUtil {

    private static final IResourceManager RESOURCE_MANAGER = Minecraft.getMinecraft().getResourceManager();

    public static Font getResource(final String resource, final int size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, RESOURCE_MANAGER.getResource(new ResourceLocation(resource)).getInputStream()).deriveFont((float) size);
        } catch (final FontFormatException | IOException ignored) {
            return null;
        }
    }

    public static Font getDiskResource(final String resource, final int size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File(resource)).deriveFont((float) size);
        } catch (final FontFormatException | IOException ignored) {
            return null;
        }
    }
}


