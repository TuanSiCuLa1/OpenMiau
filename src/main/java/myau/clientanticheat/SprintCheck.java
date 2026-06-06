package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class SprintCheck {
    private final Map<String, Integer> buffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        ItemStack heldItem = player.getHeldItem();
        boolean blocking = heldItem != null && heldItem.getItem() instanceof ItemSword && player.isBlocking();
        boolean suspicious = blocking && player.isSprinting() && player.motionY == 0.0D;
        String name = player.getName();
        int vl = this.buffer.getOrDefault(name, 0);
        if (suspicious) {
            vl++;
            if (vl > 10) {
                context.receiveSignal(name, "Sprint");
                vl = 0;
            }
        } else {
            vl = Math.max(0, vl - 1);
        }
        this.buffer.put(name, vl);
    }

    public void reset() {
        this.buffer.clear();
    }
}
