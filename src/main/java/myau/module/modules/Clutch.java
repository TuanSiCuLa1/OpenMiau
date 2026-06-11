package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PacketUtil;
import myau.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Comparator;

public class Clutch extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double HALF_WIDTH = 0.3D;
    private static final double[][] CORNERS = {{-HALF_WIDTH, -HALF_WIDTH}, {HALF_WIDTH, -HALF_WIDTH}, {-HALF_WIDTH, HALF_WIDTH}, {HALF_WIDTH, HALF_WIDTH}};

    public final FloatProperty reach = new FloatProperty("reach", 4.5F, 0.5F, 4.5F);
    public final IntProperty speed = new IntProperty("speed", 8, 0, 100);
    public final IntProperty snapbackSpeed = new IntProperty("snapback-speed", 12, 0, 100);
    public final IntProperty maxDistance = new IntProperty("max-distance", 10, 0, 20);
    public final IntProperty rotationTolerance = new IntProperty("rotation-tolerance", 25, 20, 100);
    public final BooleanProperty simulateFuturePosition = new BooleanProperty("simulate-future-position", true);
    public final BooleanProperty autoClutch = new BooleanProperty("auto-clutch", false);
    public final IntProperty minimumFallDistance = new IntProperty("minimum-fall-distance", 10, 3, 20);
    public final IntProperty selectKeybind = new IntProperty("select-keybind", 0, 0, Keyboard.KEYBOARD_SIZE - 1);

    private boolean hasAim;
    private boolean resetting;
    private boolean placing;
    private boolean slotWasSwapped;
    private boolean autoClickerWasOn;
    private boolean autoClutchActive;
    private int prevSlot = -1;
    private int plannedSlot = -1;
    private int placedBlocks;
    private float aimYaw;
    private float aimPitch;
    private BlockPos targetPos;
    private EnumFacing targetSide;

    public Clutch() {
        super("Clutch", false);
    }

    @Override
    public void onEnabled() {
        this.clearAll(false);
    }

    @Override
    public void onDisabled() {
        this.clearAll(true);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null || mc.theWorld == null) return;

        this.updateAutoClutch();
        boolean active = this.isKeyDown() || this.autoClutchActive;
        if (mc.currentScreen != null || !active) {
            this.clearAim(true);
            return;
        }

        if (mc.thePlayer.onGround) this.placedBlocks = 0;
        int blockSlot = this.pickBlockSlot();
        if (blockSlot == -1 || !this.canPlaceThrough(this.getFeetBelow())) {
            this.clearAim(true);
            return;
        }

        this.plannedSlot = blockSlot;
        AimResult result = this.findAim(event.getYaw(), event.getPitch());
        if (result == null) {
            this.clearAim(true);
            return;
        }

        this.aimYaw = result.yaw;
        this.aimPitch = result.pitch;
        this.targetPos = result.ray.getBlockPos();
        this.targetSide = result.ray.sideHit;
        this.hasAim = true;
        this.resetting = false;
        this.enablePlacing();
        this.equipPlannedSlot();

        float[] smoothed = this.smooth(event.getYaw(), event.getPitch(), this.aimYaw, this.aimPitch, false);
        event.setRotation(smoothed[0], smoothed[1], 6);

        MovingObjectPosition ray = RotationUtil.rayTrace(smoothed[0], smoothed[1], this.reach.getValue(), 1.0F);
        if (this.placing
                && ray != null
                && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && ray.getBlockPos().equals(this.targetPos)
                && ray.sideHit == this.targetSide
                && (this.maxDistance.getValue() == 0 || this.placedBlocks < this.maxDistance.getValue())) {
            this.place(ray);
        }
    }

    private void updateAutoClutch() {
        if (!this.autoClutch.getValue()) {
            this.autoClutchActive = false;
            return;
        }
        if (mc.thePlayer.onGround) {
            this.autoClutchActive = false;
            return;
        }
        if (mc.thePlayer.motionY < 0.0D
                && this.willFallFar(this.minimumFallDistance.getValue())
                && this.hasRescuePlacement()) {
            this.autoClutchActive = true;
        } else {
            this.autoClutchActive = false;
        }
    }

    private boolean isKeyDown() {
        int key = this.selectKeybind.getValue();
        return key > 0 && Keyboard.isKeyDown(key);
    }

    private BlockPos getFeetBelow() {
        return new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - 1, MathHelper.floor_double(mc.thePlayer.posZ));
    }

    private AimResult findAim(float eventYaw, float eventPitch) {
        Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        Vec3 futurePos = this.simulateFuturePosition.getValue() ? playerPos.addVector(mc.thePlayer.motionX * 6.0D, Math.min(mc.thePlayer.motionY * 6.0D, -1.0D), mc.thePlayer.motionZ * 6.0D) : playerPos;
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        ArrayList<BlockCandidate> candidates = new ArrayList<>();
        int feetX = MathHelper.floor_double(playerPos.xCoord);
        int feetY = MathHelper.floor_double(playerPos.yCoord);
        int feetZ = MathHelper.floor_double(playerPos.zCoord);

        int scanRadius = MathHelper.ceiling_float_int(this.reach.getValue()) + 1;
        int minY = Math.max(0, Math.min(feetY - 1, MathHelper.floor_double(futurePos.yCoord) - 1));

        for (int y = feetY - 1; y >= minY - 2; y--) {
            for (int x = feetX - scanRadius; x <= feetX + scanRadius; x++) {
                for (int z = feetZ - scanRadius; z <= feetZ + scanRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (this.canPlaceThrough(pos)) continue;
                    double score = pos.distanceSq(futurePos.xCoord, futurePos.yCoord, futurePos.zCoord);
                    candidates.add(new BlockCandidate(pos, score));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(c -> c.score));

        for (BlockCandidate candidate : candidates) {
            for (EnumFacing side : EnumFacing.VALUES) {
                if (side == EnumFacing.DOWN) continue;
                BlockPos placeCell = candidate.pos.offset(side);
                if (!this.canPlaceThrough(placeCell)) continue;
                Vec3 hitVec = BlockUtil.getClickVec(candidate.pos, side);
                double dx = hitVec.xCoord - eye.xCoord;
                double dy = hitVec.yCoord - eye.yCoord;
                double dz = hitVec.zCoord - eye.zCoord;
                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, eventYaw, eventPitch);
                MovingObjectPosition ray = RotationUtil.rayTrace(rotations[0], rotations[1], this.reach.getValue(), 1.0F);
                if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
                if (!ray.getBlockPos().equals(candidate.pos) || ray.sideHit != side) continue;
                if (Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - eventYaw)) > this.rotationTolerance.getValue()) continue;
                return new AimResult(ray, rotations[0], rotations[1]);
            }
        }
        return null;
    }

    private void place(MovingObjectPosition ray) {
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), ray.getBlockPos(), ray.sideHit, ray.hitVec)) {
            this.placedBlocks++;
            mc.thePlayer.swingItem();
        }
    }

    private int pickBlockSlot() {
        int current = mc.thePlayer.inventory.currentItem;
        if (this.isBlockSlot(current)) return current;
        for (int slot = 8; slot >= 0; slot--) {
            if (this.isBlockSlot(slot)) return slot;
        }
        return -1;
    }

    private boolean isBlockSlot(int slot) {
        if (slot < 0 || slot > 8) return false;
        ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        return !BlockUtil.isInteractable(block) && BlockUtil.isSolid(block);
    }

    private void enablePlacing() {
        if (this.placing) return;
        this.placing = true;
        this.prevSlot = mc.thePlayer.inventory.currentItem;
        AutoClicker autoClicker = (AutoClicker) Myau.moduleManager.modules.get(AutoClicker.class);
        this.autoClickerWasOn = autoClicker != null && autoClicker.isEnabled();
        if (this.autoClickerWasOn) autoClicker.setEnabled(false);
    }

    private void equipPlannedSlot() {
        if (this.plannedSlot != -1 && this.plannedSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = this.plannedSlot;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(this.plannedSlot));
            this.slotWasSwapped = true;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    private void clearAim(boolean restore) {
        this.hasAim = false;
        this.resetting = false;
        this.placing = false;
        this.targetPos = null;
        this.targetSide = null;
        this.plannedSlot = -1;
        if (restore) this.restoreState();
    }

    private void clearAll(boolean restore) {
        this.autoClutchActive = false;
        this.placedBlocks = 0;
        this.clearAim(restore);
    }

    private void restoreState() {
        if (this.slotWasSwapped && this.prevSlot != -1 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = this.prevSlot;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(this.prevSlot));
        }
        this.slotWasSwapped = false;
        this.prevSlot = -1;
        if (mc.currentScreen == null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), Mouse.isButtonDown(0));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Mouse.isButtonDown(1));
        }
        AutoClicker autoClicker = (AutoClicker) Myau.moduleManager.modules.get(AutoClicker.class);
        if (this.autoClickerWasOn && autoClicker != null) autoClicker.setEnabled(true);
        this.autoClickerWasOn = false;
    }

    private float[] smooth(float currentYaw, float currentPitch, float targetYaw, float targetPitch, boolean snapback) {
        float deltaYaw = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;
        float maxStep = snapback ? this.snapbackSpeed.getValue() : this.speed.getValue();
        float total = Math.abs(deltaYaw) + Math.abs(deltaPitch);
        if (maxStep <= 0 || total <= maxStep) return new float[]{targetYaw, targetPitch};
        float scale = maxStep / total;
        return new float[]{currentYaw + deltaYaw * scale, MathHelper.clamp_float(currentPitch + deltaPitch * scale, -90.0F, 90.0F)};
    }

    private boolean willFallFar(int minFall) {
        return mc.thePlayer.fallDistance >= minFall || mc.thePlayer.posY - (mc.thePlayer.posY + mc.thePlayer.motionY * 12.0D) >= minFall;
    }

    private boolean hasRescuePlacement() {
        if (!this.canPlaceThrough(this.getFeetBelow())) return false;
        return this.findAim(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch) != null;
    }

    private boolean canPlaceThrough(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return material == Material.air || material == Material.water || material == Material.lava || block == Blocks.fire || BlockUtil.isReplaceable(block);
    }

    private static class BlockCandidate {
        private final BlockPos pos;
        private final double score;

        private BlockCandidate(BlockPos pos, double score) {
            this.pos = pos;
            this.score = score;
        }
    }

    private static class AimResult {
        private final MovingObjectPosition ray;
        private final float yaw;
        private final float pitch;

        private AimResult(MovingObjectPosition ray, float yaw, float pitch) {
            this.ray = ray;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
