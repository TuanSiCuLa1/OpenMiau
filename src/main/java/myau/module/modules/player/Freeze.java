package myau.module.modules.player;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.StrafeEvent;
import myau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Freeze extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public Freeze() {
        super("Freeze", false);
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        mc.thePlayer.motionX = 0.0D;
        mc.thePlayer.motionY = 0.0D;
        mc.thePlayer.motionZ = 0.0D;
        mc.thePlayer.movementInput.moveForward = 0.0F;
        mc.thePlayer.movementInput.moveStrafe = 0.0F;
        mc.thePlayer.movementInput.jump = false;
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        event.setForward(0.0F);
        event.setStrafe(0.0F);
        event.setFriction(0.0F);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof C03PacketPlayer
                && !(event.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            event.setCancelled(true);
        }
    }
}
