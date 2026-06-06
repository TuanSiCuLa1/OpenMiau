package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class AimDuplicateLookCheck {
    private final Map<String, Float> lastYaw = new HashMap<>();
    private final Map<String, Float> lastPitch = new HashMap<>();
    private final Map<String, Integer> duplicateBuffer = new HashMap<>();

    public void check(EntityPlayer player, World world, ClientAntiCheatContext context) {
        String name = player.getName();
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        boolean duplicate = this.lastYaw.containsKey(name)
                && Float.compare(this.lastYaw.get(name), yaw) == 0
                && Float.compare(this.lastPitch.get(name), pitch) == 0;
        this.lastYaw.put(name, yaw);
        this.lastPitch.put(name, pitch);

        boolean combat = player.swingProgress > 0 && this.hasNearbyTarget(player, world);
        int buffer = this.duplicateBuffer.getOrDefault(name, 0);
        if (duplicate && combat) {
            buffer++;
            if (buffer > 8) {
                context.receiveSignal(name, "AimDuplicateLook");
                buffer = 0;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
        this.duplicateBuffer.put(name, buffer);
    }

    private boolean hasNearbyTarget(EntityPlayer player, World world) {
        for (EntityPlayer target : world.playerEntities) {
            if (target != player && !target.isDead && player.getDistanceToEntity(target) < 6.0F) return true;
        }
        return false;
    }

    public void reset() {
        this.lastYaw.clear();
        this.lastPitch.clear();
        this.duplicateBuffer.clear();
    }
}
