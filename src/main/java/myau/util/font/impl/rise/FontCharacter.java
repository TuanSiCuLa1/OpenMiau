package myau.util.font.impl.rise;

import org.lwjgl.opengl.GL11;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public class FontCharacter {
    private final int texture;
    private final int width;
    private final int height;

    public FontCharacter(final int texture, final int width, final int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public void render(final float x, final float y) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(x, y + this.height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(x + this.width, y + this.height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(x + this.width, y);
        GL11.glEnd();
    }

    public int getTexture() {
        return this.texture;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
