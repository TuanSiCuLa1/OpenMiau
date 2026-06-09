package myau.module.modules;

import myau.event.EventTarget;
import myau.events.LeftClickMouseEvent;
import myau.events.Render2DEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

public class Keystrokes extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    public final IntProperty x = new IntProperty("x", 6, 0, 1000);
    public final IntProperty y = new IntProperty("y", 18, 0, 1000);
    public final IntProperty scale = new IntProperty("scale", 100, 50, 200);
    public final IntProperty opacity = new IntProperty("opacity", 102, 20, 255);
    public final BooleanProperty centerY = new BooleanProperty("center-y", true);
    public final BooleanProperty showMouse = new BooleanProperty("mouse-buttons", true);
    public final BooleanProperty showCPS = new BooleanProperty("cps", true, () -> this.showMouse.getValue());

    public Keystrokes() {
        super("Keystrokes", false);
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        recordLeftClick();
    }

    public static void recordLeftClick() {
        MyauKeystrokesHolder.INSTANCE.leftClicks.addLast(System.currentTimeMillis());
    }

    private static class MyauKeystrokesHolder {
        private static final Keystrokes INSTANCE = (Keystrokes) myau.Myau.moduleManager.modules.get(Keystrokes.class);
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        rightClicks.addLast(System.currentTimeMillis());
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        long now = System.currentTimeMillis();
        prune(leftClicks, now);
        prune(rightClicks, now);

        ScaledResolution sr = new ScaledResolution(mc);
        float scaleValue = this.scale.getValue() / 100.0F;
        int baseX = this.x.getValue();
        int baseY = this.centerY.getValue() ? sr.getScaledHeight() / 2 - this.y.getValue() : this.y.getValue();

        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleValue, scaleValue, 1.0F);
        baseX = (int) (baseX / scaleValue);
        baseY = (int) (baseY / scaleValue);

        drawKey("W", baseX + 24, baseY, 22, 22, Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        drawKey("A", baseX, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
        drawKey("S", baseX + 24, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
        drawKey("D", baseX + 48, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
        if (this.showMouse.getValue()) {
            drawMouse("LMB", leftClicks.size(), baseX, baseY + 48, 34, 22, Mouse.isButtonDown(0));
            drawMouse("RMB", rightClicks.size(), baseX + 36, baseY + 48, 34, 22, Mouse.isButtonDown(1));
        }
        GlStateManager.popMatrix();
    }

    private void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.removeFirst();
        }
    }

    private int background(boolean down) {
        int alpha = down ? 204 : this.opacity.getValue();
        return (alpha << 24) | (down ? 0xFFFFFF : 0x000000);
    }

    private void drawKey(String label, int x, int y, int w, int h, boolean down) {
        int fg = down ? 0xFF111111 : 0xFFFFFFFF;
        Gui.drawRect(x, y, x + w, y + h, background(down));
        mc.fontRendererObj.drawStringWithShadow(label, x + w / 2 - mc.fontRendererObj.getStringWidth(label) / 2, y + 7, fg);
    }

    private void drawMouse(String label, int cps, int x, int y, int w, int h, boolean down) {
        int fg = down ? 0xFF111111 : 0xFFFFFFFF;
        Gui.drawRect(x, y, x + w, y + h, background(down));
        int labelY = this.showCPS.getValue() ? y + 3 : y + 7;
        mc.fontRendererObj.drawStringWithShadow(label, x + w / 2 - mc.fontRendererObj.getStringWidth(label) / 2, labelY, fg);
        if (this.showCPS.getValue()) {
            String cpsText = cps + " CPS";
            mc.fontRendererObj.drawStringWithShadow(cpsText, x + w / 2 - mc.fontRendererObj.getStringWidth(cpsText) / 2, y + 12, fg);
        }
    }
}
