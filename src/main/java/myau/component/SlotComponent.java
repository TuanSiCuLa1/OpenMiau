package myau.component;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.UpdateEvent;
import myau.events.Render2DEvent;
import myau.util.font.Fonts;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import java.awt.Color;

public class SlotComponent {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean alternativeSlot = false;
    public static int alternativeCurrentItem = 0;
    private static boolean render = true;

    private static float animationProgress = 0f;
    private static long lastFrame = System.currentTimeMillis();

    public static void setSlot(int slot, boolean renderEffect) {
        if (slot < 0 || slot >= 9) return;
        alternativeCurrentItem = slot;
        alternativeSlot = true;
        render = renderEffect;
        
        if (mc.thePlayer != null && mc.playerController != null) {
            ((myau.mixin.IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        }
    }

    public static int getItemIndex() {
        return alternativeSlot ? alternativeCurrentItem : mc.thePlayer.inventory.currentItem;
    }

    public static ItemStack getItemStack() {
        if (mc.thePlayer == null || mc.thePlayer.inventoryContainer == null) return null;
        return mc.thePlayer.inventoryContainer.getSlot(getItemIndex() + 36).getStack();
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && mc.thePlayer != null) {
            alternativeSlot = false;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null) return;

        long currentFrame = System.currentTimeMillis();
        float delta = (currentFrame - lastFrame) / 1000f;
        lastFrame = currentFrame;

        ItemStack itemStack = getItemStack();
        boolean isBlock = itemStack != null && itemStack.getItem() instanceof ItemBlock;
        boolean shouldShow = alternativeSlot && isBlock && render;

        float target = shouldShow ? 1f : 0f;
        animationProgress += (target - animationProgress) * 12f * delta;
        animationProgress = Math.max(0f, Math.min(1f, animationProgress));

        if (animationProgress <= 0.01f) return;

        ScaledResolution sr = new ScaledResolution(mc);
        String amount = itemStack != null ? String.valueOf(itemStack.stackSize) : "0";
        String prefix = "Amount: ";
        
        float textWidth = Fonts.MAIN.get(18).width(prefix) + Fonts.MAIN.get(18).width(amount);
        float width = 16f + 8f + textWidth + 8f; 
        float height = 22f;
        float x = (sr.getScaledWidth() - width) / 2f;
        float y = sr.getScaledHeight() - 90f;

        GlStateManager.pushMatrix();
        
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(animationProgress, animationProgress, 1f);
        GlStateManager.translate(-centerX, -centerY, 0);

        int bgAlpha = (int) (150 * animationProgress);
        RoundedUtils.drawRound(x, y, width, height, 4f, new Color(0, 0, 0, bgAlpha));

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, (int) x + 4, (int) y + 3);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
        int textAlpha = (int) (255 * animationProgress);
        float fontY = y + (height / 2f) - (Fonts.MAIN.get(18).height() / 2f);
        float textX = x + 24f;

        Fonts.MAIN.get(18).drawWithShadow(prefix, textX, fontY, new Color(200, 200, 200, textAlpha).getRGB());
        Fonts.MAIN.get(18).drawWithShadow(amount, textX + Fonts.MAIN.get(18).width(prefix), fontY, new Color(81, 99, 149, textAlpha).getRGB());
        
        GlStateManager.popMatrix();
    }
}