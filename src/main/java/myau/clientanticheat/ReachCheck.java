package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ReachCheck {
    private static final double BASE_REACH = 3.05D;
    private final Map<String, CheckBuffer> reachBuffers = new HashMap<>();

    public void check(EntityPlayer player, World world, PlayerCheckData data, ClientAntiCheatContext context) {
        if (data == null || data.recentlyTeleported() || !data.startedSwinging()) return;
        String name = player.getName();
        if (name == null) return;

        double nearest = Double.MAX_VALUE;
        Vec3 eyes = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        for (EntityPlayer target : world.playerEntities) {
            if (target == player || target.isDead || target.getName() == null) continue;
            AxisAlignedBB box = target.getEntityBoundingBox().expand(0.12D, 0.1D, 0.12D);
            double distance = distanceToBox(eyes, box);
            if (distance < nearest) nearest = distance;
        }
        if (nearest == Double.MAX_VALUE || nearest > 6.0D) return;

        CheckBuffer buffer = this.reachBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        double movementTolerance = Math.min(0.35D, data.horizontalDelta + data.lastHorizontalDelta);
        double allowed = BASE_REACH + movementTolerance + (data.recentlyHurt() ? 0.15D : 0.0D);
        if (nearest > allowed) {
            double over = nearest - allowed;
            if (buffer.flag(1.0D + Math.min(2.0D, over * 2.0D), 4.0D)) {
                context.receiveSignal(name, "Reach");
                buffer.reset();
            }
        } else {
            buffer.decay(0.45D);
        }
    }

    private double distanceToBox(Vec3 point, AxisAlignedBB box) {
        double x = clamp(point.xCoord, box.minX, box.maxX);
        double y = clamp(point.yCoord, box.minY, box.maxY);
        double z = clamp(point.zCoord, box.minZ, box.maxZ);
        double dx = point.xCoord - x;
        double dy = point.yCoord - y;
        double dz = point.zCoord - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void reset() {
        this.reachBuffers.clear();
    }
}
