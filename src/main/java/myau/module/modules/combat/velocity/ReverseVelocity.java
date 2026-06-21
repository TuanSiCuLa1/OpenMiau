package myau.module.modules.combat.velocity;

import myau.events.KnockbackEvent;
import myau.module.modules.combat.Velocity;

public class ReverseVelocity extends VelocityMode {
    public ReverseVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onKnockback(KnockbackEvent event) {
        if (!event.isCancelled()) {
            event.setX(event.getX() * -0.5); // Reverse knockback
            event.setZ(event.getZ() * -0.5);
        }
    }
}
