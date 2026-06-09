package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Collection;

public class StatusEffect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty x = new IntProperty("x", 132, 0, 1000);
    public final IntProperty y = new IntProperty("y", 28, 0, 1000);
    public final IntProperty scale = new IntProperty("scale", 100, 50, 200);
    public final BooleanProperty rightSide = new BooleanProperty("right-side", true);
    public final BooleanProperty showBackground = new BooleanProperty("background", true);
    public final BooleanProperty showBadEffects = new BooleanProperty("bad-effects", true);
    public final BooleanProperty romanAmplifier = new BooleanProperty("roman-amplifier", true);

    public StatusEffect() {
        super("StatusEffect", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;
        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float scaleValue = this.scale.getValue() / 100.0F;
        int drawX = this.rightSide.getValue() ? sr.getScaledWidth() - this.x.getValue() : this.x.getValue();
        int drawY = this.y.getValue();

        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleValue, scaleValue, 1.0F);
        drawX = (int) (drawX / scaleValue);
        drawY = (int) (drawY / scaleValue);

        int panelWidth = 0;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null || (potion.isBadEffect() && !this.showBadEffects.getValue())) continue;
            String name = getEffectName(potion, effect);
            String time = Potion.getDurationString(effect);
            panelWidth = Math.max(panelWidth, mc.fontRendererObj.getStringWidth(name) + mc.fontRendererObj.getStringWidth(time) + 18);
        }
        panelWidth = Math.max(panelWidth, 126);

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null || (potion.isBadEffect() && !this.showBadEffects.getValue())) continue;
            if (this.showBackground.getValue()) {
                Gui.drawRect(drawX - 4, drawY - 3, drawX + panelWidth, drawY + 12, 0x66000000);
            }
            int color = potion.isBadEffect() ? 0xFFFF6666 : 0xFF77FF99;
            String name = getEffectName(potion, effect);
            String time = Potion.getDurationString(effect);
            mc.fontRendererObj.drawStringWithShadow(name, drawX, drawY, color);
            mc.fontRendererObj.drawStringWithShadow(time, drawX + panelWidth - 4 - mc.fontRendererObj.getStringWidth(time), drawY, 0xFFFFFFFF);
            drawY += 16;
        }
        GlStateManager.popMatrix();
    }

    private String getEffectName(Potion potion, PotionEffect effect) {
        String name = net.minecraft.client.resources.I18n.format(potion.getName());
        if (effect.getAmplifier() > 0) {
            name += " " + (this.romanAmplifier.getValue() ? toRoman(effect.getAmplifier() + 1) : String.valueOf(effect.getAmplifier() + 1));
        }
        return name;
    }

    private String toRoman(int value) {
        switch (value) {
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(value);
        }
    }
}

