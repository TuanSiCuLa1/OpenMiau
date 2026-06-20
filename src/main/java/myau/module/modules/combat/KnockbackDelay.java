package myau.module.modules.combat;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.Iterator;
import java.util.LinkedList;

public class KnockbackDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty distanceToTarget = new FloatProperty("distance-to-target", 6.0F, 3.0F, 12.0F);
    public final IntProperty chance = new IntProperty("chance", 100, 0, 100);
    public final IntProperty maximumDelay = new IntProperty("maximum-delay", 200, 50, 1000);
    public final BooleanProperty inAir = new BooleanProperty("in-air", true);
    public final BooleanProperty lookingAtPlayer = new BooleanProperty("looking-at-player", false);
    public final BooleanProperty requireLeftMouse = new BooleanProperty("require-left-mouse", false);

    private final LinkedList<DelayedPacket> delayedPackets = new LinkedList<>();
    private boolean delaying;

    public KnockbackDelay() {
        super("KnockbackDelay", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null || mc.theWorld == null) return;
        Packet<?> packet = event.getPacket();
        if (event.getType() != EventType.RECEIVE) return;

        if (packet instanceof S08PacketPlayerPosLook) {
            this.flushPackets();
            return;
        }
        if (!(packet instanceof S12PacketEntityVelocity)) return;

        S12PacketEntityVelocity velocity = (S12PacketEntityVelocity) packet;
        if (velocity.getEntityID() != mc.thePlayer.getEntityId()) return;
        if (!this.conditionsMet()) return;
        if (this.chance.getValue() < 100 && Math.random() * 100.0 >= this.chance.getValue()) return;

        event.setCancelled(true);
        this.delaying = true;
        this.delayedPackets.add(new DelayedPacket(this.castToClientPacket(packet), System.currentTimeMillis()));
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead || !this.conditionsMet()) {
            this.flushPackets();
            return;
        }
        this.releaseExpiredPackets();
    }

    private boolean conditionsMet() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (this.inAir.getValue() && mc.thePlayer.onGround) return false;
        if (this.requireLeftMouse.getValue() && !Mouse.isButtonDown(0)) return false;
        if (this.findTarget(this.distanceToTarget.getValue()) == null) return false;
        if (this.lookingAtPlayer.getValue()) {
            MovingObjectPosition mop = mc.objectMouseOver;
            return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit instanceof EntityPlayer;
        }
        return true;
    }

    private EntityPlayer findTarget(float range) {
        EntityPlayer best = null;
        double bestDist = range * range;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || player.getHealth() <= 0.0F) continue;
            double dist = mc.thePlayer.getDistanceSqToEntity(player);
            if (dist <= bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private Packet<INetHandlerPlayClient> castToClientPacket(Packet<?> packet) {
        return (Packet<INetHandlerPlayClient>) packet;
    }

    private void releaseExpiredPackets() {
        long now = System.currentTimeMillis();
        Iterator<DelayedPacket> iterator = this.delayedPackets.iterator();
        while (iterator.hasNext()) {
            DelayedPacket data = iterator.next();
            if (now - data.time >= this.maximumDelay.getValue()) {
                PacketUtil.handlePacket(data.packet);
                iterator.remove();
            }
        }
        this.delaying = !this.delayedPackets.isEmpty();
    }

    private void flushPackets() {
        for (DelayedPacket data : this.delayedPackets) {
            PacketUtil.handlePacket(data.packet);
        }
        this.delayedPackets.clear();
        this.delaying = false;
    }

    @Override
    public void onDisabled() {
        this.flushPackets();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.maximumDelay.getValue() + "ms" + (this.delaying ? " " + this.delayedPackets.size() : "")};
    }

    private static class DelayedPacket {
        private final Packet<INetHandlerPlayClient> packet;
        private final long time;

        private DelayedPacket(Packet<INetHandlerPlayClient> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
