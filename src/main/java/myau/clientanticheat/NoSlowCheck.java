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
    private final Map<String, CheckBuffer> sprintBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> speedBuffers = new HashMap<>();

    public void check(EntityPlayer player, long currentTick, ClientAntiCheatContext context) {
        check(player, null, currentTick, context);
    }

    public void check(EntityPlayer player, PlayerCheckData data, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        ItemStack heldItem = player.getHeldItem();
        boolean usingSlowItem = this.isSlowItem(heldItem) && (player.isBlocking() || player.isEating() || player.isUsingItem());
        CheckBuffer sprintBuffer = this.sprintBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer speedBuffer = this.speedBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        boolean exempt = player.hurtTime > 0
                || player.hurtResistantTime > 10
                || player.isCollidedHorizontally
                || player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || data != null && data.recentlyTeleported();

        if (usingSlowItem && !exempt) {
            this.usingTicks.putIfAbsent(name, currentTick);
            long ticksUsing = currentTick - this.usingTicks.get(name);
            if (ticksUsing > 5 && player.isSprinting()) {
                if (sprintBuffer.flag(1.0D, 2.5D)) {
                    context.receiveSignal(name, "Noslow");
                    sprintBuffer.reset();
                }
            } else {
                sprintBuffer.decay(0.4D);
            }

            double horizontalSpeed = data != null ? data.horizontalDelta : Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
            double maxExpected = player.onGround ? 0.205D : 0.285D;
            if (ticksUsing > 7 && horizontalSpeed > maxExpected && player.hurtTime == 0) {
                if (speedBuffer.flag(1.0D + Math.min(1.0D, horizontalSpeed - maxExpected), 3.5D)) {
                    context.receiveSignal(name, "Noslow");
                    speedBuffer.reset();
                }
            } else {
                speedBuffer.decay(0.35D);
            }
        } else {
            this.usingTicks.remove(name);
            sprintBuffer.decay(0.5D);
            speedBuffer.decay(0.5D);
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

    public void reset() {
        this.usingTicks.clear();
        this.sprintBuffers.clear();
        this.speedBuffers.clear();
    }
}
