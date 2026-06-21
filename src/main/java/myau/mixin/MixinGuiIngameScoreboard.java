package myau.mixin;

import myau.Myau;
import myau.module.modules.render.HUD;
import myau.module.modules.render.Scoreboard;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngameScoreboard {

    @Shadow
    protected abstract void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes);

    private static boolean isRenderingBloom = false;

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardPre(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (!isRenderingBloom) {
            HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
            if (hud != null && hud.isEnabled() && hud.shaders.getValue()) {
                isRenderingBloom = true;
                
                myau.util.shader.RenderSystem.renderBloom(() -> {
                    this.renderScoreboard(objective, scaledRes);
                });
                
                this.renderScoreboard(objective, scaledRes);
                
                isRenderingBloom = false;
                ci.cancel();
                return;
            }
        }

        if (Myau.moduleManager != null) {
            Scoreboard scoreboardMod = (Scoreboard) Myau.moduleManager.getModule(Scoreboard.class);
            if (scoreboardMod != null && scoreboardMod.isEnabled()) {
                scoreboardMod.updateBounds(scaledRes);
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) scoreboardMod.drag.position.x - scoreboardMod.defaultX, (float) scoreboardMod.drag.position.y - scoreboardMod.defaultY, 0.0f);
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
