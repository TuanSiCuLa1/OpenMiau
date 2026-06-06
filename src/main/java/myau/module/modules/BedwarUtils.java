package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarUtils extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty hud = new BooleanProperty("hud", true);
    public final IntProperty hudX = new IntProperty("hud-x", 4, 0, 500, this.hud::getValue);
    public final IntProperty hudY = new IntProperty("hud-y", 66, 0, 500, this.hud::getValue);
    public final FloatProperty hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 2.0F, this.hud::getValue);
    public final BooleanProperty hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);
    public final BooleanProperty diamondUpgrades = new BooleanProperty("diamond-upgrades", true);
    public final BooleanProperty itemTracker = new BooleanProperty("item-tracker", true);

    private static final Pattern ITEM_TRACKER_PATTERN = Pattern.compile("^(.+?)\\s+has\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private final Set<String> trackedItemMessages = new HashSet<>();

    private boolean trap;
    private boolean sharp;
    private int protLevel;

    public BedwarUtils() {
        super("BedwarUtils", false, false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE
                || !(event.getPacket() instanceof S02PacketChat)) {
            return;
        }
        String text = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
        String formattedText = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
        this.scanMessage(text, formattedText);
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.hud.getValue()) {
            return;
        }
        float scale = this.hudScale.getValue();
        float x = this.hudX.getValue() / scale;
        float y = this.hudY.getValue() / scale;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        float rowY = y;
        if (this.diamondUpgrades.getValue()) {
            this.drawLine("Trap", this.trap, -1, x, rowY);
            rowY += 10.0F;
            this.drawLine("Sharp", this.sharp, -1, x, rowY);
            rowY += 10.0F;
            this.drawLine("Prot", this.protLevel > 0, this.protLevel, x, rowY);
        }
        GlStateManager.popMatrix();
    }

    private void scanMessage(String text, String formattedText) {
        if (text == null) {
            return;
        }
        String lower = text.toLowerCase();
        if (this.diamondUpgrades.getValue()) {
            if (lower.contains("trap") || lower.contains("it's a trap") || lower.contains("alarm trap")
                    || lower.contains("miner fatigue")) {
                this.trap = true;
            }
            if (lower.contains("sharpened swords") || lower.contains("sharpness") || lower.contains("sharp")) {
                this.sharp = true;
            }
            if (lower.contains("reinforced armor") || lower.contains("protection") || lower.contains("prot")) {
                int level = this.parseProtLevel(lower);
                this.protLevel = Math.max(this.protLevel, level <= 0 ? 1 : level);
            }
        }
        this.scanItemTracker(text, formattedText);
    }

    private void scanItemTracker(String text, String formattedText) {
        if (!this.itemTracker.getValue()) {
            return;
        }
        Matcher matcher = ITEM_TRACKER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return;
        }
        String item = matcher.group(2).trim();
        if (!this.isTrackedItem(item)) {
            return;
        }
        String key = text.toLowerCase();
        if (!this.trackedItemMessages.add(key)) {
            return;
        }
        this.sendItemTrackerMessage(this.extractFormattedPlayer(formattedText, matcher.group(1).trim()), item);
    }

    private boolean isTrackedItem(String item) {
        String lower = item.toLowerCase();
        return lower.contains("sword")
                || lower.contains("armor")
                || lower.contains("chestplate")
                || lower.contains("leggings")
                || lower.contains("boots")
                || lower.contains("helmet")
                || lower.contains("bow")
                || lower.contains("pickaxe")
                || lower.contains("axe")
                || lower.contains("shears");
    }

    private String extractFormattedPlayer(String formattedText, String fallback) {
        if (formattedText == null) {
            return fallback;
        }
        String marker = " has ";
        int index = formattedText.toLowerCase().indexOf(marker);
        return index > 0 ? formattedText.substring(0, index) : fallback;
    }

    private int parseProtLevel(String text) {
        if (text.contains(" iv") || text.contains(" 4") || text.contains("level iv") || text.contains("level 4"))
            return 4;
        if (text.contains(" iii") || text.contains(" 3") || text.contains("level iii") || text.contains("level 3"))
            return 3;
        if (text.contains(" ii") || text.contains(" 2") || text.contains("level ii") || text.contains("level 2"))
            return 2;
        if (text.contains(" i") || text.contains(" 1") || text.contains("level i") || text.contains("level 1"))
            return 1;
        return 0;
    }

    private void drawLine(String name, boolean value, int level, float x, float y) {
        int white = 0xFFFFFFFF;
        int green = 0xFF55FF55;
        int red = 0xFFFF5555;
        boolean shadow = this.hudShadow.getValue();
        String prefix = "- " + name + ": ";
        mc.fontRendererObj.drawString(prefix, x, y, white, shadow);
        float valueX = x + mc.fontRendererObj.getStringWidth(prefix);
        mc.fontRendererObj.drawString(value ? "true" : "false", valueX, y, value ? green : red, shadow);
        if (level > 0) {
            String suffix = " [" + this.toRoman(level) + "]";
            mc.fontRendererObj.drawString(suffix, valueX + mc.fontRendererObj.getStringWidth("true"), y, white, shadow);
        }
    }

    private String toRoman(int level) {
        switch (level) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return String.valueOf(level);
        }
    }

    private void reset() {
        this.trap = false;
        this.sharp = false;
        this.protLevel = 0;
        this.trackedItemMessages.clear();
    }

    private void sendItemTrackerMessage(String formattedPlayer, String item) {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(this.getMyauPrefix() + " §f" + formattedPlayer + " §fhas §a" + item));
    }

    private String getMyauPrefix() {
        return "§7[" + this.getHudColorCode() + "M§fyau§7]";
    }

    private String getHudColorCode() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        Color color = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE;
        return this.nearestChatColor(color);
    }

    private String nearestChatColor(Color color) {
        String[] codes = new String[]{"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
        Color[] colors = new Color[]{
                new Color(0, 0, 0), new Color(0, 0, 170), new Color(0, 170, 0), new Color(0, 170, 170),
                new Color(170, 0, 0), new Color(170, 0, 170), new Color(255, 170, 0), new Color(170, 170, 170),
                new Color(85, 85, 85), new Color(85, 85, 255), new Color(85, 255, 85), new Color(85, 255, 255),
                new Color(255, 85, 85), new Color(255, 85, 255), new Color(255, 255, 85), new Color(255, 255, 255)
        };
        int best = 15;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            double red = color.getRed() - colors[i].getRed();
            double green = color.getGreen() - colors[i].getGreen();
            double blue = color.getBlue() - colors[i].getBlue();
            double distance = red * red + green * green + blue * blue;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return codes[best];
    }
}
