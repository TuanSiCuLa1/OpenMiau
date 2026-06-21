package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ScaffoldCheck {
    private final Map<String, CheckBuffer> supportBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> rotationBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> pitchBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> edgeBuffers = new HashMap<>();
    private final Map<String, Long> lastFlag = new HashMap<>();

    public void check(EntityPlayer player, World world, PlayerCheckData data, ClientAntiCheatContext context) {
        String name = player.getName();
        if (name == null || data == null) return;

        CheckBuffer supportBuffer = this.supportBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer rotationBuffer = this.rotationBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer pitchBuffer = this.pitchBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer edgeBuffer = this.edgeBuffers.computeIfAbsent(name, key -> new CheckBuffer());

        ItemStack held = player.getHeldItem();
        boolean holdingBlock = held != null && held.getItem() instanceof ItemBlock;
        if (!holdingBlock || this.isExempt(player, data)) {
            supportBuffer.decay(0.5D);
            rotationBuffer.decay(0.5D);
            pitchBuffer.decay(0.5D);
            edgeBuffer.decay(0.5D);
            return;
        }

        boolean moving = data.horizontalDelta > 0.12D;
        boolean hasBlockBelow = this.hasSolidBelow(player, world, 1.0D);
        boolean hasRecentSupport = this.hasSolidBelow(player, world, 1.35D);
        boolean nearEdge = player.onGround && !this.hasSolidBelowOffset(player, world, data.deltaX, data.deltaZ);
        boolean bridgeContext = moving && (nearEdge || !hasBlockBelow || hasRecentSupport && data.pitch > 55.0F);

        if (bridgeContext && data.horizontalDelta > 0.18D && hasRecentSupport) {
            supportBuffer.flag(1.0D, 999.0D);
        } else {
            supportBuffer.decay(0.25D);
        }

        if (bridgeContext && (data.yawAcceleration > 45.0F || data.pitchAcceleration > 18.0F || data.yawDelta > 110.0F)) {
            rotationBuffer.flag(1.25D, 999.0D);
        } else {
            rotationBuffer.decay(0.3D);
        }

        if (bridgeContext && data.pitch > 63.0F && data.pitchDelta < 0.35F && data.yawDelta < 3.5F) {
            pitchBuffer.flag(0.9D, 999.0D);
        } else {
            pitchBuffer.decay(0.2D);
        }

        if (nearEdge && moving && !player.isSneaking() && data.groundTicks > 3) {
            edgeBuffer.flag(1.0D, 999.0D);
        } else {
            edgeBuffer.decay(0.25D);
        }

        boolean failed = supportBuffer.get() > 5.0D && rotationBuffer.get() > 2.0D
                || supportBuffer.get() > 6.0D && pitchBuffer.get() > 5.0D
                || edgeBuffer.get() > 8.0D && pitchBuffer.get() > 3.0D;
        if (failed) {
            long now = System.currentTimeMillis();
            long last = this.lastFlag.getOrDefault(name, 0L);
            if (now - last > 2500L) {
                context.receiveSignal(name, "Scaffold");
                this.lastFlag.put(name, now);
                supportBuffer.reset();
                rotationBuffer.reset();
                pitchBuffer.reset();
                edgeBuffer.reset();
            }
        }
    }

    private boolean isExempt(EntityPlayer player, PlayerCheckData data) {
        return player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || player.capabilities.isFlying
                || data.recentlyTeleported();
    }

    private boolean hasSolidBelow(EntityPlayer player, World world, double below) {
        for (double xOffset = -0.3D; xOffset <= 0.3D; xOffset += 0.3D) {
            for (double zOffset = -0.3D; zOffset <= 0.3D; zOffset += 0.3D) {
                BlockPos pos = new BlockPos(
                        MathHelper.floor_double(player.posX + xOffset),
                        MathHelper.floor_double(player.posY - below),
                        MathHelper.floor_double(player.posZ + zOffset)
                );
                if (!world.isAirBlock(pos)) return true;
            }
        }
        return false;
    }

    private boolean hasSolidBelowOffset(EntityPlayer player, World world, double motionX, double motionZ) {
        BlockPos pos = new BlockPos(
                MathHelper.floor_double(player.posX + motionX * 2.0D),
                MathHelper.floor_double(player.posY - 1.0D),
                MathHelper.floor_double(player.posZ + motionZ * 2.0D)
        );
        return !world.isAirBlock(pos);
    }

    public void reset() {
        this.supportBuffers.clear();
        this.rotationBuffers.clear();
        this.pitchBuffers.clear();
        this.edgeBuffers.clear();
        this.lastFlag.clear();
    }
}
