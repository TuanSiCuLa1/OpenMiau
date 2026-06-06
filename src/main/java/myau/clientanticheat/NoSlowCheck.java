package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class NoSlowCheck {
    private final Map<String, Long> usingTicks = new HashMap<>();
    private final Map<String, Integer> sprintBuffer = new HashMap<>();
    private final Map<String, Integer> speedBuffer = new HashMap<>();

    public void check(EntityPlayer player, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        ItemStack heldItem = player.getHeldItem();
        boolean usingSlowItem = this.isSlowItem(heldItem) && (player.isBlocking() || player.isEating() || player.isUsingItem());
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

        if (usingSlowItem) {
            this.usingTicks.putIfAbsent(name, currentTick);
            long ticksUsing = currentTick - this.usingTicks.get(name);
            if (ticksUsing > 5 && player.isSprinting()) {
                this.buffer(name, this.sprintBuffer, 3, context);
            } else {
                this.decay(name, this.sprintBuffer);
            }

            double maxExpected = player.onGround ? 0.18D : 0.26D;
            if (ticksUsing > 7 && horizontalSpeed > maxExpected) {
                this.buffer(name, this.speedBuffer, 4, context);
            } else {
                this.decay(name, this.speedBuffer);
            }
        } else {
            this.usingTicks.remove(name);
            this.decay(name, this.sprintBuffer);
            this.decay(name, this.speedBuffer);
        }
    }

    private boolean isSlowItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        return stack.getItem() instanceof ItemSword
                || stack.getItem() instanceof ItemBow
                || stack.getItem() instanceof ItemFood
                || stack.getItem() instanceof ItemPotion
                || stack.getItem() instanceof ItemBlock;
    }

    private void buffer(String name, Map<String, Integer> bufferMap, int threshold, ClientAntiCheatContext context) {
        int buffer = bufferMap.getOrDefault(name, 0) + 1;
        if (buffer >= threshold) {
            context.receiveSignal(name, "Noslow");
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
        this.usingTicks.clear();
        this.sprintBuffer.clear();
        this.speedBuffer.clear();
    }
}
