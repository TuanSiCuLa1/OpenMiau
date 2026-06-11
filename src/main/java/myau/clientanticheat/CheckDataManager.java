package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CheckDataManager {
    private final Map<String, PlayerCheckData> data = new HashMap<>();

    public void update(World world) {
        Set<String> seen = new HashSet<>();
        for (EntityPlayer player : world.playerEntities) {
            String key = getPlayerKey(player);
            if (key == null) continue;
            seen.add(key);
            PlayerCheckData playerData = this.data.computeIfAbsent(key, ignored -> new PlayerCheckData(player));
            playerData.update(player);
        }
        Iterator<String> iterator = this.data.keySet().iterator();
        while (iterator.hasNext()) {
            if (!seen.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public PlayerCheckData get(EntityPlayer player) {
        String key = getPlayerKey(player);
        return key == null ? null : this.data.get(key);
    }

    public boolean isMovementExempt(EntityPlayer player, PlayerCheckData data) {
        return player == null
                || data == null
                || player.isDead
                || player.ticksExisted < 20
                || data.recentlyTeleported()
                || player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || player.capabilities.isFlying
                || player.capabilities.disableDamage;
    }

    public static String getPlayerKey(EntityPlayer player) {
        if (player == null) return null;
        if (player.getUniqueID() != null) return player.getUniqueID().toString();
        String name = player.getName();
        return name == null ? String.valueOf(player.getEntityId()) : name;
    }

    public void reset() {
        this.data.clear();
    }
}
