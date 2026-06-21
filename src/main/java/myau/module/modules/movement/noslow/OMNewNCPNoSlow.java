package myau.module.modules.movement.noslow;

import myau.Myau;
import myau.component.BadPacketsComponent;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.KillAura;
import myau.module.modules.movement.NoSlow;
import myau.util.network.PacketUtil;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class OMNewNCPNoSlow extends NoSlowMode {
    private int disable;

    public OMNewNCPNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            this.disable++;
            if (this.getParent().isAnyActive()) {
                this.performBypass();
            }
        }
        
        KillAura aura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (aura != null && aura.getTarget() != null) {
            return;
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

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.disable = 0;
        }
    }

    private void performBypass() {
        KillAura aura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (this.disable > 10 && !BadPacketsComponent.bad(false, true, true, false, false) && (aura == null || aura.getTarget() == null)) {
            int currentSlot = mc.thePlayer.inventory.currentItem;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
    }
}
