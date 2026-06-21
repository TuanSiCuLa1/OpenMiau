package myau.module.modules.combat.velocity;

import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.math.RandomUtil;

public class JumpResetVelocity extends VelocityMode {
    public JumpResetVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.POST) {
            if (mc.thePlayer.onGround && mc.thePlayer.hurtTime >= 9 && !parent.isInLiquidOrWeb() && RandomUtil.nextInt(1, 100) <= parent.chance.getValue()) {
                mc.thePlayer.jump();
            }
        }
    }
}
