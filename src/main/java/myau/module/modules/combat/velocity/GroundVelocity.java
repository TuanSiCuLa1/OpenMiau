package myau.module.modules.combat.velocity;

import myau.events.MoveInputEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;

public class GroundVelocity extends VelocityMode {
    public GroundVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;
            // Delay is typically 1 in Rise GroundVelocity
            if (mc.thePlayer.hurtTime == 10 - 1) { // roughly ticksSinceVelocity
                mc.thePlayer.onGround = true;
            }
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (mc.thePlayer.hurtTime == 10 - 2) {
            mc.thePlayer.movementInput.jump = false;
        }
    }
}
