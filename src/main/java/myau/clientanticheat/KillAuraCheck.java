package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class KillAuraCheck {
    private final Map<String, Long> lastAttackTime = new HashMap<>();
    private final Map<String, Integer> consecutiveHeadshots = new HashMap<>();

    public void check(EntityPlayer player, World world, long currentTick, ClientAntiCheatContext context) {
        if (!this.hasNearbyPlayerTarget(player, world)) {
            return;
        }
        boolean attacking = player.swingProgress > 0 && player.prevSwingProgress == 0;
        if (attacking) {
            long lastAttack = this.lastAttackTime.getOrDefault(player.getName(), currentTick);
            long delay = currentTick - lastAttack;
            if (delay > 0 && delay < 3) {
                context.receiveSignal(player.getName(), "KillAura");
            }
            this.lastAttackTime.put(player.getName(), currentTick);

            int headshots = this.consecutiveHeadshots.getOrDefault(player.getName(), 0) + 1;
            this.consecutiveHeadshots.put(player.getName(), headshots);
            if (headshots > 8) {
                context.receiveSignal(player.getName(), "KillAura");
                this.consecutiveHeadshots.put(player.getName(), 0);
            }
        } else {
            this.consecutiveHeadshots.put(player.getName(), 0);
        }
    }

    private boolean hasNearbyPlayerTarget(EntityPlayer player, World world) {
        Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        for (EntityPlayer target : world.playerEntities) {
            if (target == player || target.isDead) continue;
            Vec3 targetPos = new Vec3(target.posX, target.posY + target.getEyeHeight(), target.posZ);
            double distance = playerPos.distanceTo(targetPos);
            if (distance < 6.0 && distance > 0.1) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        this.lastAttackTime.clear();
        this.consecutiveHeadshots.clear();
    }
}
