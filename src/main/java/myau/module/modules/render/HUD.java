package myau.module.modules.render;

import myau.module.modules.combat.AimAssist;
import myau.module.modules.movement.AntiAFK;
import myau.module.modules.misc.AntiBot;
import myau.module.modules.misc.AntiCheatDetector;
import myau.module.modules.player.AntiDebuff;
import myau.module.modules.combat.AntiFireball;
import myau.module.modules.misc.AntiObbyTrap;
import myau.module.modules.misc.AntiObfuscate;
import myau.module.modules.movement.AntiVoid;
import myau.module.modules.misc.AutoAnduril;
import myau.module.modules.misc.AutoAuth;
import myau.module.modules.player.AutoBedDef;
import myau.module.modules.player.AutoBlockIn;
import myau.module.modules.combat.AutoClicker;
import myau.module.modules.player.AutoHeal;
import myau.module.modules.player.AutoPot;
import myau.module.modules.player.AutoRod;
import myau.module.modules.player.AutoTool;
import myau.module.modules.latency.BackTrack;
import myau.module.modules.misc.BedNuker;
import myau.module.modules.minigames.BedwarUtils;
import myau.module.modules.movement.Blink;
import myau.module.modules.player.ChestStealer;
import myau.module.modules.misc.ClientSpoofer;
import myau.module.modules.movement.Eagle;
import myau.module.modules.latency.FakeLag;
import myau.module.modules.player.FastPlace;
import myau.module.modules.movement.Fly;
import myau.module.modules.combat.Freeze;
import myau.module.modules.player.GhostHand;
import myau.module.modules.misc.HackerDetector;
import myau.module.modules.combat.HitBox;
import myau.module.modules.combat.HitSelect;
import myau.module.modules.combat.Hitflick;
import myau.module.modules.player.InvManager;
import myau.module.modules.player.InvWalk;
import myau.module.modules.misc.InventoryClicker;
import myau.module.modules.movement.Jesus;
import myau.module.modules.movement.KeepSprint;
import myau.module.modules.combat.KillAura;
import myau.module.modules.latency.LagRange;
import myau.module.modules.misc.LightningTracker;
import myau.module.modules.movement.LongJump;
import myau.module.modules.player.MCF;
import myau.module.modules.combat.MoreKB;
import myau.module.modules.misc.MurderDetector;
import myau.module.modules.misc.NickHider;
import myau.module.modules.movement.NoFall;
import myau.module.modules.combat.NoHitDelay;
import myau.module.modules.movement.NoJumpDelay;
import myau.module.modules.misc.NoRotate;
import myau.module.modules.movement.NoSlow;
import myau.module.modules.misc.Panic;
import myau.module.modules.combat.ProjectileAimBot;
import myau.module.modules.misc.RPC;
import myau.module.modules.combat.Reach;
import myau.module.modules.combat.Refill;
import myau.module.modules.movement.SafeWalk;
import myau.module.modules.player.Scaffold;
import myau.module.modules.misc.Spammer;
import myau.module.modules.movement.Speed;
import myau.module.modules.player.SpeedMine;
import myau.module.modules.movement.Sprint;
import myau.module.modules.combat.TargetStrafe;
import myau.module.modules.latency.TickBase;
import myau.module.modules.combat.Velocity;
import myau.module.modules.combat.Wtap;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorGuiChat;
import myau.module.Module;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.property.properties.*;
import myau.util.Themes;
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
    private final java.util.Map<Module, Double> animationMap = new java.util.HashMap<>();
    private final java.util.Map<Module, Float> yMap = new java.util.HashMap<>();
    private long lastMS = System.currentTimeMillis();
    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty hudMode = new ModeProperty("hud-mode", 0, new String[]{"NORMAL", "EXHIBITION"});
    public final TextProperty watermarkName = new TextProperty("watermark-name", "Miau", () -> this.showWatermark.getValue());
    public final BooleanProperty showWatermark = new BooleanProperty("watermark", true);
    public final BooleanProperty showCoordinates = new BooleanProperty("coordinates", true, () -> this.hudMode.getValue() == 1);
    public final BooleanProperty showTime = new BooleanProperty("show-time", true, () -> this.showWatermark.getValue());
    public final BooleanProperty showFps = new BooleanProperty("show-fps", true, () -> this.showWatermark.getValue());
    public final BooleanProperty showPing = new BooleanProperty("show-ping", true, () -> this.showWatermark.getValue());

    private static String[] getCombinedColorModes() {
        String[] oldColors = new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"};
        String[] themeColors = java.util.Arrays.stream(Themes.values()).map(Themes::getThemeName).toArray(String[]::new);
        String[] combined = new String[oldColors.length + themeColors.length];
        System.arraycopy(oldColors, 0, combined, 0, oldColors.length);
        System.arraycopy(themeColors, 0, combined, oldColors.length, themeColors.length);
        return combined;
    }

    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, getCombinedColorModes()
    );
    public final ModeProperty colorAnimation = new ModeProperty(
            "color-animation", 1, new String[]{"STATIC", "FADE", "RAINBOW"},
            () -> this.colorMode.getValue() >= 6
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F, () -> this.colorMode.getValue() < 6);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50, () -> this.colorMode.getValue() < 6);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100, () -> this.colorMode.getValue() < 6);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final IntProperty notifX = new IntProperty("notif-x", 0, -2000, 2000);
    public final IntProperty notifY = new IntProperty("notif-y", 0, -2000, 2000);
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
    public final BooleanProperty hideCombat = new BooleanProperty("hide-combat", false);
    public final BooleanProperty hideMovement = new BooleanProperty("hide-movement", false);
    public final BooleanProperty hideRender = new BooleanProperty("hide-render", false);
    public final BooleanProperty hidePlayer = new BooleanProperty("hide-player", false);
    public final BooleanProperty hideMisc = new BooleanProperty("hide-misc", false);
    public final BooleanProperty hideLatency = new BooleanProperty("hide-latency", false);

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
        return this.calculateStringWidth(
                this.getModuleName(module), this.getModuleSuffix(module)
        );
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = mc.fontRendererObj.getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + mc.fontRendererObj.getStringWidth(str);
            }
        }
        return width;
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    public HUD() {
        super("HUD", true, true);
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        int index = this.colorMode.getValue();
        if (index < 6) {
            Color color = Color.white;
            switch (index) {
                case 0:
                    color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                    break;
                case 1:
                    color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                    break;
                case 2:
                    float cycle = this.getColorCycle(time, offset);
                    if (cycle % 1.0F < 0.5F) {
                        cycle = 1.0F - cycle % 1.0F;
                    }
                    color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                    break;
                case 3:
                    color = new Color(this.custom1.getValue());
                    break;
                case 4:
                    double cycle1 = this.getColorCycle(time, offset);
                    color = ColorUtil.interpolate(
                            (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                            new Color(this.custom1.getValue()),
                            new Color(this.custom2.getValue())
                    );
                    break;
                case 5:
                    double cycle2 = this.getColorCycle(time, offset);
                    float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                    if (floor <= 0.5F) {
                        color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                    } else {
                        color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                    }
            }
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            return Color.getHSBColor(
                    hsb[0],
                    hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                    hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
            );
        } else {
            Themes theme = Themes.values()[index - 6];
            Themes.setCurrentTheme(theme);

            switch (this.colorAnimation.getValue()) {
                case 0: // STATIC
                    return theme.getFirstColor();
                case 1: // FADE
                    return theme.getAccentColor(new Vector2d(0, offset * 15));
                case 2: // RAINBOW
                    return ColorUtil.rainbow((int) (offset * 500 / 6));
                default:
                    return Color.white;
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Myau.moduleManager.modules.values().stream().filter(module -> module.isEnabled() && !module.isHidden() && !this.isCategoryHidden(module)).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
            try {
                Myau.clientName = ChatColors.getDynamicPrefix();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private boolean isCategoryHidden(Module module) {
        String category = module.getCategory();
        if (category == null) {
            return false;
        }
        switch (category) {
            case "combat":
                return this.hideCombat.getValue();
            case "movement":
                return this.hideMovement.getValue();
            case "render":
                return this.hideRender.getValue();
            case "player":
                return this.hidePlayer.getValue();
            case "misc":
                return this.hideMisc.getValue();
            case "latency":
                return this.hideLatency.getValue();
            default:
                return false;
        }
    }

    private String getExhibitionWatermark() {
        String customName = this.watermarkName.getValue();
        if (customName == null || customName.isEmpty()) {
            customName = "Miau";
        }
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
            double currentAnim = animationMap.getOrDefault(module, 0.0);
            boolean shouldBeVisible = module.isEnabled() && !module.isHidden() && !this.isCategoryHidden(module);
            
            if (shouldBeVisible) {
                currentAnim = Math.min(1.0, currentAnim + (delta * 0.006));
            } else {
                currentAnim = Math.max(0.0, currentAnim - (delta * 0.006));
            }
            animationMap.put(module, currentAnim);
        }

        java.util.List<Module> animatingModules = Myau.moduleManager.modules.values().stream()
                .filter(module -> animationMap.getOrDefault(module, 0.0) > 0.001)
                .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                .collect(Collectors.toList());

        float heightExhibition = (float) mc.fontRendererObj.FONT_HEIGHT + 2.0F;
        float heightNormal = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
        float currentYExhibition = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
        float currentYNormal = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();

        if (this.posY.getValue() == 1) {
            currentYExhibition = (float) new ScaledResolution(mc).getScaledHeight() - currentYExhibition - heightExhibition * this.scale.getValue();
            currentYNormal = (float) new ScaledResolution(mc).getScaledHeight() - currentYNormal - heightNormal * this.scale.getValue();
        }

        for (Module module : animatingModules) {
            float targetY = (this.hudMode.getValue() == 1) ? currentYExhibition : currentYNormal;
            float currentY = yMap.getOrDefault(module, targetY);
            
            currentY += (targetY - currentY) * Math.min(1.0f, 0.012f * delta);
            yMap.put(module, currentY);

            if (module.isEnabled() && !module.isHidden() && !this.isCategoryHidden(module)) {
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
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            long l = System.currentTimeMillis();
            // Render watermark
            if (this.showWatermark.getValue()) {
                String watermark = getExhibitionWatermark();
                if (watermark != null) {
                    int colour = this.getColor(l).getRGB();
                    mc.fontRendererObj.drawStringWithShadow(watermark, 3.0F, 3.0F, colour);
                }
            }
            if (this.hudMode.getValue() == 1) { // Exhibition mode
                // Render coordinates
                if (this.showCoordinates.getValue() && mc.thePlayer != null) {
                    String posX2 = String.valueOf(Math.round(mc.thePlayer.posX));
                    String posY2 = String.valueOf(Math.round(mc.thePlayer.posY));
                    String posZ2 = String.valueOf(Math.round(mc.thePlayer.posZ));
                    float yCoord = new ScaledResolution(mc).getScaledHeight() - 10;
                    float fontHeight = mc.fontRendererObj.FONT_HEIGHT;
                    int colour = this.getColor(l).getRGB();
                    mc.fontRendererObj.drawStringWithShadow("X: §7" + posX2, 3.0F, yCoord - fontHeight * 2, colour);
                    mc.fontRendererObj.drawStringWithShadow("Y: §7" + posY2, 3.0F, yCoord - fontHeight, colour);
                    mc.fontRendererObj.drawStringWithShadow("Z: §7" + posZ2, 3.0F, yCoord, colour);
                }

                // Render ArrayList (Exhibition style)
                float height = (float) mc.fontRendererObj.FONT_HEIGHT + 2.0F;
                float x = (float) this.offsetX.getValue();
                float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
                if (this.posX.getValue() == 1) {
                    x = (float) new ScaledResolution(mc).getScaledWidth() - x;
                }
                if (this.posY.getValue() == 1) {
                    y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * this.scale.getValue();
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
                long offset = 0L;
                
                for (Module module : animatingModules) {
                    String moduleName = this.getModuleName(module);
                    String[] moduleSuffix = this.getModuleSuffix(module);
                    float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                    
                    double animProgress = animationMap.getOrDefault(module, 0.0);
                    float drawY = yMap.getOrDefault(module, 0.0f) / this.scale.getValue();
                    
                    // Tính độ lệch X để tạo hiệu ứng phóng/trượt từ viền màn hình vào
                    float xOffset = (float) ((1.0 - animProgress) * (totalWidth + 10));
                    float drawX = x / this.scale.getValue();
                    if (this.posX.getValue() == 1) drawX += xOffset - totalWidth; // Trượt từ bên phải
                    else drawX -= xOffset; // Trượt từ bên trái
                    
                    // Làm mờ dần màu sắc (Fade Alpha) theo tiến trình animation
                    int alpha = (int) (255 * animProgress);
                    int color = (alpha << 24) | (this.getColor(l, offset).getRGB() & 0x00FFFFFF);
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, (this.background.getValue().floatValue() / 100.0F) * (float)animProgress).getRGB();
                    
                    RenderUtil.enableRenderState();
                    if (this.background.getValue() > 0) {
                        RenderUtil.drawRect(drawX - 2.0F, drawY - 2.0F, drawX + totalWidth + 2.0F, drawY + height - 2.0F, bgColor);
                    }
                    
                    if (this.showBar.getValue()) {
                        int barColor = color;
                        if (this.posX.getValue() == 0) {
                            RenderUtil.drawRect(drawX - 3.0F, drawY - 2.0F, drawX - 2.0F, drawY + height - 2.0F, barColor);
                        } else {
                            RenderUtil.drawRect(drawX + totalWidth + 2.0F, drawY - 2.0F, drawX + totalWidth + 3.0F, drawY + height - 2.0F, barColor);
                        }
                    }
                    RenderUtil.disableRenderState();
                    
                    mc.fontRendererObj.drawStringWithShadow(moduleName, drawX, drawY, color);
                    
                    if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                        float suffixX = drawX + mc.fontRendererObj.getStringWidth(moduleName) + 2.0F;
                        int suffixColor = ((int)(170 * animProgress) << 24) | 0x00AAAAAA; // Suffix xám mờ dần
                        for (String str : moduleSuffix) {
                            mc.fontRendererObj.drawStringWithShadow(str, suffixX, drawY, suffixColor);
                            suffixX += mc.fontRendererObj.getStringWidth(str) + 2.0F;
                        }
                    }
                    offset++;
                }
                GlStateManager.popMatrix();
            } else { // Normal mode
                float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
                float x = (float) this.offsetX.getValue()
                        + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
                float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
                if (this.posX.getValue() == 1) {
                    x = (float) new ScaledResolution(mc).getScaledWidth() - x;
                }
                if (this.posY.getValue() == 1) {
                    y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * this.scale.getValue();
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
                long offset = 0L;
                
                // [THAY THẾ VÒNG LẶP]
                for (Module module : animatingModules) {
                    String moduleName = this.getModuleName(module);
                    String[] moduleSuffix = this.getModuleSuffix(module);
                    float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                    
                    // Áp dụng animation toán học bộ lọc
                    double animProgress = animationMap.getOrDefault(module, 0.0);
                    float drawY = yMap.getOrDefault(module, 0.0f) / this.scale.getValue();
                    
                    float xOffset = (float) ((1.0 - animProgress) * (totalWidth + 10));
                    float baseX = x / this.scale.getValue();
                    float drawX = baseX - (this.posX.getValue() == 1 ? totalWidth : 0.0F);
                    if (this.posX.getValue() == 1) drawX += xOffset;
                    else drawX -= xOffset;
                    
                    int alpha = (int) (255 * animProgress);
                    int color = (alpha << 24) | (this.getColor(l, offset).getRGB() & 0x00FFFFFF);
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, (this.background.getValue().floatValue() / 100.0F) * (float)animProgress).getRGB();
                    
                    RenderUtil.enableRenderState();
                    if (this.background.getValue() > 0) {
                        RenderUtil.drawRect(
                                drawX - 1.0F,
                                drawY - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                                drawX + totalWidth + 1.0F,
                                drawY + height + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                                bgColor
                        );
                    }
                    if (this.showBar.getValue()) {
                        if (this.shadow.getValue()) {
                            RenderUtil.drawRect(drawX + (this.posX.getValue() == 0 ? -3.0F : totalWidth + 1.0F), drawY - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F), drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 2.0F), drawY + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)), color);
                        } else {
                            RenderUtil.drawRect(drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 1.0F), drawY - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F), drawX + (this.posX.getValue() == 0 ? -1.0F : totalWidth + 2.0F), drawY + height + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)), color);
                        }
                    }
                    RenderUtil.disableRenderState();
                    GlStateManager.disableDepth();
                    
                    if (this.shadow.getValue()) {
                        mc.fontRendererObj.drawStringWithShadow(moduleName, drawX, drawY, color);
                    } else {
                        mc.fontRendererObj.drawString(moduleName, drawX, drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F), color, false);
                    }
                    
                    if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                        float width = (float) mc.fontRendererObj.getStringWidth(moduleName) + 3.0F;
                        int suffixColor = ((int)(160 * animProgress) << 24) | 0x00AAAAAA;
                        for (String string : moduleSuffix) {
                            if (this.shadow.getValue()) {
                                mc.fontRendererObj.drawStringWithShadow(string, drawX + width, drawY, suffixColor);
                            } else {
                                mc.fontRendererObj.drawString(string, drawX + width, drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F), suffixColor, false);
                            }
                            width += (float) mc.fontRendererObj.getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                        }
                    }
                    offset++;
                }
                if (this.blinkTimer.getValue()) {
                    BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                    if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                        long movementPacketSize = Myau.blinkManager.countMovement();
                        if (movementPacketSize > 0L) {
                            GlStateManager.enableBlend();
                            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                            mc.fontRendererObj
                                    .drawString(
                                            String.valueOf(movementPacketSize),
                                            (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                                    - (float) mc.fontRendererObj.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                            (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                            this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                            this.shadow.getValue()
                                    );
                            GlStateManager.disableBlend();
                        }
                    }
                }
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
            if (mc.thePlayer != null) {
                java.util.Collection<net.minecraft.potion.PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
                if (!effects.isEmpty()) {
                    myau.util.font.Font font = myau.util.font.Fonts.MAIN.get(18);
                    int drawY = new ScaledResolution(mc).getScaledHeight() - 22;
                    for (net.minecraft.potion.PotionEffect effect : effects) {
                        net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.potionTypes[effect.getPotionID()];
                        if (potion == null) continue;

                        String name = net.minecraft.client.resources.I18n.format(potion.getName());
                        if (effect.getAmplifier() > 0) {
                            name += " " + toRoman(effect.getAmplifier() + 1);
                        }
                        String time = net.minecraft.potion.Potion.getDurationString(effect);
                        String text = name + " | " + time;

                        int textWidth = font.getStringWidth(text);
                        int totalWidth = 18 + 4 + textWidth;
                        int drawX = new ScaledResolution(mc).getScaledWidth() - totalWidth - 6;

                        RenderUtil.renderPotionEffect(effect, drawX, drawY);
                        font.drawWithShadow(text, drawX + 22, drawY + (18.0f - font.height()) / 2.0f, 0xFFFFFFFF);

                        drawY -= 20;
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
}
