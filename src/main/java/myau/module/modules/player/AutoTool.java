package myau.module.modules.player;

import myau.module.modules.combat.KillAura;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import myau.util.PacketUtil;
import myau.util.TeamUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int serverSlot = -1;
    private int spoofedToolSlot = -1;
    private int previousSlot = -1;
    private int tickCounter;
    private int attackHeldSince = -1;
    private int hoverStartedAt = -1;
    private BlockPos lastHoverPos;
    private boolean swapped;

    public final IntProperty activationTime = new IntProperty("activation-time", 0, 0, 1000);
    public final IntProperty hoverDelay = new IntProperty("hover-delay", 0, 0, 1000);
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", true);
    public final BooleanProperty overrideSwitchBack = new BooleanProperty("override-switch-back", true);
    public final BooleanProperty spoofItem = new BooleanProperty("spoof-item", false);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneak-only", false);
    public final BooleanProperty requireLeftMouse = new BooleanProperty("require-left-mouse", true);

    public AutoTool() {
        super("AutoTool", false);
    }

    public boolean isKillAura() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura == null || !killAura.isEnabled()) return false;
        return TeamUtil.isEntityLoaded(killAura.getTarget()) && killAura.isAttackAllowed();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.resetState(true, false);
            return;
        }

        int currentTick = ++this.tickCounter;
        boolean attackDown = mc.gameSettings.keyBindAttack.isKeyDown();
        this.updateAttackState(attackDown, currentTick);

        if (!this.canAutoTool(currentTick)) {
            this.resetState(false, this.switchBack.getValue());
            return;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        int slot = this.findBestHotbarTool(block);
        if (slot == -1 || slot == mc.thePlayer.inventory.currentItem) return;

        if (this.previousSlot == -1) {
            this.previousSlot = mc.thePlayer.inventory.currentItem;
        } else if (this.overrideSwitchBack.getValue() && !this.swapped) {
            this.previousSlot = mc.thePlayer.inventory.currentItem;
        }

        this.selectTool(slot);
    }

    private boolean canAutoTool(int currentTick) {
        if (mc.currentScreen != null || mc.thePlayer.isDead || !mc.thePlayer.capabilities.allowEdit) return false;
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) return false;
        if (this.isKillAura() || mc.thePlayer.isUsingItem()) return false;
        if (this.sneakOnly.getValue() && !KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) return false;
        if (this.requireLeftMouse.getValue()) {
            if (!mc.gameSettings.keyBindAttack.isKeyDown()) return false;
            if (!this.hasElapsed(this.attackHeldSince, this.activationTime.getValue(), currentTick)) return false;
        }
        this.updateHoverState(mc.objectMouseOver, currentTick);
        if (!this.hasElapsed(this.hoverStartedAt, this.hoverDelay.getValue(), currentTick)) return false;
        Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
        return block.getBlockHardness(mc.theWorld, mc.objectMouseOver.getBlockPos()) != 0.0F;
    }

    private void updateAttackState(boolean attackDown, int currentTick) {
        if (attackDown) {
            if (this.attackHeldSince == -1) this.attackHeldSince = currentTick;
        } else {
            this.attackHeldSince = -1;
        }
    }

    private void updateHoverState(MovingObjectPosition mop, int currentTick) {
        BlockPos hoverPos = mop == null || mop.typeOfHit != MovingObjectType.BLOCK ? null : mop.getBlockPos();
        if (hoverPos == null) {
            this.hoverStartedAt = -1;
            this.lastHoverPos = null;
            return;
        }
        if (!hoverPos.equals(this.lastHoverPos)) {
            this.lastHoverPos = hoverPos;
            this.hoverStartedAt = currentTick;
        }
    }

    private boolean hasElapsed(int startTick, int requiredMs, int currentTick) {
        int requiredTicks = requiredMs <= 0 ? 0 : (int) Math.ceil(requiredMs / 50.0D);
        return requiredTicks <= 0 || startTick != -1 && currentTick - startTick >= requiredTicks;
    }

    private int findBestHotbarTool(Block block) {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int bestSlot = ItemUtil.findInventorySlot(currentSlot, block);
        return bestSlot == currentSlot ? -1 : bestSlot;
    }

    private void selectTool(int slot) {
        if (this.spoofItem.getValue()) {
            this.selectToolSilently(slot);
        } else if (slot != mc.thePlayer.inventory.currentItem) {
            this.switchToSlot(slot);
        }
        this.swapped = true;
    }

    private void selectToolSilently(int slot) {
        if (this.serverSlot == -1) this.serverSlot = mc.thePlayer.inventory.currentItem;
        if (this.spoofedToolSlot != slot || mc.thePlayer.inventory.currentItem != slot) {
            this.switchToSlot(slot);
            this.spoofedToolSlot = slot;
        }
    }

    private void switchToSlot(int slot) {
        mc.thePlayer.inventory.currentItem = slot;
        PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
    }

    private void resetState(boolean resetTimers, boolean switchBackToPrevious) {
        if (switchBackToPrevious) {
            if (this.spoofItem.getValue()) {
                this.resetSilentSlot(true);
            } else if (this.previousSlot != -1 && this.previousSlot != mc.thePlayer.inventory.currentItem) {
                this.switchToSlot(this.previousSlot);
            }
        } else {
            this.resetSilentSlot(false);
        }
        this.previousSlot = -1;
        this.swapped = false;
        if (resetTimers) {
            this.tickCounter = 0;
            this.attackHeldSince = -1;
            this.hoverStartedAt = -1;
            this.lastHoverPos = null;
        }
    }

    private void resetSilentSlot(boolean sendSwitchBack) {
        if (this.serverSlot != -1 && sendSwitchBack) {
            this.switchToSlot(this.serverSlot);
        }
        this.serverSlot = -1;
        this.spoofedToolSlot = -1;
    }

    @Override
    public void onDisabled() {
        this.resetState(true, true);
    }
}