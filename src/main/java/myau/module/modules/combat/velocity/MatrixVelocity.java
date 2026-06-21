package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;

public class MatrixVelocity extends VelocityMode {
    public MatrixVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;

            if (mc.thePlayer.hurtTime > 0) {
                mc.thePlayer.motionX *= 0.6D;
                mc.thePlayer.motionZ *= 0.6D;
            }
        }
    }
}
