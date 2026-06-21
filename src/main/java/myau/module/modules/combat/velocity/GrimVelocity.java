package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.network.PacketUtil;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class GrimVelocity extends VelocityMode {
    private boolean realVelocity;
    private boolean velocity;

    public GrimVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (velocity) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(
                        (mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK),
                        new BlockPos(mc.thePlayer), EnumFacing.UP));
                velocity = false;
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == myau.event.types.EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S19PacketEntityStatus) {
                S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                if (packet.getEntity(mc.theWorld) == mc.thePlayer && packet.getOpCode() == 2) {
                    realVelocity = true;
                }
            }
            if (event.getPacket() instanceof S12PacketEntityVelocity && realVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    event.setCancelled(true);
                    realVelocity = false;
                    velocity = true;
                }
            }
        }
    }
}
