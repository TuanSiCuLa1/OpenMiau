package myau.module.modules.combat.velocity;

import myau.events.AttackEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;

public class IntaveVelocity extends VelocityMode {
    private boolean attacked;
    private boolean slowDown;

    public IntaveVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) return;

            if (attacked && !slowDown && mc.thePlayer.isSprinting()) {
                mc.thePlayer.motionX *= 0.6D;
                mc.thePlayer.motionZ *= 0.6D;
                mc.thePlayer.setSprinting(false);
            }

            attacked = false;
            slowDown = false;
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        attacked = true;
    }
}
