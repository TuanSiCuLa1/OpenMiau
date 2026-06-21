package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.module.modules.combat.Velocity;
import myau.mixin.IAccessorS12PacketEntityVelocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class BufferAbuseVelocity extends VelocityMode {
    private int amount = 0;

    public BufferAbuseVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;
        if (event.getType() == myau.event.types.EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (amount < 1) { // simplified buffer
                        event.setCancelled(true);
                        amount++;
                        return;
                    }

                    double horizontal = parent.horizontal.getValue();
                    double vertical = parent.vertical.getValue();
                    IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity) packet;
                    accessor.setMotionX((int) (packet.getMotionX() * horizontal / 100));
                    accessor.setMotionY((int) (packet.getMotionY() * vertical / 100));
                    accessor.setMotionZ((int) (packet.getMotionZ() * horizontal / 100));
                    amount = 0;
                }
            }
        }
    }
}
