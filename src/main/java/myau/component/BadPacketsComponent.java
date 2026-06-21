package myau.component;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;

public final class BadPacketsComponent {

    private static boolean slot, attack, swing, block, inventory;

    public static boolean bad() {
        return bad(true, true, true, true, true);
    }

    public static boolean bad(final boolean slotCheck, final boolean attackCheck, final boolean swingCheck, final boolean blockCheck, final boolean inventoryCheck) {
        return (slot && slotCheck) ||
                (attack && attackCheck) ||
                (swing && swingCheck) ||
                (block && blockCheck) ||
                (inventory && inventoryCheck);
    }

    @EventTarget(Priority.HIGHEST)
    public final void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            final Packet<?> packet = event.getPacket();

            if (packet instanceof C09PacketHeldItemChange) {
                slot = true;
            } else if (packet instanceof C0APacketAnimation) {
                swing = true;
            } else if (packet instanceof C02PacketUseEntity) {
                attack = true;
            } else if (packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging) {
                block = true;
            } else if (packet instanceof C0EPacketClickWindow ||
                    (packet instanceof C16PacketClientStatus && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) ||
                    packet instanceof C0DPacketCloseWindow) {
                inventory = true;
            } else if (packet instanceof C03PacketPlayer) {
                reset();
            }
        }
    }

    public static void reset() {
        slot = false;
        swing = false;
        attack = false;
        block = false;
        inventory = false;
    }
}
