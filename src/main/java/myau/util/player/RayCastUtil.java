package myau.util.player;

import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;

public class RayCastUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range) {
        return rayCast(yaw, pitch, range, 0);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand) {
        return rayCast(yaw, pitch, range, expand, mc.thePlayer);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand, Entity entity) {
        final float partialTicks = ((myau.mixin.IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            Vec3 eyePos = entity.getPositionEyes(partialTicks);
            Vec3 lookVec = getVectorForRotation(pitch, yaw);
            Vec3 targetPos = eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
            
            objectMouseOver = mc.theWorld.rayTraceBlocks(eyePos, targetPos, false, false, true);

            double d1 = range;
            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(eyePos);
            }

            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(
                    entity, 
                    entity.getEntityBoundingBox().addCoord(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range).expand(f, f, f), 
                    Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith)
            );
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + expand;
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(eyePos, targetPos);

                if (axisalignedbb.isVecInside(eyePos)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? eyePos : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = eyePos.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        }

        return null;
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }
}
