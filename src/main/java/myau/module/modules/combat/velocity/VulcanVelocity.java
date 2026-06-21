package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.module.modules.combat.Velocity;
import myau.util.network.PacketUtil;
import myau.mixin.IAccessorS12PacketEntityVelocity;
import myau.util.network.PacketUtil;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

public class VulcanVelocity extends VelocityMode {
    private boolean transaction = false;

    public VulcanVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress || event.isCancelled()) return;

        if (event.getType() == myau.event.types.EventType.RECEIVE) {
            double horizontal = parent.horizontal.getValue();
            double vertical = parent.vertical.getValue();

            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity wrapper = (S12PacketEntityVelocity) event.getPacket();

                if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (horizontal == 0 && vertical == 0) {
                        event.setCancelled(true);
                        return;
                    }

                    IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity) wrapper;
                    accessor.setMotionX((int) (wrapper.getMotionX() * horizontal / 100));
                    accessor.setMotionY((int) (wrapper.getMotionY() * vertical / 100));
                    accessor.setMotionZ((int) (wrapper.getMotionZ() * horizontal / 100));
                }
            }

            if (event.getPacket() instanceof S32PacketConfirmTransaction && mc.thePlayer.hurtTime == 10) {
                event.setCancelled(true);
                PacketUtil.sendPacket(new C0FPacketConfirmTransaction((short) (transaction ? 1 : -1), (short) (transaction ? -1 : 1), transaction));
                transaction = !transaction;
            }
        }
    }
}
