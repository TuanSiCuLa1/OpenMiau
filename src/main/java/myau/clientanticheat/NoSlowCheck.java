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
    private final Map<String, CheckBuffer> sprintBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> speedBuffers = new HashMap<>();

    public void check(EntityPlayer player, PlayerCheckData data, long currentTick, ClientAntiCheatContext context) {
        String name = player.getName();
        if (name == null) return;

        ItemStack heldItem = player.getHeldItem();
        boolean usingSlowItem = this.isSlowItem(heldItem) && (player.isBlocking() || player.isEating() || player.isUsingItem());
        CheckBuffer sprintBuffer = this.sprintBuffers.computeIfAbsent(name, key -> new CheckBuffer());
        CheckBuffer speedBuffer = this.speedBuffers.computeIfAbsent(name, key -> new CheckBuffer());

        if (!usingSlowItem || this.isExempt(player, data)) {
            sprintBuffer.decay(0.5D);
            speedBuffer.decay(0.5D);
            return;
        }

        int ticksUsing = data != null ? data.usingItemTicks : 0;
        double horizontalSpeed = data != null ? data.horizontalDelta : Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double expected = player.onGround ? 0.155D : 0.245D;
        if (player.isPotionActive(net.minecraft.potion.Potion.moveSpeed)) {
            expected += 0.035D * (player.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier() + 1);
        }

        if (ticksUsing > 4 && player.isSprinting()) {
            if (sprintBuffer.flag(1.0D, 2.5D)) {
                context.receiveSignal(name, "Noslow");
                sprintBuffer.reset();
            }
        } else {
            sprintBuffer.decay(0.35D);
        }

        if (ticksUsing > 6 && horizontalSpeed > expected) {
            double over = horizontalSpeed - expected;
            if (speedBuffer.flag(1.0D + Math.min(1.5D, over * 6.0D), 4.0D)) {
                context.receiveSignal(name, "Noslow");
                speedBuffer.reset();
            }
        } else {
            speedBuffer.decay(0.3D);
        }
    }

    private boolean isExempt(EntityPlayer player, PlayerCheckData data) {
        return player.hurtTime > 0
                || player.hurtResistantTime > 10
                || player.isCollidedHorizontally
                || player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || player.capabilities.isFlying
                || data != null && data.recentlyTeleported();
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
        this.sprintBuffers.clear();
        this.speedBuffers.clear();
    }
}
