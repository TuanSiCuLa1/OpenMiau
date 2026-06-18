package myau.module.modules.movement;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.module.modules.player.InvWalk;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

public class Eagle extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty onlyGround = new BooleanProperty("only-ground", false);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", false);
    public final BooleanProperty noSpeedPotion = new BooleanProperty("no-speed-potion", false);
    public final BooleanProperty onHoldShift = new BooleanProperty("on-hold-shift", false);
    public final BooleanProperty pitchLimit = new BooleanProperty("pitch", false);
    public final IntProperty shiftMax = new IntProperty("shift-max", 0, 0, 20);
    public final IntProperty shiftMin = new IntProperty("shift-min", 0, 0, 20);
    public final IntProperty pitchMax = new IntProperty("pitch-max", 90, 0, 90, this.pitchLimit::getValue);
    public final IntProperty pitchMin = new IntProperty("pitch-min", 0, 0, 90, this.pitchLimit::getValue);

    public Eagle() {
        super("Eagle", false);
    }

    private boolean isInvWalking() {
        InvWalk invWalk = (InvWalk) Myau.moduleManager.modules.get(InvWalk.class);
        return invWalk != null && invWalk.isEnabled() && invWalk.canInvWalk();
    }

    private boolean canSneak() {
        if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        if (this.isInvWalking()) {
            return false;
        }
        if (!mc.gameSettings.keyBindBack.isKeyDown()) {
            return false;
        }
        if (this.blocksOnly.getValue() && (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock))) {
            return false;
        }
        if (this.onHoldShift.getValue() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return false;
        }
        if (this.onlyGround.getValue() && !mc.thePlayer.onGround) {
            return false;
        }
        if (this.noSpeedPotion.getValue() && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return false;
        }
        return !this.pitchLimit.getValue() || mc.thePlayer.rotationPitch < this.pitchMax.getValue() && mc.thePlayer.rotationPitch > this.pitchMin.getValue();
    }

    private double getShift() {
        double min = Math.min(this.shiftMin.getValue(), this.shiftMax.getValue()) / 10.0D;
        double max = Math.max(this.shiftMin.getValue(), this.shiftMax.getValue()) / 10.0D;
        return Math.random() * (max - min) + min;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (canSneak()) {
            BlockPos checkPos = new BlockPos(
                    mc.thePlayer.posX + mc.thePlayer.motionX * getShift(),
                    mc.thePlayer.posY - 1.0D,
                    mc.thePlayer.posZ + mc.thePlayer.motionZ * getShift()
            );
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.air);
            return;
        }

        if (mc.thePlayer != null && mc.thePlayer.moveForward > 0.0F && mc.thePlayer.isSneaking() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
        if (this.blocksOnly.getValue() && mc.thePlayer != null && (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock))) {
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "shift-min":
                if (this.shiftMin.getValue() > this.shiftMax.getValue()) {
                    this.shiftMax.setValue(this.shiftMin.getValue());
                }
                break;
            case "shift-max":
                if (this.shiftMin.getValue() > this.shiftMax.getValue()) {
                    this.shiftMin.setValue(this.shiftMax.getValue());
                }
                break;
            case "pitch-min":
                if (this.pitchMin.getValue() > this.pitchMax.getValue()) {
                    this.pitchMax.setValue(this.pitchMin.getValue());
                }
                break;
            case "pitch-max":
                if (this.pitchMin.getValue() > this.pitchMax.getValue()) {
                    this.pitchMin.setValue(this.pitchMax.getValue());
                }
                break;
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.shiftMin.getValue(), this.shiftMax.getValue())
                ? new String[]{this.shiftMin.getValue().toString()}
                : new String[]{String.format("%d-%d", this.shiftMin.getValue(), this.shiftMax.getValue())};
    }
}