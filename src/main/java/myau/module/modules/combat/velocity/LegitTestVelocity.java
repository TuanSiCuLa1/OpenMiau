package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;

public class LegitTestVelocity extends VelocityMode {
    private boolean shouldJump = false;
    private int jumpCooldown = 0;

    public LegitTestVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.POST) {
            int hurtTime = mc.thePlayer.hurtTime;

            if (hurtTime >= 8) {
                if (jumpCooldown <= 0) {
                    shouldJump = true;
                    jumpCooldown = 2;
                }
            } else if (hurtTime <= 1) {
                shouldJump = false;
                jumpCooldown = 0;
            }

            if (shouldJump && mc.thePlayer.onGround && jumpCooldown <= 0) {
                mc.thePlayer.jump();
                shouldJump = false;
            }

            if (jumpCooldown > 0) {
                jumpCooldown--;
            }
        }
    }
}
