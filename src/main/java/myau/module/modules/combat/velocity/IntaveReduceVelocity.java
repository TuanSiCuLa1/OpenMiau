package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class IntaveReduceVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private int intaveTick = 0;
    private int intaveDamageTick = 0;

    public IntaveReduceVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == myau.event.types.EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                hasReceivedVelocity = true;
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.POST) {
            if (hasReceivedVelocity) {
                intaveTick++;
                if (mc.thePlayer.hurtTime == 2) {
                    intaveDamageTick++;
                    if (mc.thePlayer.onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                        mc.thePlayer.jump();
                        intaveTick = 0;
                    }
                    hasReceivedVelocity = false;
                }
            }
        }
    }
}
