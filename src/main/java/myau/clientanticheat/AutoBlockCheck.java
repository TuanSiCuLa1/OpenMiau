package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class AutoBlockCheck {
    private final Map<String, Long> guardingTicks = new HashMap<>();
    private final Map<String, Long> lastBlockStart = new HashMap<>();
    private final Map<String, CheckBuffer> attackWhileBlockingBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> sprintBlockBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> rapidToggleBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> impossibleBlockBuffers = new HashMap<>();

    public void check(EntityPlayer player, PlayerCheckData data, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        if (name == null) return;

        ItemStack heldItem = player.getHeldItem();
        boolean holdingSword = heldItem != null && heldItem.getItem() instanceof ItemSword;
        boolean blocking = player.isBlocking();
        boolean guarding = holdingSword && blocking;
        boolean attacking = data != null ? data.startedSwinging() : player.swingProgress > 0.0F && player.prevSwingProgress == 0.0F;
        double horizontalSpeed = data != null ? data.horizontalDelta : Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

        CheckBuffer attackBuffer = this.attackWhileBlockingBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer sprintBuffer = this.sprintBlockBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer rapidBuffer = this.rapidToggleBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer impossibleBuffer = this.impossibleBlockBuffers.computeIfAbsent(name, key -> new CheckBuffer());

        if (!holdingSword && blocking) {
            if (impossibleBuffer.flag(1.5D, 3.0D)) {
                context.receiveSignal(name, "AutoBlock");
                impossibleBuffer.reset();
            }
            return;
        }

        if (guarding) {
            Long previousStart = this.guardingTicks.putIfAbsent(name, currentTick);
            if (previousStart == null) {
                long lastStart = this.lastBlockStart.getOrDefault(name, -100L);
                if (currentTick - lastStart <= 3L) {
                    if (rapidBuffer.flag(1.0D, 3.0D)) {
                        context.receiveSignal(name, "AutoBlock");
                        rapidBuffer.reset();
                    }
                }
                this.lastBlockStart.put(name, currentTick);
            }

            long ticksGuarded = currentTick - this.guardingTicks.get(name);
            if (attacking && ticksGuarded > 1L) {
                if (attackBuffer.flag(1.25D, 3.5D)) {
                    context.receiveSignal(name, "AutoBlock");
                    attackBuffer.reset();
                }
            } else {
                attackBuffer.decay(0.35D);
            }

            if (player.isSprinting() && horizontalSpeed > 0.17D && ticksGuarded > 3L) {
                if (sprintBuffer.flag(1.0D, 5.0D)) {
                    context.receiveSignal(name, "AutoBlock");
                    sprintBuffer.reset();
                }
            } else {
                sprintBuffer.decay(0.25D);
            }
        } else {
            this.guardingTicks.remove(name);
            attackBuffer.decay(0.45D);
            sprintBuffer.decay(0.35D);
            rapidBuffer.decay(0.15D);
        }
    }

    public void reset() {
        this.guardingTicks.clear();
        this.lastBlockStart.clear();
        this.attackWhileBlockingBuffers.clear();
        this.sprintBlockBuffers.clear();
        this.rapidToggleBuffers.clear();
        this.impossibleBlockBuffers.clear();
    }
}
