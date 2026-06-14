package myau.mixin;

import myau.Myau;
import myau.module.modules.render.Scoreboard;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngameScoreboard {
    @Inject(method = "renderScoreboard", at = @At("HEAD"))
    private void onRenderScoreboardPre(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Myau.moduleManager != null) {
            Scoreboard scoreboardMod = (Scoreboard) Myau.moduleManager.getModule(Scoreboard.class);
            if (scoreboardMod != null && scoreboardMod.isEnabled()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(scoreboardMod.offX.getValue(), scoreboardMod.offY.getValue(), 0.0f);
            }
        }
    }

    @Inject(method = "renderScoreboard", at = @At("RETURN"))
    private void onRenderScoreboardPost(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Myau.moduleManager != null) {
            Scoreboard scoreboardMod = (Scoreboard) Myau.moduleManager.getModule(Scoreboard.class);
            if (scoreboardMod != null && scoreboardMod.isEnabled()) {
                GlStateManager.popMatrix();
            }
        }
    }
}
