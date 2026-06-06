package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    public final ModeProperty style = new ModeProperty("style", 0, new String[]{"MYAU", "CLEAN"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"}, () -> this.style.getValue() == 0);
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25, () -> this.style.getValue() == 0);
    public final BooleanProperty head = new BooleanProperty("head", true, () -> this.style.getValue() == 0);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true, () -> this.style.getValue() == 0);
    public final BooleanProperty outline = new BooleanProperty("outline", false, () -> this.style.getValue() == 0);
    public final BooleanProperty animations = new BooleanProperty("animations", true, () -> this.style.getValue() == 0);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase entityLivingBase = this.target;
            this.target = this.resolveTarget();
            if (this.target != null) {
                float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
                float abs = this.target.getAbsorptionAmount() / 2.0F;
                float heal = this.target.getHealth() / 2.0F + abs;
                if (this.target != entityLivingBase) {
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                }
                if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = this.target.getMaxHealth() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }
                ResourceLocation resourceLocation = this.getSkin(this.target);
                if (resourceLocation != null) {
                    this.headTexture = resourceLocation;
                }
                float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
                float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
                Color targetColor = this.getTargetColor(this.target);
                if (this.style.getValue() == 1) {
                    drawCleanStyle(this.target);
                    return;
                }
                Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
                float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
                Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
                int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
                String healthText = ChatColors.formatColor(
                        String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
                );
                int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
                String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
                int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
                String healthDiffText = ChatColors.formatColor(
                        String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
                );
                int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);
                float barContentWidth = Math.max(
                        (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
                        (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
                );
                float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
                float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
                float posX = this.offX.getValue().floatValue() / this.scale.getValue();
                switch (this.posX.getValue()) {
                    case 1:
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
                        break;
                    case 2:
                        posX *= -1.0F;
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barTotalWidth;
                }
                float posY = this.offY.getValue().floatValue() / this.scale.getValue();
                switch (this.posY.getValue()) {
                    case 1:
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - 13.5F;
                        break;
                    case 2:
                        posY *= -1.0F;
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - 27.0F;
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
                GlStateManager.translate(posX, posY, -450.0F);
                RenderUtil.enableRenderState();
                int backgroundColor = new Color(0.0F, 0.0F, 0.0F, (float) this.background.getValue() / 100.0F).getRGB();
                int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
                RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
                RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
                RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
                mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());
                if (this.indicator.getValue()) {
                    mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
                    mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
                }
                if (this.head.getValue() && this.headTexture != null) {
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(this.headTexture);
                    Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                }
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
        }
    }

    private void drawCleanStyle(EntityLivingBase entity) {
        String targetName = TeamUtil.stripName(entity);
        String targetPrefix = "Target: ";
        String healthPrefix = "Health: ";
        String healthValue = healthFormat.format(entity.getHealth());
        String status = getCleanStatus(entity);
        int targetWidth = mc.fontRendererObj.getStringWidth(targetPrefix + targetName + (status.isEmpty() ? "" : " " + status));
        int healthWidth = mc.fontRendererObj.getStringWidth(healthPrefix + healthValue);
        int hudWidth = Math.max(98, Math.max(targetWidth, healthWidth) + 14);
        int hudHeight = 30;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float baseX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                baseX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - hudWidth / 2.0F;
                break;
            case 2:
                baseX *= -1.0F;
                baseX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - hudWidth;
                break;
        }
        float baseY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                baseY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - hudHeight / 2.0F;
                break;
            case 2:
                baseY *= -1.0F;
                baseY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - hudHeight;
                break;
        }

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0F, baseY + hudHeight / 2.0F, 0);
        GL11.glScalef(sc, sc, 1.0F);
        GL11.glTranslated(-(baseX + hudWidth / 2.0F), -(baseY + hudHeight / 2.0F), 0);
        int x = (int) baseX;
        int y = (int) baseY;
        int bgColor = (150 << 24) | 0x000000;
        int whiteColor = 0xFFFFFFFF;
        int greenColor = 0xFF00FF38;
        int redColor = 0xFFFF3030;
        int yellowColor = 0xFFFFFF00;
        int statusColor = "W".equals(status) ? greenColor : ("L".equals(status) ? redColor : yellowColor);
        GlStateManager.enableBlend();
        Gui.drawRect(x, y, x + hudWidth, y + hudHeight, bgColor);
        drawVerticalGradientRect(x, y, x + 2, y + hudHeight, 0xFFC44DFF, 0xFF4D8DFF);
        int textX = x + 8;
        int targetY = y + 5;
        int healthY = y + 17;
        mc.fontRendererObj.drawString(targetPrefix, textX, targetY, whiteColor, this.shadow.getValue());
        int nameX = textX + mc.fontRendererObj.getStringWidth(targetPrefix);
        mc.fontRendererObj.drawString(targetName, nameX, targetY, whiteColor, this.shadow.getValue());
        if (!status.isEmpty()) {
            int statusX = nameX + mc.fontRendererObj.getStringWidth(targetName + " ");
            mc.fontRendererObj.drawString(status, statusX, targetY, statusColor, this.shadow.getValue());
        }
        mc.fontRendererObj.drawString(healthPrefix, textX, healthY, whiteColor, this.shadow.getValue());
        int healthX = textX + mc.fontRendererObj.getStringWidth(healthPrefix);
        mc.fontRendererObj.drawString(healthValue, healthX, healthY, greenColor, this.shadow.getValue());
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

    private String getCleanStatus(EntityLivingBase entity) {
        if (mc.thePlayer == null || entity == null) return "N";
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        boolean killAuraReady = killAura != null && killAura.isEnabled() && killAura.isAttackAllowed()
                && killAura.getTarget() == entity && mc.thePlayer.getDistanceToEntity(entity) <= (double) killAura.attackRange.getValue();
        float playerScore = getCleanFightScore(mc.thePlayer);
        float targetScore = getCleanFightScore(entity);
        if (killAuraReady && playerScore >= targetScore * 0.92F) return "W";
        if (!killAuraReady || playerScore < targetScore * 0.85F) return "L";
        return "N";
    }

    private float getCleanFightScore(EntityLivingBase entity) {
        float health = entity.getHealth() + entity.getAbsorptionAmount();
        if (entity instanceof EntityPlayer) health += ((EntityPlayer) entity).getTotalArmorValue() * 0.45F;
        return health;
    }

    private void drawVerticalGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startA = (float) (startColor >> 24 & 255) / 255.0F;
        float startR = (float) (startColor >> 16 & 255) / 255.0F;
        float startG = (float) (startColor >> 8 & 255) / 255.0F;
        float startB = (float) (startColor & 255) / 255.0F;
        float endA = (float) (endColor >> 24 & 255) / 255.0F;
        float endR = (float) (endColor >> 16 & 255) / 255.0F;
        float endG = (float) (endColor >> 8 & 255) / 255.0F;
        float endB = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(startR, startG, startB, startA);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(right, top);
        GL11.glColor4f(endR, endG, endB, endA);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(left, bottom);
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
