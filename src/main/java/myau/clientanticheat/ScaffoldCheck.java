package myau.clientanticheat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScaffoldCheck {
    private final Map<UUID, Integer> supportBuffer = new HashMap<>();
    private final Map<UUID, Integer> rotationBuffer = new HashMap<>();
    private final Map<UUID, Integer> pitchBuffer = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, Long> lastFlag = new HashMap<>();

    public void check(EntityPlayer player, World world, ClientAntiCheatContext context) {
        UUID uuid = player.getUniqueID();
        ItemStack held = player.getHeldItem();
        boolean holdingBlock = held != null && held.getItem() instanceof ItemBlock;
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        boolean movingFast = horizontalSpeed > 0.15D;
        boolean airborne = !player.onGround;
        boolean falling = player.motionY < -0.08D;
        boolean supportBelow = this.hasSupportBelow(player, world);

        int supportVl = this.supportBuffer.getOrDefault(uuid, 0);
        if (holdingBlock && airborne && falling && movingFast && supportBelow) {
            supportVl += 2;
        } else {
            supportVl = Math.max(0, supportVl - 1);
        }
        this.supportBuffer.put(uuid, supportVl);

        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - this.lastYaw.getOrDefault(uuid, yaw)));
        float pitchDiff = Math.abs(pitch - this.lastPitch.getOrDefault(uuid, pitch));
        this.lastYaw.put(uuid, yaw);
        this.lastPitch.put(uuid, pitch);

        int rotationVl = this.rotationBuffer.getOrDefault(uuid, 0);
        if (holdingBlock && movingFast && (yawDiff > 105.0F || pitchDiff > 32.0F)) {
            rotationVl += 2;
        } else {
            rotationVl = Math.max(0, rotationVl - 1);
        }
        this.rotationBuffer.put(uuid, rotationVl);

        int pitchVl = this.pitchBuffer.getOrDefault(uuid, 0);
        if (holdingBlock && movingFast && pitch > 65.0F && pitchDiff < 1.0F && supportBelow) {
            pitchVl++;
        } else {
            pitchVl = Math.max(0, pitchVl - 1);
        }
        this.pitchBuffer.put(uuid, pitchVl);

        if ((supportVl > 10 && rotationVl > 3) || (supportVl > 12 && pitchVl > 8)) {
            long now = System.currentTimeMillis();
            long last = this.lastFlag.getOrDefault(uuid, 0L);
            if (now - last > 3000L) {
                context.receiveSignal(player.getName(), "Scaffold");
                this.lastFlag.put(uuid, now);
                this.supportBuffer.put(uuid, 0);
                this.rotationBuffer.put(uuid, 0);
                this.pitchBuffer.put(uuid, 0);
            }
        }
    }

    private boolean hasSupportBelow(EntityPlayer player, World world) {
        for (double xOffset = -0.3D; xOffset <= 0.3D; xOffset += 0.3D) {
            for (double zOffset = -0.3D; zOffset <= 0.3D; zOffset += 0.3D) {
                BlockPos below = new BlockPos(
                        MathHelper.floor_double(player.posX + xOffset),
                        MathHelper.floor_double(player.posY - 1.0D),
                        MathHelper.floor_double(player.posZ + zOffset)
                );
                Block block = world.getBlockState(below).getBlock();
                if (!(block instanceof BlockAir)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void reset() {
        this.supportBuffer.clear();
        this.rotationBuffer.clear();
        this.pitchBuffer.clear();
        this.lastYaw.clear();
        this.lastPitch.clear();
        this.lastFlag.clear();
    }
}
