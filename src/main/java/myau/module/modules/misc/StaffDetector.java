package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.ChatUtil;
import myau.util.notification.NotificationManager;
import myau.util.notification.NotificationType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import myau.mixin.IAccessorS14PacketEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaffDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    public final BooleanProperty autoLeave = new BooleanProperty("auto-leave", true);

    private final List<String> staff = Arrays.asList("sennenkoi", "ongtrum2k10", "cheesetheslave");

    private final Set<Integer> validEntities = new HashSet<>();
    private final Set<Integer> flaggedStaff = new HashSet<>();

    public StaffDetector() {
        super("StaffDetector", false, false);
    }

    @Override
    public void onEnabled() {
        validEntities.clear();
        flaggedStaff.clear();
    }

    @Override
    public void onDisabled() {
        validEntities.clear();
        flaggedStaff.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem item = (S38PacketPlayerListItem) packet;
            if (item.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData player : item.getEntries()) {
                    if (player != null && player.getProfile() != null && player.getProfile().getName() != null) {
                        if (staff.contains(player.getProfile().getName().toLowerCase())) {
                            ChatUtil.sendFormatted("&c[WARNING] &fStaff " + player.getProfile().getName() + " is online!");
                            triggerAutoLeave(player.getProfile().getName());
                        }
                    }
                }
            }
        } else if (packet instanceof S3EPacketTeams) {
            S3EPacketTeams teams = (S3EPacketTeams) packet;
            if (teams.getPlayers() != null) {
                for (String name : teams.getPlayers()) {
                    if (name != null && staff.contains(name.toLowerCase())) {
                        ChatUtil.sendFormatted("&c[WARNING] &fStaff " + name + " detected!");
                        triggerAutoLeave(name);
                    }
                }
            }
        }

        if (packet instanceof S0CPacketSpawnPlayer) {
            validEntities.add(((S0CPacketSpawnPlayer) packet).getEntityID());
        } 
        else if (packet instanceof S13PacketDestroyEntities) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                validEntities.remove(id);
                flaggedStaff.remove(id);
            }
        } 
        else if (packet instanceof S14PacketEntity) {
            checkGhostEntity(((IAccessorS14PacketEntity) packet).getEntityId());
        } else if (packet instanceof S18PacketEntityTeleport) {
            checkGhostEntity(((S18PacketEntityTeleport) packet).getEntityId());
        }
    }

    private void checkGhostEntity(int entityId) {
        if (mc.thePlayer != null && entityId == mc.thePlayer.getEntityId()) return;

        if (!validEntities.contains(entityId) && !flaggedStaff.contains(entityId)) {
            flaggedStaff.add(entityId);
            ChatUtil.sendFormatted("&c[EXPLOIT] &fGhost/Vanished Entity Detected! (ID: " + entityId + ")");
            triggerAutoLeave("Ghost (ID " + entityId + ")");
        }
    }

    private void triggerAutoLeave(String name) {
        NotificationManager.show("Staff Detector", "Staff " + name + " detected!", NotificationType.WARNING);
        
        if (autoLeave.getValue()) {
            if (mc.thePlayer != null) {
                ChatUtil.sendMessage("/hub");
            }
            this.setEnabled(false); 
        }
    }
}
