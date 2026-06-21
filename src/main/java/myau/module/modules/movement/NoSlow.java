package myau.module.modules.movement;

import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.module.modules.movement.noslow.*;
import myau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemPotion;

import java.util.ArrayList;
import java.util.List;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "NCP", "NEW_NCP", "WATCHDOG", "INTAVE", "GRIM"});
    public final BooleanProperty swordValue = new BooleanProperty("sword", true);
    public final BooleanProperty foodValue = new BooleanProperty("food", true);
    public final BooleanProperty potionValue = new BooleanProperty("potion", true);
    public final BooleanProperty bowValue = new BooleanProperty("bow", true);
    public final BooleanProperty antiSwitch = new BooleanProperty("anti-switch", false);
    
    private final List<NoSlowMode> modes = new ArrayList<>();

    public NoSlow() {
        super("NoSlow", false);
        modes.add(new OMVanillaNoSlow("VANILLA", this));
        modes.add(new OMNCPNoSlow("NCP", this));
        modes.add(new OMNewNCPNoSlow("NEW_NCP", this));
        modes.add(new OMWatchdogNoSlow("WATCHDOG", this));
        modes.add(new OMIntaveNoSlow("INTAVE", this));
        modes.add(new OMGrimNoSlow("GRIM", this));
    }

    private NoSlowMode getActiveMode() {
        return modes.get(this.mode.getValue());
    }

    public void onEnable() {
        getActiveMode().onEnable();
    }

    public void onDisable() {
        getActiveMode().onDisable();
    }

    public boolean isSwordActive() {
        return this.swordValue.getValue() && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodValue.getValue() && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowValue.getValue() && ItemUtil.isUsingBow();
    }

    public boolean isPotionActive() {
        return this.potionValue.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion;
    }

    public boolean isAntiSwitchActive() {
        if (!this.isEnabled() || !this.antiSwitch.getValue() || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        net.minecraft.item.ItemStack heldItem = mc.thePlayer.getHeldItem();
        return heldItem != null && heldItem.getItem() instanceof net.minecraft.item.ItemSword && mc.thePlayer.isUsingItem();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (isSwordActive() || isFoodActive() || isBowActive() || isPotionActive());
    }

    public boolean canSprint() {
        return true;
    }

    public float getMotionMultiplier() {
        if (this.mode.getValue() == 5) { // GRIM
            return 0.35f;
        }
        return 1.0f;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onUpdate(event);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onPacket(event);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onRightClick(event);
        }
    }
}