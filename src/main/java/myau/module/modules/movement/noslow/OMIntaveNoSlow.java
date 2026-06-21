package myau.module.modules.movement.noslow;

import myau.component.BadPacketsComponent;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.movement.NoSlow;
import myau.util.network.PacketUtil;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class OMIntaveNoSlow extends NoSlowMode {
    public OMIntaveNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.getParent().isAnyActive()) {
            float multiplier = this.getParent().getMotionMultiplier();
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.getParent().canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (this.getParent().isSwordActive() && !BadPacketsComponent.bad(false, true, true, false, false)) {
                int currentSlot = mc.thePlayer.inventory.currentItem;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            }
        }
    }
}
