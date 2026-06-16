package myau.mixin;

import myau.Myau;
import myau.module.modules.misc.AntiObfuscate;
import myau.module.modules.misc.NickHider;
import myau.module.modules.render.HUD;
import myau.enums.ChatColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = FontRenderer.class, priority = 9999)
public abstract class MixinFontRenderer {

    @Shadow public abstract int drawString(String text, float x, float y, int color, boolean dropShadow);

    private boolean isCustomRendering = false;

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String modifyGetStringWidth(String string) {
        if (string == null || Myau.moduleManager == null) return string;

        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate != null && antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }

        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider != null && nickHider.isEnabled()) {
            string = nickHider.replaceNick(string);
        }

        return string;
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    public void onDrawString(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (isCustomRendering || text == null || text.isEmpty() || Myau.moduleManager == null) {
            return;
        }

        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate != null && antiObfuscate.isEnabled()) {
            text = antiObfuscate.stripObfuscated(text);
        }

        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider != null && nickHider.isEnabled()) {
            text = nickHider.replaceNick(text);
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        if (hud == null || !hud.isEnabled()) return;

        String targetName = mc.thePlayer.getName();
        if (nickHider != null && nickHider.isEnabled()) {
            targetName = EnumChatFormatting.getTextWithoutFormattingCodes(ChatColors.formatColor(nickHider.protectName.getValue()));
        }

        if (!targetName.isEmpty() && text.contains(targetName)) {
            isCustomRendering = true;
            int result = drawTrueRGBName(text, x, y, color, dropShadow, targetName, hud);
            isCustomRendering = false;
            cir.setReturnValue(result); 
        }
    }

    private int drawTrueRGBName(String text, float x, float y, int originalColor, boolean dropShadow, String targetName, HUD hud) {
        float currentX = x;
        String remaining = text;
        int index = remaining.indexOf(targetName);
        long time = System.currentTimeMillis();
        String currentFormat = ""; 

        while (index != -1) {
            String before = remaining.substring(0, index);
            if (!before.isEmpty()) {
                String textToDraw = currentFormat + before;
                currentX = this.drawString(textToDraw, currentX, y, originalColor, dropShadow);
                currentFormat = FontRenderer.getFormatFromString(textToDraw); 
            }

            for (int i = 0; i < targetName.length(); i++) {
                String charStr = String.valueOf(targetName.charAt(i));

                int charColor = hud.getColor(time, i * 15L).getRGB();

                currentX = this.drawString(charStr, currentX, y, charColor, dropShadow);
            }

            remaining = remaining.substring(index + targetName.length());
            index = remaining.indexOf(targetName);
        }

        if (!remaining.isEmpty()) {
            this.drawString(currentFormat + remaining, currentX, y, originalColor, dropShadow);
        }

        return (int) currentX;
    }
}