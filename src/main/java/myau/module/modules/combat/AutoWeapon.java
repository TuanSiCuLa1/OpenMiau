package myau.module.modules.combat;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class AutoWeapon extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final BooleanProperty onlySwordValue = new BooleanProperty("OnlySword", false);
    private final BooleanProperty silentValue = new BooleanProperty("SpoofItem", false);
    private final IntProperty ticksValue = new IntProperty("SpoofTicks", 10, 1, 20);

    private boolean attackEnemy = false;
    private int spoofedSlot = 0;

    public AutoWeapon() {
        super("AutoWeapon", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        attackEnemy = true;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof C02PacketUseEntity))
            return;

        C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();

        if (packet.getAction() != C02PacketUseEntity.Action.ATTACK || !attackEnemy)
            return;

        attackEnemy = false;

        // Find best weapon in hotbar
        int slot = this.findBestWeaponSlot();

        if (slot == -1)
            return;

        if (slot == mc.thePlayer.inventory.currentItem) { // If in hand no need to swap
            return;
        }

        // Switch to best weapon
        this.switchWeapon(slot);

        // Resend attack packet
        mc.getNetHandler().addToSendQueue(packet);
        event.setCancelled(true);
    }

    private int findBestWeaponSlot() {
        int bestSlot = -1;
        double bestDamage = -1.0D;

        for (int slot = 0; slot < 9; slot++) {

            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);

            if (stack == null)
                continue;

            if (!(stack.getItem() instanceof ItemSword || (stack.getItem() instanceof ItemTool && !onlySwordValue.getValue())))
                continue;

            double damage = this.getWeaponDamage(stack);

            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private double getWeaponDamage(ItemStack stack) {
        double damage = 0.0D;

        try {
            damage = stack.getAttributeModifiers().get("generic.attackDamage").iterator().next().getAmount();
        } catch (Exception ignored) {
        }

        return damage + 1.25D * EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
    }

    private void switchWeapon(int slot) {
        if (silentValue.getValue()) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
            spoofedSlot = ticksValue.getValue();
        } else {
            mc.thePlayer.inventory.currentItem = slot;
            mc.playerController.updateController();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {

        // Switch back to old item after some time
        if (spoofedSlot > 0) {
            if (spoofedSlot == 1) {
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            }
            spoofedSlot--;
        }
    }
}