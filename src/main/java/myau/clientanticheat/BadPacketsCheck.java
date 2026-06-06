package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class BadPacketsCheck {
    private final Map<String, Float> lastYaw = new HashMap<>();
    private final Map<String, Float> lastPitch = new HashMap<>();
    private final Map<String, Integer> buffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        String name = player.getName();
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        int vl = this.buffer.getOrDefault(name, 0);

        boolean invalidPitch = pitch > 90.0F || pitch < -90.0F;
        boolean hugeAcceleration = false;
        if (this.lastYaw.containsKey(name)) {
            float yawDiff = Math.abs(yaw - this.lastYaw.get(name));
            float pitchDiff = Math.abs(pitch - this.lastPitch.get(name));
            hugeAcceleration = yawDiff > 160.0F && pitchDiff > 60.0F;
        }

        if (invalidPitch || hugeAcceleration) {
            vl++;
            if (vl > 2) {
                context.receiveSignal(name, invalidPitch ? "BadPacketsPitch" : "BadPacketsRotation");
                vl = 0;
            }
        } else {
            vl = Math.max(0, vl - 1);
        }

        this.buffer.put(name, vl);
        this.lastYaw.put(name, yaw);
        this.lastPitch.put(name, pitch);
    }

    public void reset() {
        this.lastYaw.clear();
        this.lastPitch.clear();
        this.buffer.clear();
    }
}
