package myau.module.modules.combat.velocity;

import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.modules.combat.Velocity;
import myau.util.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.AxisAlignedBB;

import java.util.Comparator;

public class GrimReduceVelocity extends VelocityMode {
    public GrimReduceVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == myau.event.types.EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                Entity target = getClosestEntity();
                if (target != null) {
                    PacketUtil.sendPacketNoEvent(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == myau.event.types.EventType.PRE) {
            if (mc.thePlayer.ticksExisted <= 20) return;

            if (mc.thePlayer.hurtTime > 0) {
                if (isNearBlock()) {
                    mc.thePlayer.motionX *= 0.02;
                    mc.thePlayer.motionZ *= 0.02;
                }
                
                if (mc.thePlayer.hurtTime == 9) {
                    if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                        PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    }
                }
            }
        }
    }

    private Entity getClosestEntity() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase && e != mc.thePlayer && mc.thePlayer.getDistanceToEntity(e) <= 6.0f)
                .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                .orElse(null);
    }

    private boolean isNearBlock() {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().expand(0.5, 0.0, 0.5);
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty();
    }
}
