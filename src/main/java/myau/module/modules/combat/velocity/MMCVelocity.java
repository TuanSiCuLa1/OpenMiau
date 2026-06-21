package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.client.KeyBindUtil;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class MMCVelocity extends VelocityMode {
    private boolean velocity;

    public MMCVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (mc.thePlayer.hurtTime > 0) {
                mc.thePlayer.motionX *= 0.6D;
                mc.thePlayer.motionZ *= 0.6D;
            }
            this.velocity = true;
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (parent.onSwing.getValue() && !mc.thePlayer.isSwingInProgress || event.isCancelled()) return;
        if (event.getType() == myau.event.types.EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

            if (mc.thePlayer.isSprinting()) {
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            }
        }
    }
}
