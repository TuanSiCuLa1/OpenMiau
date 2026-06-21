package myau.util.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public class BackTrackUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Vec3 getCurrentPosition(Entity entity) {
        return new Vec3(entity.posX, entity.posY, entity.posZ);
    }

    public static Vec3 getPreviousPosition(Entity entity) {
        return new Vec3(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }

    public static void setPositionAndPrevious(Entity entity, Vec3 current) {
        setPositionAndPrevious(entity, current, current);
    }

    public static void setPositionAndPrevious(Entity entity, Vec3 current, Vec3 previous) {
        double dx = current.xCoord - entity.posX;
        double dy = current.yCoord - entity.posY;
        double dz = current.zCoord - entity.posZ;
        entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(dx, dy, dz));
        entity.posX = current.xCoord;
        entity.posY = current.yCoord;
        entity.posZ = current.zCoord;
        entity.prevPosX = previous.xCoord;
        entity.prevPosY = previous.yCoord;
        entity.prevPosZ = previous.zCoord;
    }

    public static double getDistanceToBox(AxisAlignedBB box) {
        if (mc.thePlayer == null || box == null) return 0.0D;
        double x = clamp(mc.thePlayer.posX, box.minX, box.maxX);
        double y = clamp(mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), box.minY, box.maxY);
        double z = clamp(mc.thePlayer.posZ, box.minZ, box.maxZ);
        double dx = mc.thePlayer.posX - x;
        double dy = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - y;
        double dz = mc.thePlayer.posZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double getDistanceToEntityBox(Entity entity) {
        return entity == null ? 0.0D : getDistanceToBox(entity.getEntityBoundingBox());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


