package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.player.MoveUtil;

public class BounceVelocity extends VelocityMode {
    public BounceVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;

            if (mc.thePlayer.hurtTime == 9) { // simplified tick logic
                if (MoveUtil.isMoving()) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                } else {
                    mc.thePlayer.motionZ *= -1;
                    mc.thePlayer.motionX *= -1;
                }
            }
        }
    }
}
