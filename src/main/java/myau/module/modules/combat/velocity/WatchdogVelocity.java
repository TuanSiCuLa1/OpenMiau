package myau.module.modules.combat.velocity;

import myau.events.JumpEvent;
import myau.events.PacketEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.mixin.IAccessorEntityLivingBase;
import myau.util.player.MoveUtil;
import myau.util.network.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;

public class WatchdogVelocity extends VelocityMode {
    private boolean active, receiving;
    private int offGroundTicks;
    private final ArrayList<Packet<?>> packets = new ArrayList<>();
    private int amount = 0;

    public WatchdogVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (receiving) return;

        if (event.getType() == myau.event.types.EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity velocity = (S12PacketEntityVelocity) packet;
                if (velocity.getEntityID() == mc.thePlayer.getEntityId() && !event.isCancelled()) {
                    // Simplified logic from WatchdogVelocity
                    if (amount < 1 && !mc.thePlayer.onGround && Math.random() < 0.73 && offGroundTicks <= 13) {
                        amount++;
                        event.setCancelled(true);
                    } else if (!mc.thePlayer.onGround || !MoveUtil.isMoving()) {
                        amount = 0;
                        active = true;
                        packets.add(velocity);
                        event.setCancelled(true);
                    } else {
                        mc.thePlayer.motionY = velocity.getMotionY() / 8000.0D;
                        event.setCancelled(true);
                    }
                }
            } else if (packet instanceof S32PacketConfirmTransaction) {
                if (active) {
                    packets.add(packet);
                    event.setCancelled(true);
                }
            } else if (packet instanceof S00PacketKeepAlive) {
                if (active) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (mc.thePlayer.onGround) {
                offGroundTicks = 0;
            } else {
                offGroundTicks++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onJump(JumpEvent event) {
        if (mc.thePlayer.onGround && active) {
            active = false;
            receiving = true;
            double mX = mc.thePlayer.motionX;
            double mZ = mc.thePlayer.motionZ;
            packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
            packets.clear();
            mc.thePlayer.motionX = mX;
            mc.thePlayer.motionZ = mZ;
            receiving = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStrafe(StrafeEvent event) {
        if (mc.thePlayer.onGround && active && !((IAccessorEntityLivingBase) mc.thePlayer).getIsJumping()) {
            active = false;
            receiving = true;
            double mX = mc.thePlayer.motionX;
            double mZ = mc.thePlayer.motionZ;
            packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
            packets.clear();
            mc.thePlayer.jump();
            mc.thePlayer.motionX = mX;
            mc.thePlayer.motionZ = mZ;
            receiving = false;
        } else if (offGroundTicks > 12 && active) {
            active = false;
            receiving = true;
            double mX = mc.thePlayer.motionX;
            double mZ = mc.thePlayer.motionZ;
            packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
            packets.clear();
            mc.thePlayer.motionX = mX;
            mc.thePlayer.motionZ = mZ;
            receiving = false;
        }
    }
}
