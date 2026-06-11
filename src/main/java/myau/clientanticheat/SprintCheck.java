package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class SprintCheck {
    private final Map<String, CheckBuffer> blockSprintBuffers = new HashMap<>();
    private final Map<String, CheckBuffer> omniSprintBuffers = new HashMap<>();

    public void check(EntityPlayer player, PlayerCheckData data, ClientAntiCheatContext context) {
        if (data == null || player.ticksExisted < 20 || data.recentlyTeleported() || data.recentlyHurt()) return;
        String key = CheckDataManager.getPlayerKey(player);
        String name = player.getName();
        if (key == null || name == null) return;
        CheckBuffer blockBuffer = this.blockSprintBuffers.computeIfAbsent(key, ignored -> new CheckBuffer());
        CheckBuffer omniBuffer = this.omniSprintBuffers.computeIfAbsent(key, ignored -> new CheckBuffer());

        ItemStack heldItem = player.getHeldItem();
        boolean blocking = heldItem != null && heldItem.getItem() instanceof ItemSword && player.isBlocking();
        boolean blockSprint = blocking && player.isSprinting() && data.horizontalDelta > 0.16D;
        if (blockSprint) {
            if (blockBuffer.flag(1.0D, 6.0D)) {
                context.receiveSignal(name, "Sprint");
                blockBuffer.reset();
            }
        } else {
            blockBuffer.decay(0.45D);
        }

        boolean movingSideways = Math.abs(player.moveStrafing) > Math.abs(player.moveForward) && Math.abs(player.moveStrafing) > 0.0F;
        boolean movingBackwards = player.moveForward < 0.0F;
        boolean omniSprint = player.isSprinting() && (movingSideways || movingBackwards) && data.horizontalDelta > 0.12D;
        if (omniSprint) {
            if (omniBuffer.flag(1.0D, 4.0D)) {
                context.receiveSignal(name, "OmniSprint");
                omniBuffer.reset();
            }
        } else {
            omniBuffer.decay(0.35D);
        }
    }

    public void reset() {
        this.blockSprintBuffers.clear();
        this.omniSprintBuffers.clear();
    }
}
