package myau.module.modules.movement.noslow;

import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.modules.movement.NoSlow;
import myau.util.network.PacketUtil;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class OMNCPNoSlow extends NoSlowMode {
    public OMNCPNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.getParent().isSwordActive()) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        } else if (event.getType() == EventType.POST) {
            if (this.getParent().isSwordActive()) {
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
        }

        if (this.getParent().isAnyActive()) {
            float multiplier = this.getParent().getMotionMultiplier();
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.getParent().canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }
}
