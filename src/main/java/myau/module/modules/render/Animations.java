package myau.module.modules.render;

import myau.Myau;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class Animations extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] MODES = new String[]{
            "OneSeven", "OldPushdown", "NewPushdown", "Old", "Helium", "Argon", "Cesium", "Sulfur"
    };

    public final ModeProperty mode = new ModeProperty("Mode", 2, MODES);
    public final BooleanProperty oddSwing = new BooleanProperty("OddSwing", false);
    public final IntProperty swingSpeed = new IntProperty("SwingSpeed", 15, 0, 20);
    public final FloatProperty itemScale = new FloatProperty("ItemScale", 0.0F, -5.0F, 5.0F);
    public final FloatProperty x = new FloatProperty("X", 0.0F, -5.0F, 5.0F);
    public final FloatProperty y = new FloatProperty("Y", 0.0F, -5.0F, 5.0F);
    public final FloatProperty positionRotationX = new FloatProperty("PositionRotationX", 0.0F, -50.0F, 50.0F);
    public final FloatProperty positionRotationY = new FloatProperty("PositionRotationY", 0.0F, -50.0F, 50.0F);
    public final FloatProperty positionRotationZ = new FloatProperty("PositionRotationZ", 0.0F, -50.0F, 50.0F);

    public Animations() {
        super("Animations", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    public static boolean apply(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        Animations animations = (Animations) Myau.moduleManager.modules.get(Animations.class);
        if (animations == null || !animations.isEnabled() || player == null) {
            return false;
        }
        animations.transform(swingProgress, equipProgress, player);
        return true;
    }

    public static int getSwingSpeed() {
        Animations animations = (Animations) Myau.moduleManager.modules.get(Animations.class);
        return animations != null && animations.isEnabled() ? animations.swingSpeed.getValue() : 6;
    }

    public static boolean isOddSwing() {
        Animations animations = (Animations) Myau.moduleManager.modules.get(Animations.class);
        return animations != null && animations.isEnabled() && animations.oddSwing.getValue();
    }

    private void transform(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        GlStateManager.translate(this.x.getValue(), this.y.getValue(), 0.0F);
        float scale = 1.0F + this.itemScale.getValue() * 0.1F;
        GlStateManager.scale(scale, scale, scale);

        switch (this.mode.getValue()) {
            case 0:
                oneSeven(swingProgress, equipProgress, player);
                break;
            case 1:
                oldPushdown(swingProgress, equipProgress, player);
                break;
            case 2:
                newPushdown(swingProgress, equipProgress, player);
                break;
            case 3:
                old(swingProgress, equipProgress, player);
                break;
            case 4:
                helium(swingProgress, equipProgress, player);
                break;
            case 5:
                argon(swingProgress, equipProgress, player);
                break;
            case 6:
                cesium(swingProgress, equipProgress, player);
                break;
            case 7:
                sulfur(swingProgress, equipProgress, player);
                break;
            default:
                newPushdown(swingProgress, equipProgress, player);
                break;
        }
    }

    private void oneSeven(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        transformFirstPersonItem(equipProgress, swingProgress);
        doBlockTransformations();
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
    }

    private void old(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        transformFirstPersonItem(equipProgress, swingProgress);
        doBlockTransformations();
    }

    private void oldPushdown(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        GlStateManager.translate(0.56D, -0.52D, -0.5D);
        GlStateManager.translate(0.0D, -equipProgress * 0.3D, 0.0D);
        GlStateManager.rotate(45.5F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(0.0F);
        float var4 = MathHelper.sin(0.0F);
        GlStateManager.rotate(var3 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(var4 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(var4 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.32D, 0.32D, 0.32D);
        float var15 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-var15 * 125.0F / 1.75F, 3.95F, 0.35F, 8.0F);
        GlStateManager.rotate(-var15 * 35.0F, 0.0F, var15 / 100.0F, -10.0F);
        GlStateManager.translate(-1.0D, 0.6D, -0.0D);
        doBlockTransformations();
        GL11.glTranslated(1.05D, 0.35D, 0.4D);
        GL11.glTranslatef(-1.0F, 0.0F, 0.0F);
    }

    private void newPushdown(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        double tx = this.positionRotationX.getValue() - 0.08D;
        double ty = this.positionRotationY.getValue() + 0.12D;
        double tz = this.positionRotationZ.getValue();
        GlStateManager.translate(tx, ty, tz);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        transformFirstPersonItem(equipProgress / 1.4F, 0.0F);
        GlStateManager.rotate(-var9 * 65.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 60.0F, 1.0F, var9 / 3.0F, -0.0F);
        doBlockTransformations();
        GlStateManager.scale(1.0D, 1.0D, 1.0D);
    }

    private void helium(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        transformFirstPersonItem(equipProgress, 0.0F);
        float c0 = MathHelper.sin(swingProgress * equipProgress * 3.1415927F);
        float c1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-c1 * 55.0F, 30.0F, c0 / 5.0F, 0.0F);
        doBlockTransformations();
    }

    private void argon(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        transformFirstPersonItem(equipProgress / 2.5F, swingProgress);
        float c2 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float c3 = MathHelper.cos(MathHelper.sqrt_float(equipProgress) * 3.1415927F);
        GlStateManager.rotate(c3 * 50.0F / 10.0F, -c2, -0.0F, 100.0F);
        GlStateManager.rotate(c2 * 50.0F, 200.0F, -c2 / 2.0F, -0.0F);
        GlStateManager.translate(0.0D, 0.3D, 0.0D);
        doBlockTransformations();
    }

    private void cesium(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        float c4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        transformFirstPersonItem(equipProgress, 0.0F);
        GlStateManager.rotate(-c4 * 10.0F / 20.0F, c4 / 2.0F, 0.0F, 4.0F);
        GlStateManager.rotate(-c4 * 30.0F, 0.0F, c4 / 3.0F, 0.0F);
        GlStateManager.rotate(-c4 * 10.0F, 1.0F, c4 / 10.0F, 0.0F);
        GlStateManager.translate(0.0D, 0.2D, 0.0D);
        doBlockTransformations();
    }

    private void sulfur(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        float c5 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float c6 = MathHelper.cos(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        transformFirstPersonItem(equipProgress, 0.0F);
        GlStateManager.rotate(-c5 * 30.0F, c5 / 10.0F, c6 / 10.0F, 0.0F);
        GlStateManager.translate(c5 / 1.5D, 0.2D, 0.0D);
        doBlockTransformations();
    }

    private void doBlockTransformations() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }
}
