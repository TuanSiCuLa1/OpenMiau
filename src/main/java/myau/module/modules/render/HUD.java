package myau.module.modules.render;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorGuiChat;
import myau.module.modules.render.hud.InterfaceComponent;
import myau.module.Module;
import myau.util.render.ColorUtil;
import myau.util.render.RenderUtil;
import myau.property.properties.*;
import myau.util.render.Themes;
import myau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final java.util.Map<Module, InterfaceComponent> components = new java.util.HashMap<>();

    private long lastMS = System.currentTimeMillis();
    private List<Module> activeModules = new ArrayList<>();
    
    private double lastX = 0, lastZ = 0;
    private String bpsString = "0.00";

    public final BooleanProperty showWatermark = new BooleanProperty("watermark", true);
    public final ModeProperty hudMode = new ModeProperty("mode", 0, new String[]{"NORMAL", "EXHIBITION"});
    public final TextProperty watermarkName = new TextProperty("watermark-name", "Miau", this.showWatermark::getValue);
    public final BooleanProperty showCoordinates = new BooleanProperty("coordinates", true, () -> this.hudMode.getValue() == 1);
    public final BooleanProperty showTime = new BooleanProperty("show-time", true, this.showWatermark::getValue);
    public final BooleanProperty showFps = new BooleanProperty("show-fps", true, this.showWatermark::getValue);
    public final BooleanProperty showPing = new BooleanProperty("show-ping", true, this.showWatermark::getValue);

    public final ModeProperty colorAnimation = new ModeProperty("color-animation", 1, new String[]{"STATIC", "FADE", "RAINBOW"});
    public final ModeProperty modulesToShow = new ModeProperty("modules-to-show", 1, new String[]{"ALL", "EXCLUDE RENDER", "ONLY BOUND"});
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final BooleanProperty shaders = new BooleanProperty("Shaders", false);
    public final BooleanProperty blurSettings = new BooleanProperty("Blur Settings", false, () -> this.shaders.getValue());
    public final FloatProperty blurRadius = new FloatProperty("Blur Radius", 3.0F, 1.0F, 10.0F, () -> this.shaders.getValue());
    public final IntProperty blurPasses = new IntProperty("Blur Passes", 2, 1, 10, () -> this.shaders.getValue());
    public final FloatProperty bloomRadius = new FloatProperty("Bloom Radius", 3.0F, 1.0F, 10.0F, () -> this.shaders.getValue());
    public final IntProperty bloomPasses = new IntProperty("Bloom Passes", 4, 1, 10, () -> this.shaders.getValue());

    // Hàm lấy Component tự động
    private InterfaceComponent getComponent(Module module) {
        return components.computeIfAbsent(module, InterfaceComponent::new);
    }

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }
        return moduleSuffix;
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(this.getModuleName(module), this.getModuleSuffix(module));
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = getFont().getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + getFont().getStringWidth(str);
            }
        }
        return width;
    }

    public myau.util.font.Font getFont() {
        return myau.util.font.Fonts.MAIN.get(18);
    }
    
    public HUD() {
        super("HUD", true, true);
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long yPos) {
        Themes theme = Themes.getCurrentTheme();

        switch (this.colorAnimation.getValue()) {
            case 0: // STATIC
                return theme.getFirstColor();
            case 1: // FADE
                return theme.getAccentColor(new Vector2d(0, yPos));
            case 2: // RAINBOW
                return ColorUtil.rainbow((int) (time * 500 / 6));
            default:
                return Color.white;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer != null) {
                double dist = Math.hypot(mc.thePlayer.posX - lastX, mc.thePlayer.posZ - lastZ);
                bpsString = String.valueOf(myau.util.math.MathUtil.round(dist * 20.0D * ((myau.mixin.IAccessorMinecraft) mc).getTimer().timerSpeed, 2));
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
            }
        }
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Myau.moduleManager.modules.values().stream()
                    .filter(module -> module.isEnabled() && (this.modulesToShow.getValue() == 0 || !module.isHidden()) && getComponent(module).shouldDisplay(this))
                    .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                    .collect(Collectors.<Module>toList());
            try {
                Myau.clientName = ChatColors.getDynamicPrefix();
            } catch (Exception e) { }
        }
    }

    private String getExhibitionWatermark() {
        String customName = this.watermarkName.getValue();
        if (customName == null || customName.isEmpty()) customName = "Miau";

        int ping = 0;
        if (mc.getNetHandler() != null && mc.thePlayer != null) {
            net.minecraft.client.network.NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (playerInfo != null) ping = playerInfo.getResponseTime();
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a");
        String formattedTime = sdf.format(new java.util.Date());

        String text = customName.charAt(0) + "§7" + customName.substring(1);

        if (this.showTime.getValue()) text += " [§f" + formattedTime + "§7]";
        if (this.showFps.getValue()) text += " [§f" + Minecraft.getDebugFPS() + " FPS§7]";
        if (this.showPing.getValue()) text += " [§f" + ping + "ms§7]";
        return text;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        long currentMS = System.currentTimeMillis();
        float delta = (currentMS - lastMS);
        lastMS = currentMS;
        if (delta > 200 || delta < 0) delta = 16;

        for (Module module : Myau.moduleManager.modules.values()) {
            InterfaceComponent component = getComponent(module);
            boolean shouldBeVisible = module.isEnabled() && (this.modulesToShow.getValue() == 0 || !module.isHidden()) && component.shouldDisplay(this);

            if (shouldBeVisible) {
                component.animationTime = (float) Math.min(1.0, component.animationTime + (delta * 0.006));
            } else {
                component.animationTime = (float) Math.max(0.0, component.animationTime - (delta * 0.006));
            }
        }

        java.util.List<InterfaceComponent> animatingComponents = Myau.moduleManager.modules.values().stream()
                .map(this::getComponent)
                .filter(c -> c.animationTime > 0.001)
                .sorted(Comparator.comparingInt((InterfaceComponent c) -> this.getModuleWidth(c.module)).reversed())
                .collect(Collectors.toList());

        float heightExhibition = (float) getFont().getFontHeight() + 2.0F;
        float heightNormal = (float) getFont().getFontHeight() - 1.0F;
        float currentYExhibition = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
        float currentYNormal = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();

        if (this.posY.getValue() == 1) {
            currentYExhibition = (float) new ScaledResolution(mc).getScaledHeight() - currentYExhibition - heightExhibition * this.scale.getValue();
            currentYNormal = (float) new ScaledResolution(mc).getScaledHeight() - currentYNormal - heightNormal * this.scale.getValue();
        }

        for (InterfaceComponent component : animatingComponents) {
            float targetY = (this.hudMode.getValue() == 1) ? currentYExhibition : currentYNormal;

            if (component.position.y == 0) component.position.y = targetY;

            component.position.y = myau.util.math.MathUtil.lerp((float) component.position.y, targetY, 0.015f * delta);

            if (component.module.isEnabled() && (this.modulesToShow.getValue() == 0 || !component.module.isHidden()) && component.shouldDisplay(this)) {
                float spacingEx = heightExhibition * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                float spacingNorm = (heightNormal + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                currentYExhibition += spacingEx;
                currentYNormal += spacingNorm;
            }
        }

        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(2.0F, (float) (mc.currentScreen.height - 14), (float) (mc.currentScreen.width - 2), (float) (mc.currentScreen.height - 2), 1.5F, 0, this.getColor(System.currentTimeMillis()).getRGB());
                RenderUtil.disableRenderState();
            }
        }

        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            long l = System.currentTimeMillis();

            if (this.shaders.getValue()) {
                final long finalL = l;
                final float finalDelta = delta;
                myau.util.shader.RenderSystem.renderBloom(() -> {
                    renderElements(finalL, finalDelta, animatingComponents);
                    renderBPSBackground(false);
                    renderBPSText(finalL);
                });
                
                if (this.blurSettings.getValue()) {
                    myau.util.shader.RenderSystem.renderBlur(() -> {
                        renderBPSBackground(true);
                    });
                }
            }
            renderElements(l, delta, animatingComponents);
            
            renderBPSBackground(false);
            renderBPSText(l);
        }
    }

    private void renderElements(long l, float delta, java.util.List<InterfaceComponent> animatingComponents) {
        if (this.showWatermark.getValue()) {
            String watermark = getExhibitionWatermark();
            if (watermark != null) getFont().drawWithShadow(watermark, 3.0F, 3.0F, this.getColor(l).getRGB());
        }

        if (this.hudMode.getValue() == 1) { // Exhibition mode
            if (this.showCoordinates.getValue() && mc.thePlayer != null) {
                String posX2 = String.valueOf(Math.round(mc.thePlayer.posX));
                String posY2 = String.valueOf(Math.round(mc.thePlayer.posY));
                String posZ2 = String.valueOf(Math.round(mc.thePlayer.posZ));
                float yCoord = new ScaledResolution(mc).getScaledHeight() - 10;
                float fontHeight = getFont().getFontHeight();
                int colour = this.getColor(l).getRGB();
                getFont().drawWithShadow("X: §7" + posX2, 3.0F, yCoord - fontHeight * 2, colour);
                getFont().drawWithShadow("Y: §7" + posY2, 3.0F, yCoord - fontHeight, colour);
                getFont().drawWithShadow("Z: §7" + posZ2, 3.0F, yCoord, colour);
            }

            float height = (float) getFont().getFontHeight() + 2.0F;
            float x = (float) this.offsetX.getValue();
            if (this.posX.getValue() == 1) x = (float) new ScaledResolution(mc).getScaledWidth() - x;

            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);

            // 4. Render Exhibition Mode
            for (InterfaceComponent component : animatingComponents) {
                Module module = component.module;
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));

                double animProgress = component.animationTime;
                float drawY = (float) component.position.y / this.scale.getValue();
                float baseX = x / this.scale.getValue();
                float targetX;
                boolean shouldBeVisible = module.isEnabled() && (this.modulesToShow.getValue() == 0 || !module.isHidden()) && component.shouldDisplay(this);

                if (this.posX.getValue() == 1) {
                    targetX = baseX - totalWidth;
                    if (!shouldBeVisible) targetX += totalWidth + 20;
                } else {
                    targetX = baseX;
                    if (!shouldBeVisible) targetX -= totalWidth + 20;
                }

                if (component.position.x == 5000) component.position.x = this.posX.getValue() == 1 ? targetX + 50 : targetX - 50;

                component.position.x = myau.util.math.MathUtil.lerp((float) component.position.x, targetX, 0.015f * delta);
                float drawX = (float) component.position.x;

                int alpha = (int) (255 * animProgress);
                long finalY = (long) component.position.y;
                int color = (alpha << 24) | (this.getColor(l, finalY).getRGB() & 0x00FFFFFF);
                int bgColor = new Color(0.0F, 0.0F, 0.0F, (this.background.getValue().floatValue() / 100.0F) * (float)animProgress).getRGB();

                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) RenderUtil.drawRect(drawX - 2.0F, drawY - 2.0F, drawX + totalWidth + 2.0F, drawY + height - 2.0F, bgColor);

                if (this.showBar.getValue()) {
                    if (this.posX.getValue() == 0) RenderUtil.drawRect(drawX - 3.0F, drawY - 2.0F, drawX - 2.0F, drawY + height - 2.0F, color);
                    else RenderUtil.drawRect(drawX + totalWidth + 2.0F, drawY - 2.0F, drawX + totalWidth + 3.0F, drawY + height - 2.0F, color);
                }
                RenderUtil.disableRenderState();

                getFont().drawWithShadow(moduleName, drawX, drawY, color);

                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float suffixX = drawX + getFont().getStringWidth(moduleName) + 2.0F;
                    int suffixColor = ((int)(170 * animProgress) << 24) | 0x00AAAAAA;
                    for (String str : moduleSuffix) {
                        getFont().drawWithShadow(str, suffixX, drawY, suffixColor);
                        suffixX += getFont().getStringWidth(str) + 2.0F;
                    }
                }
            }
            GlStateManager.popMatrix();
        } else {
            float height = (float) getFont().getFontHeight() - 1.0F;
            float x = (float) this.offsetX.getValue() + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
            if (this.posX.getValue() == 1) x = (float) new ScaledResolution(mc).getScaledWidth() - x;

            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);

            for (InterfaceComponent component : animatingComponents) {
                Module module = component.module;
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));

                double animProgress = component.animationTime;
                float drawY = (float) component.position.y / this.scale.getValue();
                float baseX = x / this.scale.getValue();
                float targetX;
                boolean shouldBeVisible = module.isEnabled() && (this.modulesToShow.getValue() == 0 || !module.isHidden()) && component.shouldDisplay(this);

                if (this.posX.getValue() == 1) {
                    targetX = baseX - totalWidth;
                    if (!shouldBeVisible) targetX += totalWidth + 20;
                } else {
                    targetX = baseX;
                    if (!shouldBeVisible) targetX -= totalWidth + 20;
                }

                if (component.position.x == 5000) component.position.x = this.posX.getValue() == 1 ? targetX + 50 : targetX - 50;

                component.position.x = myau.util.math.MathUtil.lerp((float) component.position.x, targetX, 0.015f * delta);
                float drawX = (float) component.position.x;

                int alpha = (int) (255 * animProgress);

                long finalY = (long) component.position.y;
                int color = (alpha << 24) | (this.getColor(l, finalY).getRGB() & 0x00FFFFFF);
                int bgColor = new Color(0.0F, 0.0F, 0.0F, (this.background.getValue().floatValue() / 100.0F) * (float)animProgress).getRGB();

                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    RenderUtil.drawRect(
                            drawX - 1.0F,
                            drawY - (this.posY.getValue() == 0 ? (finalY == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                            drawX + totalWidth + 1.0F,
                            drawY + height + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (finalY == 0L ? 1.0F : 0.0F)),
                            bgColor
                    );
                }
                if (this.showBar.getValue()) {
                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(drawX + (this.posX.getValue() == 0 ? -3.0F : totalWidth + 1.0F), drawY - (this.posY.getValue() == 0 ? (finalY == 0L ? 1.0F : 0.0F) : 1.0F), drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 2.0F), drawY + height + (this.posY.getValue() == 0 ? 1.0F : (finalY == 0L ? 1.0F : 0.0F)), color);
                    } else {
                        RenderUtil.drawRect(drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 1.0F), drawY - (this.posY.getValue() == 0 ? (finalY == 0L ? 1.0F : 0.0F) : 0.0F), drawX + (this.posX.getValue() == 0 ? -1.0F : totalWidth + 2.0F), drawY + height + (this.posY.getValue() == 0 ? 0.0F : (finalY == 0L ? 1.0F : 0.0F)), color);
                    }
                }
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();

                if (this.shadow.getValue()) getFont().drawWithShadow(moduleName, drawX, drawY, color);
                else getFont().draw(moduleName, drawX, drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F), color, false);

                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width = (float) getFont().getStringWidth(moduleName) + 3.0F;
                    int suffixColor = ((int)(160 * animProgress) << 24) | 0x00AAAAAA;
                    for (String string : moduleSuffix) {
                        if (this.shadow.getValue()) getFont().drawWithShadow(string, drawX + width, drawY, suffixColor);
                        else getFont().draw(string, drawX + width, drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F), suffixColor, false);
                        width += (float) getFont().getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }
            }

            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Myau.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        getFont().draw(String.valueOf(movementPacketSize), (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue() - (float) getFont().getStringWidth(String.valueOf(movementPacketSize)) / 2.0F, (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(), this.getColor(l, 0L).getRGB() & 16777215 | -1090519040, this.shadow.getValue());
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }

        // Render Potion Effects
        if (mc.thePlayer != null) {
            java.util.Collection<net.minecraft.potion.PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
            if (!effects.isEmpty()) {
                myau.util.font.Font font = getFont();
                float drawY = new ScaledResolution(mc).getScaledHeight() - 3;
                
                java.util.List<net.minecraft.potion.PotionEffect> sortedEffects = new ArrayList<>(effects);
                sortedEffects.sort((a, b) -> {
                    String nameA = net.minecraft.client.resources.I18n.format(a.getEffectName());
                    String nameB = net.minecraft.client.resources.I18n.format(b.getEffectName());
                    String timeA = net.minecraft.potion.Potion.getDurationString(a);
                    String timeB = net.minecraft.potion.Potion.getDurationString(b);
                    String textA = (lowerCase.getValue() ? nameA.toLowerCase() : nameA) + (a.getAmplifier() > 0 ? " " + (a.getAmplifier() + 1) : "") + " §7" + timeA;
                    String textB = (lowerCase.getValue() ? nameB.toLowerCase() : nameB) + (b.getAmplifier() > 0 ? " " + (b.getAmplifier() + 1) : "") + " §7" + timeB;
                    return Float.compare(-font.getStringWidth(textA), -font.getStringWidth(textB));
                });

                for (net.minecraft.potion.PotionEffect effect : sortedEffects) {
                    net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.potionTypes[effect.getPotionID()];
                    if (potion == null) continue;

                    String name = net.minecraft.client.resources.I18n.format(potion.getName());
                    if (lowerCase.getValue()) name = name.toLowerCase();
                    if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);

                    String time = net.minecraft.potion.Potion.getDurationString(effect);
                    String text = name + " §7" + time;
                    int textWidth = font.getStringWidth(text);
                    float drawX = new ScaledResolution(mc).getScaledWidth() - 2;

                    drawY -= (font.height() + 1.5f);
                    
                    int effectColor = potion.getLiquidColor() | 0xFF000000;
                    font.drawWithShadow(text, drawX - textWidth - 1, drawY, effectColor);

                    if (potion.hasStatusIcon()) {
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        GlStateManager.enableBlend();
                        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("textures/gui/container/inventory.png"));
                        int iconIndex = potion.getStatusIconIndex();
                        net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(
                                (int) (drawX - textWidth - 14), (int) drawY, 
                                (iconIndex % 8) * 18, 198 + (iconIndex / 8) * 18, 
                                18, 18, 9, 9, 256, 256);
                        GlStateManager.disableBlend();
                    }
                }
            }
        }
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

    private void renderBPSBackground(boolean blur) {
        myau.util.font.Font titleFont = myau.util.font.Fonts.MAIN.get(18, myau.util.font.Weight.BOLD);
        myau.util.font.Font bpsFont = myau.util.font.Fonts.MAIN.get(18, myau.util.font.Weight.REGULAR);
        
        float titleWidth = titleFont.getStringWidth("BPS ");
        float bpsWidth = bpsFont.getStringWidth(this.bpsString);
        float width = titleWidth + bpsWidth + 6;
        float height = Math.max(titleFont.getFontHeight(), bpsFont.getFontHeight()) + 4;
        
        float x = 5;
        float y;
        if (this.hudMode.getValue() == 1 && this.showCoordinates.getValue() && mc.thePlayer != null) {
            y = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight() - 10 - getFont().getFontHeight() * 2 - 4 - height;
        } else {
            y = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight() - height - 5;
        }
        
        if (blur) {
            myau.util.shader.RoundedUtils.drawRound(x, y, width, height, 4.0f, true, Color.black);
        } else {
            myau.util.shader.RoundedUtils.drawRound(x, y, width, height, 4.0f, false, new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F));
        }
    }

    private void renderBPSText(long l) {
        myau.util.font.Font titleFont = myau.util.font.Fonts.MAIN.get(18, myau.util.font.Weight.BOLD);
        myau.util.font.Font bpsFont = myau.util.font.Fonts.MAIN.get(18, myau.util.font.Weight.REGULAR);
        
        float titleWidth = titleFont.getStringWidth("BPS ");
        float height = Math.max(titleFont.getFontHeight(), bpsFont.getFontHeight()) + 4;
        
        float x = 5;
        float y;
        if (this.hudMode.getValue() == 1 && this.showCoordinates.getValue() && mc.thePlayer != null) {
            y = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight() - 10 - getFont().getFontHeight() * 2 - 4 - height;
        } else {
            y = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight() - height - 5;
        }
        
        float textX = x + 3.0F;
        float textY = y + height / 2.0F - bpsFont.getFontHeight() / 2.0F;
        
        titleFont.drawWithShadow("BPS ", textX, textY, this.getColor(l).getRGB());
        bpsFont.drawWithShadow(this.bpsString, textX + titleWidth, textY, Color.WHITE.getRGB());
    }
}