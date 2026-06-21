package myau.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Redirect(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiMainMenu;drawString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"
            )
    )
    private void replaceCopyrightWithCredits(GuiMainMenu instance, FontRenderer fontRenderer, String text, int x, int y, int color) {
        String credits = "Credits: [ksyz, OpenMyau Project, idle]";
        if ("Copyright Mojang AB. Do not distribute!".equals(text)) {
            int creditsX = instance.width - fontRenderer.getStringWidth(credits) - 2;
            instance.drawString(fontRenderer, credits, creditsX, y, 0xFFFFFFFF);
            return;
        }
        instance.drawString(fontRenderer, text, x, y, color);
    }
}