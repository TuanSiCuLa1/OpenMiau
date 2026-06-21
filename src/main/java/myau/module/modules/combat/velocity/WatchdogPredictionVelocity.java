package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.network.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;

public class WatchdogPredictionVelocity extends VelocityMode {
    private boolean active, receiving;
    private int offGroundTicks;
    private final ArrayList<Packet<?>> packets = new ArrayList<>();
    private float desiredYaw;
    private double velX, velZ;

    public WatchdogPredictionVelocity(String name, Velocity parent) {
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
                    active = true;
                    double vX = velocity.getMotionX() / 8000.0D;
                    double vZ = velocity.getMotionZ() / 8000.0D;
                    desiredYaw = (float) Math.toDegrees(Math.atan2(vZ, vX));

                    if (desiredYaw < -180) desiredYaw += 360;
                    if (desiredYaw > 180) desiredYaw -= 360;

                    packets.add(velocity);
                    event.setCancelled(true);

                    this.velX = vX;
                    this.velZ = vZ;
                }
            } else if (packet instanceof S32PacketConfirmTransaction) {
                if (active) {
                    packets.add(packet);
                    event.setCancelled(true);
                }
            } else if (packet instanceof S00PacketKeepAlive) {
                if (active) {
                    if (mc.thePlayer.ticksExisted % 3 == 0) {
                        packets.add(packet);
                    }
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

            if (active && mc.thePlayer.onGround || receiving) {
                double cancelX = -this.velX;
                double cancelZ = -this.velZ;
                mc.thePlayer.motionX = cancelX;
                mc.thePlayer.motionZ = cancelZ;
            } else if (mc.thePlayer.onGround && mc.thePlayer.hurtTime > 7) {
                double cancelX = -this.velX;
                double cancelZ = -this.velZ;
                mc.thePlayer.motionX *= cancelX;
                mc.thePlayer.motionZ *= cancelZ;
                mc.thePlayer.jump();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStrafe(StrafeEvent event) {
        if (active) {
            float playerYaw = mc.thePlayer.rotationYaw % 360F;
            if (playerYaw < -180F) playerYaw += 360F;
            if (playerYaw > 180F) playerYaw -= 360F;

            float yawDifference = Math.abs(playerYaw - desiredYaw);
            float leeway = 20F;

            if (yawDifference <= leeway || yawDifference >= (360F - leeway)) {
                receiving = true;
                active = false;
                packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
                packets.clear();
                receiving = false;
                offGroundTicks = 0;
            } else if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= -1D;
                mc.thePlayer.motionZ *= -1D;
                receiving = true;
                active = false;
                packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
                packets.clear();
                receiving = false;
            } else if (offGroundTicks > 12) {
                receiving = true;
                active = false;
                packets.forEach(p -> PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p));
                packets.clear();
                mc.thePlayer.jump();
                mc.thePlayer.motionX *= 0.6D;
                mc.thePlayer.motionZ *= 0.6D;
                receiving = false;
            }
        }
    }
}
