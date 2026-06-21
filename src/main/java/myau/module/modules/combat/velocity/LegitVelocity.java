package myau.module.modules.combat.velocity;

import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.player.MoveUtil;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class LegitVelocity extends VelocityMode {
    private boolean jump;

    public LegitVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            jump = false;
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;

        if (jump && MoveUtil.isMoving() && Math.random() * 100 < parent.chance.getValue()) {
            mc.thePlayer.movementInput.jump = true;
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress || event.isCancelled()) return;

        if (!mc.thePlayer.onGround) {
            return;
        }

        if (event.getType() == myau.event.types.EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();

            // legitTiming is not ported as a property to keep it clean, assuming false for now
            if (packet.getEntityID() == mc.thePlayer.getEntityId() && packet.getMotionY() > 0) {
                jump = true;
            }
        }
    }
}
