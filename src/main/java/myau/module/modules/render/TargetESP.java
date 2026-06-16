package myau.module.modules.render;

import myau.module.modules.combat.KillAura;
import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class TargetESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty hudColor = new BooleanProperty("hud-color", true);
    public final IntProperty red = new IntProperty("red", 255, 0, 255, () -> !this.hudColor.getValue());
    public final IntProperty green = new IntProperty("green", 80, 0, 255, () -> !this.hudColor.getValue());
    public final IntProperty blue = new IntProperty("blue", 140, 0, 255, () -> !this.hudColor.getValue());
    public final FloatProperty alpha = new FloatProperty("alpha", 0.85F, 0.1F, 1.0F);
    public final FloatProperty radius = new FloatProperty("radius", 0.72F, 0.4F, 1.5F);
    public final FloatProperty height = new FloatProperty("height", 0.34F, 0.15F, 0.85F);
    public final BooleanProperty pulse = new BooleanProperty("pulse", true);

    public TargetESP() {
        super("TargetESP", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) return;
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura == null) return;

        EntityLivingBase target = killAura.getTarget();
        if (target == null || !TeamUtil.isEntityLoaded(target)) return;

        Color color = this.getColor();
        double x = lerp(target.lastTickPosX, target.posX, event.getPartialTicks())
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double y = lerp(target.lastTickPosY, target.posY, event.getPartialTicks())
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double z = lerp(target.lastTickPosZ, target.posZ, event.getPartialTicks())
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        renderMiauHalo(target, x, y, z, color);
        renderGroundCircle(target, x, y, z, color);
        renderDoubleSpiral(target, x, y, z, color);
    }

    private Color getColor() {
        if (this.hudColor.getValue()) {
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            return hud.getColor(System.currentTimeMillis());
        }
        return new Color(this.red.getValue(), this.green.getValue(), this.blue.getValue());
    }

    private void renderMiauHalo(EntityLivingBase target, double x, double y, double z, Color color) {
        float hurtPulse = target.hurtTime > 0 ? target.hurtTime / 10.0F : 0.0F;
        float baseAlpha = Math.min(1.0F, this.alpha.getValue() + hurtPulse * 0.22F);
        float pulseValue = this.pulse.getValue()
                ? (float) (0.5D + 0.5D * Math.sin(System.currentTimeMillis() / 180.0D))
                : 0.5F;
        double r = (target.width * 0.75D + this.radius.getValue()) * (0.92D + pulseValue * 0.08D + hurtPulse * 0.10D);
        double top = target.height + 0.12D + hurtPulse * 0.08D;
        double lower = Math.max(0.08D, this.height.getValue());
        int segments = 72;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;

        GL11.glLineWidth(2.4F);
        GL11.glColor4f(red, green, blue, baseAlpha);
        drawRing(r, top, segments);

        GL11.glLineWidth(1.2F);
        GL11.glColor4f(red, green, blue, baseAlpha * 0.55F);
        drawRing(r * 0.82D, top - lower, segments);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(red, green, blue, baseAlpha * 0.7F);
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians((System.currentTimeMillis() / 8.0D + i * 90.0D) % 360.0D);
            double x1 = Math.cos(angle) * r;
            double z1 = Math.sin(angle) * r;
            double x2 = Math.cos(angle) * r * 0.82D;
            double z2 = Math.sin(angle) * r * 0.82D;
            GL11.glVertex3d(x1, top, z1);
            GL11.glVertex3d(x2, top - lower, z2);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            double cx = Math.cos(angle);
            double cz = Math.sin(angle);
            GL11.glColor4f(red, green, blue, baseAlpha * 0.10F);
            GL11.glVertex3d(cx * r, top, cz * r);
            GL11.glColor4f(red, green, blue, 0.0F);
            GL11.glVertex3d(cx * r * 0.82D, top - lower, cz * r * 0.82D);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void renderGroundCircle(EntityLivingBase target, double x, double y, double z, Color color) {
        float hurtPulse = target.hurtTime > 0 ? target.hurtTime / 10.0F : 0.0F;
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;
        float a = Math.min(1.0F, this.alpha.getValue() * 0.7F + hurtPulse * 0.25F);
        double radius = target.width + this.radius.getValue() * 0.9D + hurtPulse * 0.18D;
        int segments = 96;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y + 0.025D, z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(red, green, blue, a * 0.16F);
        GL11.glVertex3d(0.0D, 0.0D, 0.0D);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            GL11.glColor4f(red, green, blue, 0.0F);
            GL11.glVertex3d(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glLineWidth(2.0F + hurtPulse * 1.2F);
        GL11.glColor4f(red, green, blue, a);
        drawRing(radius, 0.0D, segments);
        GL11.glLineWidth(1.0F);
        GL11.glColor4f(red, green, blue, a * 0.45F);
        drawRing(radius * 0.72D, 0.0D, segments);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void renderDoubleSpiral(EntityLivingBase target, double x, double y, double z, Color color) {
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;
        float a = this.alpha.getValue() * 0.75F;
        double radius = target.width * 0.75D + this.radius.getValue() * 0.65D;
        long now = System.currentTimeMillis();
        int points = 44;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.6F);

        for (int spiral = 0; spiral < 2; spiral++) {
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i < points; i++) {
                double progress = i / (double) (points - 1);
                double spin = (now / (spiral == 0 ? 260.0D : -260.0D)) + progress * Math.PI * 2.15D + spiral * Math.PI;
                double yy = 0.15D + target.height * progress;
                double fade = Math.sin(progress * Math.PI);
                GL11.glColor4f(red, green, blue, (float) (a * fade));
                GL11.glVertex3d(Math.cos(spin) * radius, yy, Math.sin(spin) * radius);
            }
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void drawRing(double radius, double y, int segments) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            GL11.glVertex3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private static double lerp(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }
}
