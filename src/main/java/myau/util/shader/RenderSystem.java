package myau.util.shader;

import myau.module.modules.render.HUD;
import myau.util.shader.impl.rise.BloomShader;
import myau.util.shader.impl.rise.BlurShader;
import myau.util.shader.impl.rise.ShaderRenderType;
import net.minecraft.client.Minecraft;

import java.util.Collections;

public class RenderSystem {

    private static final BloomShader bloomShader = new BloomShader();
    private static final BlurShader blurShader = new BlurShader();

    public static void renderBlur(Runnable renderOps) {
        HUD hud = (HUD) myau.Myau.moduleManager.getModule(HUD.class);
        if (hud == null || !hud.shaders.getValue() || !hud.blurSettings.getValue()) {
            return;
        }
        renderBlur(hud.blurRadius.getValue().floatValue(), hud.blurPasses.getValue(), renderOps);
    }

    public static void renderBlur(float radius, int passes, Runnable renderOps) {
        blurShader.setRadius((int) radius);
        blurShader.setCompression(passes); // Used as compression in Rise
        blurShader.update();
        
        blurShader.run(ShaderRenderType.OVERLAY, ((myau.mixin.IAccessorMinecraft) Minecraft.getMinecraft()).getTimer().renderPartialTicks, Collections.singletonList(renderOps));
    }

    public static void renderBloom(Runnable renderOps) {
        HUD hud = (HUD) myau.Myau.moduleManager.getModule(HUD.class);
        if (hud == null || !hud.shaders.getValue()) {
            return;
        }
        renderBloom(hud.bloomRadius.getValue().floatValue(), hud.bloomPasses.getValue(), renderOps);
    }

    public static void renderBloom(float radius, int passes, Runnable renderOps) {
        bloomShader.setRadius((int) radius);
        bloomShader.setCompression(passes); // Used as compression in Rise
        bloomShader.update();
        
        bloomShader.run(ShaderRenderType.OVERLAY, ((myau.mixin.IAccessorMinecraft) Minecraft.getMinecraft()).getTimer().renderPartialTicks, Collections.singletonList(renderOps));
    }
}
