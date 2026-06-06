package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class AutoBlockCheck {
    private final Map<String, Long> guardingTicks = new HashMap<>();
    private final Map<String, Integer> attackWhileBlockingBuffer = new HashMap<>();
    private final Map<String, Integer> sprintBlockBuffer = new HashMap<>();
    private final Map<String, Integer> impossibleBlockBuffer = new HashMap<>();

    public void check(EntityPlayer player, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        ItemStack heldItem = player.getHeldItem();
        boolean holdingSword = heldItem != null && heldItem.getItem() instanceof ItemSword;
        boolean blocking = player.isBlocking();
        boolean guarding = holdingSword && blocking;
        boolean attacking = player.swingProgress > 0 && player.prevSwingProgress == 0;
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

        if (!holdingSword && blocking) {
            this.buffer(name, this.impossibleBlockBuffer, 2, "AutoBlock", context);
            return;
        }

        if (guarding) {
            this.guardingTicks.putIfAbsent(name, currentTick);
            long ticksGuarded = currentTick - this.guardingTicks.get(name);
            if (attacking && ticksGuarded > 3) {
                this.buffer(name, this.attackWhileBlockingBuffer, 2, "AutoBlock", context);
            } else {
                this.decay(name, this.attackWhileBlockingBuffer);
            }

            if (player.isSprinting() && horizontalSpeed > 0.16D && ticksGuarded > 4) {
                this.buffer(name, this.sprintBlockBuffer, 6, "AutoBlock", context);
            } else {
                this.decay(name, this.sprintBlockBuffer);
            }
        } else {
            this.guardingTicks.remove(name);
            this.decay(name, this.attackWhileBlockingBuffer);
            this.decay(name, this.sprintBlockBuffer);
        }
    }

    private void buffer(String name, Map<String, Integer> bufferMap, int threshold, String check, ClientAntiCheatContext context) {
        int buffer = bufferMap.getOrDefault(name, 0) + 1;
        if (buffer >= threshold) {
            context.receiveSignal(name, check);
            buffer = 0;
        }
        bufferMap.put(name, buffer);
    }

    private void decay(String name, Map<String, Integer> bufferMap) {
        int buffer = bufferMap.getOrDefault(name, 0);
        if (buffer > 0) {
            bufferMap.put(name, buffer - 1);
        }
    }

    public void reset() {
        this.guardingTicks.clear();
        this.attackWhileBlockingBuffer.clear();
        this.sprintBlockBuffer.clear();
        this.impossibleBlockBuffer.clear();
    }
}
