package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class VelocityCheck {
    private final Map<String, Integer> velocityWindows = new HashMap<>();
    private final Map<String, CheckBuffer> horizontalBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> verticalBuffers = new HashMap<>();

    public void check(EntityPlayer player, PlayerCheckData data, ClientAntiCheatContext context) {
        String key = CheckDataManager.getPlayerKey(player);
        String name = player.getName();
        if (key == null || name == null) return;
        CheckBuffer horizontalBuffer = this.horizontalBuffers.computeIfAbsent(key, ignored -> new CheckBuffer());
        CheckBuffer verticalBuffer = this.verticalBuffers.computeIfAbsent(key, ignored -> new CheckBuffer());
        if (isExempt(player, data)) {
            reset(key);
            return;
        }

        if (player.hurtTime > 0 || player.hurtResistantTime > 10) {
            this.velocityWindows.put(key, 0);
            horizontalBuffer.reset();
            verticalBuffer.reset();
            return;
        }

        if (!this.velocityWindows.containsKey(key)) {
            horizontalBuffer.decay(0.25D);
            verticalBuffer.decay(0.25D);
            return;
        }

        int ticks = this.velocityWindows.get(key) + 1;
        this.velocityWindows.put(key, ticks);
        if (ticks > 12) {
            this.velocityWindows.remove(key);
            return;
        }

        boolean horizontalMissing = ticks >= 2 && ticks <= 7 && data.horizontalDelta < 0.018D && data.lastHorizontalDelta < 0.05D && !player.isCollidedHorizontally;
        boolean verticalMissing = ticks >= 1 && ticks <= 5 && data.deltaY <= 0.005D && !data.onGround && data.airTicks <= 3;
        if (horizontalMissing) {
            if (horizontalBuffer.flag(1.0D, 2.5D)) {
                context.receiveSignal(name, "Velocity");
                reset(key);
                return;
            }
        } else {
            horizontalBuffer.decay(0.5D);
        }

        if (verticalMissing) {
            if (verticalBuffer.flag(1.0D, 2.0D)) {
                context.receiveSignal(name, "Velocity");
                reset(key);
            }
        } else {
            verticalBuffer.decay(0.5D);
        }
    }

    private boolean isExempt(EntityPlayer player, PlayerCheckData data) {
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

    private void reset(String name) {
        this.velocityWindows.remove(name);
        this.horizontalBuffers.remove(name);
        this.verticalBuffers.remove(name);
    }

    public void reset() {
        this.velocityWindows.clear();
        this.horizontalBuffers.clear();
        this.verticalBuffers.clear();
    }
}
