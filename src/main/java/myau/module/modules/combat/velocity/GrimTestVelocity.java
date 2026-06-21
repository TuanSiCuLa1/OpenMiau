package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;

public class GrimTestVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private int jumpCount = 0;

    public GrimTestVelocity(String name, Velocity parent) {
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
                if (mc.thePlayer.onGround && mc.thePlayer.hurtTime >= 8 && mc.thePlayer.isSprinting() && mc.currentScreen == null && !mc.thePlayer.isPotionActive(Potion.jump) && !parent.isInLiquidOrWeb()) {
                    if (jumpCount >= parent.grimReduceJumpLimit.getValue()) {
                        jumpCount = 0;
                        hasReceivedVelocity = false;
                    } else {
                        jumpCount++;
                        mc.thePlayer.movementInput.jump = true;
                    }
                } else if (mc.thePlayer.hurtTime <= 1) {
                    hasReceivedVelocity = false;
                    jumpCount = 0;
                }
            }
        }
    }
}
