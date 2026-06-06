package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class AimModulo360Check {
    private final Map<String, Float> lastYaw = new HashMap<>();
    private final Map<String, Float> lastDeltaYaw = new HashMap<>();
    private final Map<String, Integer> buffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        String name = player.getName();
        float yaw = player.rotationYaw;
        if (!this.lastYaw.containsKey(name)) {
            this.lastYaw.put(name, yaw);
            this.lastDeltaYaw.put(name, 0.0F);
            return;
        }

        float delta = yaw - this.lastYaw.get(name);
        float lastDelta = this.lastDeltaYaw.getOrDefault(name, 0.0F);
        int vl = this.buffer.getOrDefault(name, 0);
        if (yaw < 360.0F && yaw > -360.0F && Math.abs(delta) > 320.0F && Math.abs(lastDelta) < 30.0F) {
            vl++;
            if (vl > 1) {
                context.receiveSignal(name, "AimModulo360");
                vl = 0;
            }
        } else {
            vl = Math.max(0, vl - 1);
        }
        this.buffer.put(name, vl);
        this.lastYaw.put(name, yaw);
        this.lastDeltaYaw.put(name, delta);
    }

    public void reset() {
        this.lastYaw.clear();
        this.lastDeltaYaw.clear();
        this.buffer.clear();
    }
}
