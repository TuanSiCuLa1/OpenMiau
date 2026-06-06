package myau.module.modules;

import myau.Myau;
import myau.clientanticheat.AimDuplicateLookCheck;
import myau.clientanticheat.AimModulo360Check;
import myau.clientanticheat.AutoBlockCheck;
import myau.clientanticheat.BadPacketsCheck;
import myau.clientanticheat.ClientAntiCheatContext;
import myau.clientanticheat.KillAuraCheck;
import myau.clientanticheat.NoSlowCheck;
import myau.clientanticheat.ReachCheck;
import myau.clientanticheat.ScaffoldCheck;
import myau.clientanticheat.SprintCheck;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HackerDetector extends Module implements ClientAntiCheatContext {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int FLAG_WINDOW_SECONDS = 5;
    private static final int ALERT_COOLDOWN_SECONDS = 5;

    public final BooleanProperty autoBlock = new BooleanProperty("autoblock", true);
    public final BooleanProperty noSlow = new BooleanProperty("noslow", true);
    public final BooleanProperty killAura = new BooleanProperty("killaura", true);
    public final BooleanProperty scaffold = new BooleanProperty("scaffold", true);
    public final BooleanProperty aimDuplicateLook = new BooleanProperty("aim-duplicate-look", true);
    public final BooleanProperty aimModulo360 = new BooleanProperty("aim-modulo-360", true);
    public final BooleanProperty reach = new BooleanProperty("reach", true);
    public final BooleanProperty sprint = new BooleanProperty("sprint", true);
    public final BooleanProperty badPackets = new BooleanProperty("bad-packets", true);
    public final BooleanProperty addTarget = new BooleanProperty("add-target", true);
    public final BooleanProperty sound = new BooleanProperty("sound", true);

    private final AutoBlockCheck autoBlockCheck = new AutoBlockCheck();
    private final NoSlowCheck noSlowCheck = new NoSlowCheck();
    private final KillAuraCheck killAuraCheck = new KillAuraCheck();
    private final ScaffoldCheck scaffoldCheck = new ScaffoldCheck();
    private final AimDuplicateLookCheck aimDuplicateLookCheck = new AimDuplicateLookCheck();
    private final AimModulo360Check aimModulo360Check = new AimModulo360Check();
    private final ReachCheck reachCheck = new ReachCheck();
    private final SprintCheck sprintCheck = new SprintCheck();
    private final BadPacketsCheck badPacketsCheck = new BadPacketsCheck();
    private final Map<String, int[]> flagMap = new HashMap<>();
    private final Map<String, Integer> alertCooldowns = new HashMap<>();
    private final Set<String> whitelist = new HashSet<>();

    public HackerDetector() {
        super("HackerDetector", false, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        World world = mc.theWorld;
        long currentTick = world.getTotalWorldTime();
        for (EntityPlayer player : new ArrayList<>(world.playerEntities)) {
            if (player == mc.thePlayer || player.isDead || player.getName() == null) {
                continue;
            }
            if (this.autoBlock.getValue()) {
                this.autoBlockCheck.check(player, currentTick, this);
            }
            if (this.noSlow.getValue()) {
                this.noSlowCheck.check(player, currentTick, this);
            }
            if (this.killAura.getValue()) {
                this.killAuraCheck.check(player, world, currentTick, this);
            }
            if (this.scaffold.getValue()) {
                this.scaffoldCheck.check(player, world, this);
            }
            if (this.aimDuplicateLook.getValue()) {
                this.aimDuplicateLookCheck.check(player, world, this);
            }
            if (this.aimModulo360.getValue()) {
                this.aimModulo360Check.check(player, this);
            }
            if (this.reach.getValue()) {
                this.reachCheck.check(player, world, this);
            }
            if (this.sprint.getValue()) {
                this.sprintCheck.check(player, this);
            }
            if (this.badPackets.getValue()) {
                this.badPacketsCheck.check(player, this);
            }
        }
        this.pruneFlags();
    }

    @Override
    public void receiveSignal(String playerName, String cheatName) {
        if (playerName == null || playerName.isEmpty() || cheatName == null) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (playerName.equalsIgnoreCase(mc.thePlayer.getName())) return;
        if (this.isWhitelisted(playerName)) return;

        int currentTime = (int) (mc.theWorld.getTotalWorldTime() / 20);
        String flagKey = this.getFlagKey(playerName, cheatName);
        int[] flagData = this.flagMap.getOrDefault(flagKey, new int[]{0, currentTime});
        if (currentTime - flagData[1] > FLAG_WINDOW_SECONDS) {
            flagData[0] = 0;
        }
        flagData[0] += 1;
        flagData[1] = currentTime;
        this.flagMap.put(flagKey, flagData);

        int maxFlagCount = this.maxFlagsFor(cheatName);
        int lastAlert = this.alertCooldowns.getOrDefault(flagKey, -ALERT_COOLDOWN_SECONDS);
        if (flagData[0] >= maxFlagCount && currentTime - lastAlert >= ALERT_COOLDOWN_SECONDS) {
            ChatUtil.sendFormatted(String.format(
                    "%s%s%s%s failed %s%s",
                    Myau.clientName,
                    EnumChatFormatting.RED,
                    playerName,
                    EnumChatFormatting.GRAY,
                    EnumChatFormatting.RED,
                    cheatName
            ));
            if (this.sound.getValue()) {
                mc.thePlayer.playSound("random.orb", 0.3F, 1.0F);
            }
            if (this.addTarget.getValue() && Myau.targetManager != null) {
                Myau.targetManager.add(playerName);
            }
            this.alertCooldowns.put(flagKey, currentTime);
            this.flagMap.remove(flagKey);
        }
    }

    private void pruneFlags() {
        int currentTime = (int) (mc.theWorld.getTotalWorldTime() / 20);
        Map<String, int[]> nextFlagMap = new HashMap<>();
        for (Map.Entry<String, int[]> entry : this.flagMap.entrySet()) {
            int[] flagData = entry.getValue();
            if (currentTime - flagData[1] <= FLAG_WINDOW_SECONDS) {
                nextFlagMap.put(entry.getKey(), flagData);
            }
        }
        this.flagMap.clear();
        this.flagMap.putAll(nextFlagMap);
        this.alertCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > ALERT_COOLDOWN_SECONDS);
    }

    private boolean isWhitelisted(String playerName) {
        for (String name : this.whitelist) {
            if (name.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    private int maxFlagsFor(String cheatName) {
        if (cheatName.equals("AutoBlock")) return 5;
        if (cheatName.equals("Noslow")) return 3;
        if (cheatName.equals("KillAura")) return 4;
        if (cheatName.equals("Scaffold")) return 4;
        return 2;
    }

    private String getFlagKey(String playerName, String cheatName) {
        return playerName.toLowerCase(Locale.ROOT) + ":" + cheatName;
    }

    @Override
    public void onDisabled() {
        this.autoBlockCheck.reset();
        this.noSlowCheck.reset();
        this.killAuraCheck.reset();
        this.scaffoldCheck.reset();
        this.aimDuplicateLookCheck.reset();
        this.aimModulo360Check.reset();
        this.reachCheck.reset();
        this.sprintCheck.reset();
        this.badPacketsCheck.reset();
        this.flagMap.clear();
        this.alertCooldowns.clear();
    }
}
