package myau.module.modules.combat.velocity;

import myau.module.modules.combat.Velocity;

public class KarhuVelocity extends VelocityMode {
    public KarhuVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    // Karhu Velocity in Rise relies on BlockAABBEvent, which OpenMiau does not have.
    // So this is currently a stub unless OpenMiau implements a BlockAABB hook.
}
