package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.player.MoveUtil;

public class TickVelocity extends VelocityMode {
    public TickVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;

            if (mc.thePlayer.hurtTime == 10 - 1) { // Default tickVelocity = 1
                MoveUtil.setSpeed(0, MoveUtil.getMoveYaw()); // Equivalent to MoveUtil.stop()
            }
        }
    }
}
