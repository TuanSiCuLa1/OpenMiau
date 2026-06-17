package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.mixin.IAccessorS14PacketEntity;
import myau.util.ChatUtil;
import myau.util.notification.NotificationManager;
import myau.util.notification.NotificationType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;

import java.util.*;

public class StaffDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty ghostDetect = new BooleanProperty("ghost-detect", true);
    public final BooleanProperty autoLeave   = new BooleanProperty("auto-leave",   false);

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
    private final Set<Integer> flaggedGhost  = new HashSet<>();

    public StaffDetector() {
        super("StaffDetector", false, false);
    }

    @Override
    public void onEnabled() {
        validEntities.clear();
        flaggedGhost.clear();
    }

    @Override
    public void onDisabled() {
        validEntities.clear();
        flaggedGhost.clear();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        validEntities.clear();
        flaggedGhost.clear();
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

        if (ghostDetect.getValue()) {
            if (packet instanceof S14PacketEntity) {
                checkGhost(((IAccessorS14PacketEntity) packet).getEntityId());
            } else if (packet instanceof S18PacketEntityTeleport) {
                checkGhost(((S18PacketEntityTeleport) packet).getEntityId());
            }
        }
    }

    private void checkGhost(int entityId) {
        if (mc.thePlayer != null && entityId == mc.thePlayer.getEntityId()) return;
        if (validEntities.contains(entityId) || flaggedGhost.contains(entityId)) return;

        flaggedGhost.add(entityId);
        alert(NotificationType.ERROR, "Ghost Entity!", "Vanish detected (ID: " + entityId + ")",
            "&c&l[GHOST] &r&fVanish entity detected &7(ID: " + entityId + ")");
        triggerAutoLeave("Ghost#" + entityId);
    }

    private void alert(NotificationType type, String title, String desc, String chatMsg) {
        NotificationManager.show(title, desc, type);
        ChatUtil.display(chatMsg);
    }

    private void triggerAutoLeave(String name) {
        if (autoLeave.getValue() && mc.thePlayer != null) {
            ChatUtil.sendMessage("/hub");
            this.setEnabled(false);
        }
    }
}
