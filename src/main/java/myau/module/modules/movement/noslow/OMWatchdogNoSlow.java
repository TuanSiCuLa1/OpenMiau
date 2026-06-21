package myau.module.modules.movement.noslow;

import myau.event.types.EventType;
import myau.events.RightClickMouseEvent;
import myau.events.UpdateEvent;
import myau.module.modules.movement.NoSlow;
import myau.util.network.PacketUtil;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;

public class OMWatchdogNoSlow extends NoSlowMode {
    private int offGroundTicks;
    private boolean disable;

    public OMWatchdogNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
            if (mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.air && !mc.thePlayer.isUsingItem()) {
                disable = false;
            }

            double posY = mc.thePlayer.posY;
            if (Math.abs(posY - Math.round(posY)) > 0.03 && mc.thePlayer.onGround) {
                disable = true;
            }

            if (mc.thePlayer.isUsingItem() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                if (mc.thePlayer.onGround) {
                    offGroundTicks = 0;
                } else {
                    offGroundTicks++;
                }
            }
        }

        if (!disable) {
            if (this.getParent().isFoodActive() || this.getParent().isBowActive()) {
                float multiplier = this.getParent().getMotionMultiplier();
                mc.thePlayer.movementInput.moveForward *= multiplier;
                mc.thePlayer.movementInput.moveStrafe *= multiplier;
                if (!this.getParent().canSprint()) {
                    mc.thePlayer.setSprinting(false);
                }
            }
        }

        if (this.getParent().isSwordActive()) {
            int currentSlot = mc.thePlayer.inventory.currentItem;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 7 + (int) (Math.random() * 2) + 1));
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            
            float multiplier = this.getParent().getMotionMultiplier();
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.getParent().canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @Override
    public void onRightClick(RightClickMouseEvent event) {
        if (mc.thePlayer.getHeldItem() == null) {
            return;
        }

        if (mc.thePlayer.isUsingItem() || mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion || mc.thePlayer.getHeldItem().getItem() instanceof ItemFood || mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            if (offGroundTicks < 2 && offGroundTicks != 0 && !disable) {
                event.setCancelled(true);
            } else if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                event.setCancelled(true);
            }
        }
    }
}
