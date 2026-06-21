package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.events.PacketEvent;
import myau.module.modules.combat.Velocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class LegitSmartVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private int legitSmartJumpCount = 0;

    public LegitSmartVelocity(String name, Velocity parent) {
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
                if (mc.thePlayer.onGround && mc.thePlayer.hurtTime >= 8 && mc.thePlayer.isSprinting() && !parent.isInLiquidOrWeb()) {
                    if (legitSmartJumpCount >= parent.legitSmartJumpLimit.getValue()) {
                        legitSmartJumpCount = 0;
                        hasReceivedVelocity = false;
                    } else {
                        legitSmartJumpCount++;
                        mc.thePlayer.movementInput.jump = true;
                    }
                } else if (mc.thePlayer.hurtTime <= 1) {
                    hasReceivedVelocity = false;
                    legitSmartJumpCount = 0;
                }
            }
        }
    }
}
