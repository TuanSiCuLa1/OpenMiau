package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class BlinkCheck {
    private final Map<String, Integer> frozenTicks = new HashMap<>();
    private final Map<String, CheckBuffer> burstBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> pulseBuffers = new HashMap<>();

    public void check(EntityPlayer player, PlayerCheckData data, ClientAntiCheatContext context) {
        String name = player.getName();
        if (name == null || data == null) return;
        CheckBuffer burstBuffer = this.burstBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer pulseBuffer = this.pulseBuffers.computeIfAbsent(name, key -> new CheckBuffer());

        if (isExempt(player, data)) {
            this.frozenTicks.remove(name);
            burstBuffer.decay(0.5D);
            pulseBuffer.decay(0.5D);
            return;
        }

        boolean frozen = data.totalDelta < 0.002D && data.yawDelta < 0.03F && data.pitchDelta < 0.03F;
        if (frozen) {
            this.frozenTicks.put(name, this.frozenTicks.getOrDefault(name, 0) + 1);
            burstBuffer.decay(0.1D);
            return;
        }

        int frozenBefore = this.frozenTicks.getOrDefault(name, 0);
        this.frozenTicks.remove(name);
        boolean catchUpBurst = frozenBefore >= 8 && data.totalDelta > 0.65D && data.totalDelta < 8.0D;
        boolean pulseMove = data.totalDelta > 1.15D && data.totalDelta < 8.0D && data.horizontalDelta > 0.8D;

        if (catchUpBurst) {
            if (burstBuffer.flag(1.5D + Math.min(1.0D, frozenBefore / 20.0D), 3.0D)) {
                context.receiveSignal(name, "Blink");
                burstBuffer.reset();
                pulseBuffer.reset();
            }
        } else {
            burstBuffer.decay(0.25D);
        }

        if (pulseMove && !player.isCollidedHorizontally && data.airTicks < 8) {
            if (pulseBuffer.flag(1.0D, 3.5D)) {
                context.receiveSignal(name, "Blink");
                pulseBuffer.reset();
            }
        } else {
            pulseBuffer.decay(0.2D);
        }
    }

    private boolean isExempt(EntityPlayer player, PlayerCheckData data) {
        return player.isDead
                || player.ticksExisted < 40
                || data.recentlyTeleported()
                || player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || player.capabilities.isFlying;
    }

    public void reset() {
        this.frozenTicks.clear();
        this.burstBuffers.clear();
        this.pulseBuffers.clear();
    }
}
