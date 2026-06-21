package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.module.modules.combat.Velocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class UniversoCraftVelocity extends VelocityMode {
    public UniversoCraftVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress || event.isCancelled()) return;

        if (event.getType() == myau.event.types.EventType.RECEIVE) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity wrapper = (S12PacketEntityVelocity) event.getPacket();

                if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                    event.setCancelled(true);
                    mc.thePlayer.motionY += 0.1 - Math.random() / 100f;
                }
            }

            if (event.getPacket() instanceof S27PacketExplosion) {
                event.setCancelled(true);
                mc.thePlayer.motionY += 0.1 - Math.random() / 100f;
            }
        }
    }
}
