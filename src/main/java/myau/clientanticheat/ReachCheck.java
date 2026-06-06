package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ReachCheck {
    private static final double REACH_THRESHOLD = 3.45D;
    private final Map<String, Integer> buffer = new HashMap<>();

    public void check(EntityPlayer player, World world, ClientAntiCheatContext context) {
        boolean attacking = player.swingProgress > 0 && player.prevSwingProgress == 0;
        if (!attacking) return;

        double nearest = Double.MAX_VALUE;
        for (EntityPlayer target : world.playerEntities) {
            if (target == player || target.isDead) continue;
            double distance = player.getDistanceToEntity(target);
            if (distance < nearest) nearest = distance;
        }
        if (nearest == Double.MAX_VALUE || nearest > 6.0D) return;

        String name = player.getName();
        int vl = this.buffer.getOrDefault(name, 0);
        if (nearest > REACH_THRESHOLD) {
            vl++;
            if (vl > 3) {
                context.receiveSignal(name, "Reach");
                vl = 0;
            }
        } else {
            vl = Math.max(0, vl - 1);
        }
        this.buffer.put(name, vl);
    }

    public void reset() {
        this.buffer.clear();
    }
}
