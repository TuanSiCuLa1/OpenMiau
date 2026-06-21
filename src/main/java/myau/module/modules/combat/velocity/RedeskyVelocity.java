package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.module.modules.combat.Velocity;
import myau.util.network.PacketUtil;
import myau.mixin.IAccessorS12PacketEntityVelocity;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class RedeskyVelocity extends VelocityMode {
    public RedeskyVelocity(String name, Velocity parent) {
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

                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }
            }

            if (event.getPacket() instanceof S27PacketExplosion) {
                if (horizontal == 0 && vertical == 0) {
                    event.setCancelled(true);
                    return;
                }
                // No setter for explosion in OpenMiau, ignore or cancel.
            }
        }
    }
}
