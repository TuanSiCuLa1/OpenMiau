package myau.module.modules.combat.velocity;

import myau.Myau;
import myau.enums.DelayModules;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.module.modules.movement.LongJump;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import myau.util.player.MoveUtil;

public class OMDelayVelocity extends VelocityMode {
    private boolean delayActive = false;
    private boolean reverseFlag = false;

    public OMDelayVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == myau.event.types.EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
                    if (!reverseFlag && !parent.canDelay() && !parent.isInLiquidOrWeb() && (!longJump.isEnabled() || !longJump.canStartJump())) {
                        parent.delayChanceCounter = parent.delayChanceCounter % 100 + parent.delayChance.getValue();
                        if (parent.delayChanceCounter >= 100) {
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            reverseFlag = true;
                            delayActive = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.POST) {
            if (reverseFlag && (parent.canDelay() || parent.isInLiquidOrWeb() || Myau.delayManager.getDelay() >= (long) parent.delayTicks.getValue())) {
                Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                reverseFlag = false;
            }
            if (delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                delayActive = false;
            }
        }
    }
}
