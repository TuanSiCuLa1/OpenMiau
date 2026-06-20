package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.mixin.IAccessorS14PacketEntity;
<<<<<<< HEAD
import myau.util.client.ChatUtil;
import myau.Myau;
=======
import myau.util.ChatUtil;
import myau.Myau;
import myau.notification.NotificationManager;
>>>>>>> 746610b90671b5ee596a876af938a43584190552
import myau.notification.NotificationType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;

import java.util.*;

public class StaffDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty vanishDetect = new BooleanProperty("vanish-detect", true);
    public final BooleanProperty teamScanner = new BooleanProperty("team-scanner", true);
    public final BooleanProperty autoLeave = new BooleanProperty("auto-leave", false);

    private static final Set<String> STAFF_LIST = new HashSet<>(Arrays.asList(
        "vinghgaming",
        "cheesethesylveon",
        "thanhhau",
        "sennekoi",
        "lasgana",
        "novapev4",
        "khoaho01623"
    ));

    private final Set<Integer> validEntities = new HashSet<>();
    private final Set<Integer> flaggedGhost = new HashSet<>();
    private final Set<String> alertedStaffTeams = new HashSet<>();
    private final Set<String> alertedStaffPlayers = new HashSet<>();

    public StaffDetector() {
        super("StaffDetector", false, false);
    }

    @Override
    public void onEnabled() {
        clearCache();
    }

    @Override
    public void onDisabled() {
        clearCache();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clearCache();
    }

    private void clearCache() {
        validEntities.clear();
        flaggedGhost.clear();
        alertedStaffTeams.clear();
        alertedStaffPlayers.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem pkt = (S38PacketPlayerListItem) packet;

            if (pkt.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData data : pkt.getEntries()) {
                    if (data == null || data.getProfile() == null) continue;
                    String name = data.getProfile().getName();
                    if (name == null) continue;

                    if (STAFF_LIST.contains(name.toLowerCase())) {
                        alert(NotificationType.ERROR, "Staff Online!", name + " joined the server",
                            "&c&l[STAFF] &r&f" + name + " &ajoined the server!");
                        triggerAutoLeave(name);
                    }
                }

            } else if (pkt.getAction() == S38PacketPlayerListItem.Action.REMOVE_PLAYER && mc.theWorld != null) {
                for (S38PacketPlayerListItem.AddPlayerData data : pkt.getEntries()) {
                    if (data == null || data.getProfile() == null) continue;
                    net.minecraft.entity.player.EntityPlayer entity =
                        mc.theWorld.getPlayerEntityByUUID(data.getProfile().getId());
                    if (entity == null || entity == mc.thePlayer) continue;
                    String name = entity.getGameProfile().getName();

                    if (STAFF_LIST.contains(name.toLowerCase())) {
                        alert(NotificationType.SUCCESS, "Staff Left", name + " left the server",
                            "&a[STAFF] &f" + name + " &cleft the server.");
                    }
                }
            }
            return;
        }

        if (packet instanceof S0CPacketSpawnPlayer) {
            validEntities.add(((S0CPacketSpawnPlayer) packet).getEntityID());
            return;
        }

        if (packet instanceof S13PacketDestroyEntities) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                validEntities.remove(id);
                flaggedGhost.remove(id);
            }
            return;
        }

        // Protocol Exploit Layer 1: Unregistered Entity Tracking (Vanish Detection)
        if (vanishDetect.getValue()) {
            if (packet instanceof S14PacketEntity) {
                int entityId = ((IAccessorS14PacketEntity) packet).getEntityId();
                checkVanish(entityId);
            } else if (packet instanceof S18PacketEntityTeleport) {
                int entityId = ((S18PacketEntityTeleport) packet).getEntityId();
                checkVanish(entityId);
            }
        }

        // Protocol Exploit Layer 2: Team Packet Leak Scanner
        if (teamScanner.getValue() && packet instanceof S3EPacketTeams) {
            S3EPacketTeams teamsPacket = (S3EPacketTeams) packet;
            checkTeamLeak(teamsPacket);
        }
    }

    private void checkVanish(int entityId) {
        if (mc.theWorld == null) return;
        if (mc.thePlayer != null && entityId == mc.thePlayer.getEntityId()) return;
        if (validEntities.contains(entityId) || flaggedGhost.contains(entityId)) return;

        // Check if the entity exists in the local world tracker
        if (mc.theWorld.getEntityByID(entityId) == null) {
            flaggedGhost.add(entityId);
            
            // Build and publish notification
            Myau.notificationManager.builder(NotificationType.WARN)
                .title("Staff Detector")
                .description("Vanish Staff Movement Detected! ID: " + entityId)
                .duration(3000)
                .buildAndPublish();
                
            ChatUtil.display("&c&l[StaffDetector] &r&fVanish staff movement detected! &7(ID: " + entityId + ")");
            triggerAutoLeave("VanishStaff#" + entityId);
        }
    }

    private void checkTeamLeak(S3EPacketTeams teamsPacket) {
        myau.mixin.IAccessorS3EPacketTeams accessor = (myau.mixin.IAccessorS3EPacketTeams) teamsPacket;
        String teamName = accessor.getTeamName();
        Collection<String> players = accessor.getPlayers();

        if (teamName != null && !alertedStaffTeams.contains(teamName)) {
            String lowerTeam = teamName.toLowerCase();
            // Check team name hierarchy indicators used by admin groups
            if (lowerTeam.contains("admin") || lowerTeam.contains("mod") || 
                lowerTeam.contains("helper") || lowerTeam.contains("staff") || 
                lowerTeam.contains("owner") || lowerTeam.contains("dev") || 
                lowerTeam.contains("support") || lowerTeam.contains("system")) {
                
                alertedStaffTeams.add(teamName);
                
                Myau.notificationManager.builder(NotificationType.WARN)
                    .title("Staff Detector")
                    .description("Staff Team Leak Detected! Team: " + teamName)
                    .duration(3000)
                    .buildAndPublish();
                
                ChatUtil.display("&c&l[StaffDetector] &r&fStaff Team Leak Detected! Team: &b" + teamName);
            }
        }

        if (players != null) {
            for (String player : players) {
                if (player == null || alertedStaffPlayers.contains(player)) continue;
                
                String lowerPlayer = player.toLowerCase();
                boolean isStaff = STAFF_LIST.contains(lowerPlayer);
                
                // Scan against administrative names or system tags to catch un-nicked staff
                if (!isStaff) {
                    if (lowerPlayer.contains("admin") || lowerPlayer.contains("mod_") || 
                        lowerPlayer.contains("helper") || lowerPlayer.contains("staff") || 
                        lowerPlayer.contains("owner") || lowerPlayer.contains("dev_") || 
                        lowerPlayer.contains("support")) {
                        isStaff = true;
                    }
                }
                
                if (isStaff) {
                    alertedStaffPlayers.add(player);
                    
                    Myau.notificationManager.builder(NotificationType.WARN)
                        .title("Staff Detector")
                        .description("Vanish Staff Player Detected in Team: " + player)
                        .duration(3000)
                        .buildAndPublish();
                    
                    ChatUtil.display("&c&l[StaffDetector] &r&fStaff member detected via Team leak: &e" + player);
                    triggerAutoLeave(player);
                }
            }
        }
    }

    private void alert(NotificationType type, String title, String desc, String chatMsg) {
<<<<<<< HEAD
        Myau.notificationManager.builder(type).title(title).description(desc).duration(3000).buildAndPublish();
=======
        Myau.notificationManager.builder(type).title(title).description(desc).duration(2000).buildAndPublish();
>>>>>>> 746610b90671b5ee596a876af938a43584190552
        ChatUtil.display(chatMsg);
    }

    private void triggerAutoLeave(String name) {
        if (autoLeave.getValue() && mc.thePlayer != null) {
            ChatUtil.sendMessage("/hub");
            this.setEnabled(false);
        }
    }
}
