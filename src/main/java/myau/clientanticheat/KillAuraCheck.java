package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class KillAuraCheck {
    private final Map<String, Long> lastAttackTicks = new HashMap<>();
    private final Map<String, CheckBuffer> rateBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> aimBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> snapBuffers = new HashMap<>();

    public void check(EntityPlayer player, World world, PlayerCheckData data, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        if (name == null || data == null || data.recentlyTeleported()) return;
        if (!data.startedSwinging()) {
            this.decay(name);
            return;
        }

        EntityPlayer target = this.nearestTarget(player, world, 6.0D);
        if (target == null) return;

        CheckBuffer rateBuffer = this.rateBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer aimBuffer = this.aimBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer snapBuffer = this.snapBuffers.computeIfAbsent(name, key -> new CheckBuffer());

        long lastAttack = this.lastAttackTicks.getOrDefault(name, currentTick - 20L);
        long delay = currentTick - lastAttack;
        this.lastAttackTicks.put(name, currentTick);
        if (delay > 0L && delay < 3L) {
            rateBuffer.flag(1.0D, 999.0D);
        } else {
            rateBuffer.decay(0.35D);
        }

        float yawError = Math.abs(MathHelper.wrapAngleTo180_float(this.yawTo(player, target) - player.rotationYaw));
        float pitchError = Math.abs(this.pitchTo(player, target) - player.rotationPitch);
        if (yawError > 35.0F || pitchError > 28.0F) {
            aimBuffer.flag(1.25D, 999.0D);
        } else {
            aimBuffer.decay(0.45D);
        }

        if ((data.yawDelta > 95.0F || data.yawAcceleration > 65.0F) && yawError < 8.0F) {
            snapBuffer.flag(1.0D, 999.0D);
        } else {
            snapBuffer.decay(0.3D);
        }

        if (rateBuffer.get() > 4.0D && aimBuffer.get() > 2.0D || snapBuffer.get() > 3.0D && rateBuffer.get() > 2.0D) {
            context.receiveSignal(name, "KillAura");
            rateBuffer.reset();
            aimBuffer.reset();
            snapBuffer.reset();
        }
    }

    private void decay(String name) {
        CheckBuffer rate = this.rateBuffers.get(name);
        CheckBuffer aim = this.aimBuffers.get(name);
        CheckBuffer snap = this.snapBuffers.get(name);
        if (rate != null) rate.decay(0.15D);
        if (aim != null) aim.decay(0.15D);
        if (snap != null) snap.decay(0.15D);
    }

    private EntityPlayer nearestTarget(EntityPlayer player, World world, double maxDistance) {
        EntityPlayer nearest = null;
        double best = maxDistance * maxDistance;
        for (EntityPlayer target : world.playerEntities) {
            if (target == player || target.isDead || target.getName() == null) continue;
            double distance = player.getDistanceSqToEntity(target);
            if (distance < best) {
                best = distance;
                nearest = target;
            }
        }
        return nearest;
    }

    private float yawTo(EntityPlayer player, EntityPlayer target) {
        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        return (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
    }

    private float pitchTo(EntityPlayer player, EntityPlayer target) {
        Vec3 eyes = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 targetEyes = new Vec3(target.posX, target.posY + target.getEyeHeight(), target.posZ);
        double dx = targetEyes.xCoord - eyes.xCoord;
        double dy = targetEyes.yCoord - eyes.yCoord;
        double dz = targetEyes.zCoord - eyes.zCoord;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -(Math.atan2(dy, horizontal) * 180.0D / Math.PI);
    }

    public void reset() {
        this.lastAttackTicks.clear();
        this.rateBuffers.clear();
        this.aimBuffers.clear();
        this.snapBuffers.clear();
    }
}
