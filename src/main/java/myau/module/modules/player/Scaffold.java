package myau.module.modules.player;

import myau.module.modules.movement.LongJump;
import myau.module.modules.render.HUD;
import myau.Myau;
import myau.component.SlotComponent;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.player.MoveUtil;
import myau.util.client.KeyBindUtil;
import myau.util.player.PlayerUtil;
import myau.util.math.RandomUtil;
import myau.util.world.BlockUtil;
import myau.util.player.ItemUtil;
import myau.util.network.PacketUtil;
import myau.util.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

import myau.util.shader.RoundedUtils;
import net.minecraft.client.renderer.RenderHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Scaffold extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double[] placeOffsets = new double[] {
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };
    private int rotationTick = 0;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    public final ModeProperty bridgeMode = new ModeProperty("bridge-mode", 0, 
            new String[] { "NONE", "NORMAL", "GODBRIDGE", "BREESILY", "SNAP", "TELLY", "EAGLE" });
    public final ModeProperty rotationMode = new ModeProperty("rotations", 2,
            new String[] { "NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS" });
    public final ModeProperty rayCast = new ModeProperty("ray-cast", 1, new String[] { "OFF", "STRICT" });
    public final ModeProperty sprintMode = new ModeProperty("sprint", 0, new String[] { "NONE", "VANILLA", "WATCHDOG_JUMP", "WATCHDOG_FAST", "MATRIX", "BYPASS", "LEGIT" });
    public final BooleanProperty jumpSprint = new BooleanProperty("jump-sprint", true,
            () -> this.sprintMode.getValue() != 0);
    public final BooleanProperty diaSprint = new BooleanProperty("dia-sprint", true,
            () -> this.sprintMode.getValue() != 0);
    public final ModeProperty tower = new ModeProperty("tower", 0,
            new String[] { "NONE", "VANILLA", "EXTRA", "TELLY", "WATCHDOG", "MATRIX", "NCP", "VULCAN" });
    public final ModeProperty keepY = new ModeProperty("keep-y", 0,
            new String[] { "NONE", "VANILLA", "EXTRA" });
    public final BooleanProperty keepYonPress = new BooleanProperty("keep-y-on-press", false,
            () -> this.keepY.getValue() != 0);
    public final BooleanProperty disableWhileJumpActive = new BooleanProperty("no-keep-y-on-jump-potion", false,
            () -> this.keepY.getValue() != 0);
    public final PercentProperty rotationSpeed = new PercentProperty("rotation-speed", 100, () -> this.bridgeMode.getValue() != 0);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[] { "NONE", "SILENT" });
    public final BooleanProperty safeWalk = new BooleanProperty("safe-walk", true);
    public final BooleanProperty multiplace = new BooleanProperty("multi-place", true);
    public final BooleanProperty blockCounter = new BooleanProperty("block-counter", true);
    public final PercentProperty groundMotion = new PercentProperty("ground-motion", 100);
    public final PercentProperty airMotion = new PercentProperty("air-motion", 100);
    public final PercentProperty speedMotion = new PercentProperty("speed-motion", 100);
    private float animationProgress = 0f;
    private long lastFrame = System.currentTimeMillis();
    private int recursions = 0, recursion = 0;
    private int sneakingTicks = -1;
    private int pause = 0;
    private float targetYaw = -180.0F;
    private float targetPitch = 0.0F;
    private float yawDrift = 0.0F;
    private float pitchDrift = 0.0F;
    private int directionalChange = 0;
    private int ticksOnAir = 0;
    private int ticksOnGround = 0;
    private int placements = 0;
    private int slow = 0;

    private boolean shouldStopSprint() {
        if (this.isTowering()) {
            return false;
        } else {
            boolean stage = this.keepY.getValue() == 1 || this.keepY.getValue() == 2;
            if (stage && this.stage > 0) {
                return false;
            }
            if (this.sprintMode.getValue() == 0) {
                return true;
            }
            if (this.sprintMode.getValue() == 2) { // WATCHDOG_JUMP
                float yaw = mc.thePlayer.rotationYaw;
                float wrappedYaw = MathHelper.wrapAngleTo180_float(yaw);
                boolean closeToMultipleOf90 = (Math.abs(wrappedYaw % 90) <= 10 || Math.abs(wrappedYaw % 90) >= 80);
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && this.ticksOnGround > 4 && closeToMultipleOf90) {
                    return false;
                }
            }
            return mc.thePlayer.onGround ? !this.jumpSprint.getValue() : !(this.diaSprint.getValue() && this.isDiagonal(this.getCurrentYaw()));
        }
    }

    private boolean canPlace() {
        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isReady()) {
            return false;
        } else {
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            return !longJump.isEnabled() || !longJump.isAutoMode() || longJump.isJumping();
        }
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5,
                            (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ));
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        } else {
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!BlockUtil.isReplaceable(pos)
                                && !BlockUtil.isInteractable(pos)
                                && !(mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5,
                                        (double) pos.getZ()
                                                + 0.5) > (double) mc.playerController.getBlockReachDistance())
                                && (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (BlockUtil.isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5,
                                        (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)));
                BlockPos blockPos = positions.get(0);
                EnumFacing facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        // Use SlotComponent to get item from the active (possibly alternative) slot
        ItemStack activeItem = Myau.slotComponent.getItemStack();
        if (activeItem != null && ItemUtil.isBlock(activeItem)) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    activeItem, blockPos, enumFacing, vec3)) {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }
        }
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            case WEST:
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    private float getSpeed() {
        if (!mc.thePlayer.onGround) {
            return (float) this.airMotion.getValue() / 100.0F;
        } else {
            return MoveUtil.getSpeedLevel() > 0
                    ? (float) this.speedMotion.getValue() / 100.0F
                    : (float) this.groundMotion.getValue() / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return MoveUtil.adjustYaw(
                mc.thePlayer.rotationYaw, (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue());
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && !PlayerUtil.isAirAbove()) {
            boolean keepY = this.bridgeMode.getValue() == 5;
            boolean tower = this.tower.getValue() == 3;
            return keepY && this.stage > 0 || tower && mc.gameSettings.keyBindJump.isKeyDown();
        } else {
            return false;
        }
    }

    public Scaffold() {
        super("Scaffold", false);
    }

    public int getSlot() {
        return Myau.slotComponent.getItemIndex();
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!mc.thePlayer.onGround) {
            this.ticksOnAir++;
            this.ticksOnGround = 0;
        } else {
            this.ticksOnGround++;
            this.ticksOnAir = 0;
        }
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }
            if (mc.thePlayer.onGround) {
                if (this.stage > 0) {
                    this.stage--;
                }
                if (this.stage < 0) {
                    this.stage++;
                }
                if (this.stage == 0
                        && (this.keepY.getValue() != 0 || this.bridgeMode.getValue() == 5)
                        && (!(Boolean) this.keepYonPress.getValue() || PlayerUtil.isUsingItem())
                        && (!this.disableWhileJumpActive.getValue() || !mc.thePlayer.isPotionActive(Potion.jump))
                        && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    this.stage = 1;
                }
                this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
                this.shouldKeepY = false;
                this.towering = false;
            }
            if (this.canPlace()) {
                // Switch silently via SlotComponent to a valid block in hotbar
                int blockSlot = this.findBlock();
                if (blockSlot != -1) {
                    Myau.slotComponent.setSlot(blockSlot);
                }
                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = this.isDiagonal(currentYaw)
                        ? yawDiffTo180
                        : RotationUtil.wrapAngleDiff(
                                currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F),
                                event.getYaw());
                if (!this.canRotate) {
                    switch (this.rotationMode.getValue()) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            }
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                    }
                }
                BlockData blockData = this.getBlockData();
                Vec3 hitVec = null;
                if (blockData != null) {
                    double[] x = placeOffsets;
                    double[] y = placeOffsets;
                    double[] z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH:
                            z = new double[] { 0.0 };
                            break;
                        case EAST:
                            x = new double[] { 1.0 };
                            break;
                        case SOUTH:
                            z = new double[] { 1.0 };
                            break;
                        case WEST:
                            x = new double[] { 0.0 };
                            break;
                        case DOWN:
                            y = new double[] { 0.0 };
                            break;
                        case UP:
                            y = new double[] { 1.0 };
                    }
                    float bestYaw = -180.0F;
                    float bestPitch = 0.0F;
                    float bestDiff = 0.0F;
                    for (double dx : x) {
                        for (double dy : y) {
                            for (double dz : z) {
                                double relX = (double) blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                                double relY = (double) blockData.blockPos().getY() + dy - mc.thePlayer.posY
                                        - (double) mc.thePlayer.getEyeHeight();
                                double relZ = (double) blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                                float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                                float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1],
                                        mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop != null
                                        && mop.typeOfHit == MovingObjectType.BLOCK
                                        && mop.getBlockPos().equals(blockData.blockPos())
                                        && mop.sideHit == blockData.facing()) {
                                    float totalDiff = Math.abs(rotations[0] - baseYaw)
                                            + Math.abs(rotations[1] - this.pitch);
                                    if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                        bestYaw = rotations[0];
                                        bestPitch = rotations[1];
                                        bestDiff = totalDiff;
                                        hitVec = mop.hitVec;
                                    }
                                }
                            }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        this.yaw = bestYaw;
                        this.pitch = bestPitch;
                        this.canRotate = true;
                    }
                }
                if (this.canRotate && MoveUtil.isForwardPressed()
                        && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - this.yaw)) < 90.0F) {
                    switch (this.rotationMode.getValue()) {
                        case 2:
                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                    }
                }
                if (this.bridgeMode.getValue() != 0) {
                    this.calculateRotations(event, blockData, currentYaw, yawDiffTo180, diagonalYaw);
                    event.setRotation(this.yaw, this.pitch, 3);
                    if (this.moveFix.getValue() == 1) {
                        event.setPervRotation(this.yaw, 3);
                    }
                    this.calculateSneaking();
                } else if (this.rotationMode.getValue() != 0) {
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if (this.towering
                            && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (double) (this.startY + 1))) {
                        float yawDiff = MathHelper.wrapAngleTo180_float(this.yaw - event.getYaw());
                        float tolerance = this.rotationTick >= 2 ? RandomUtil.nextFloat(90.0F, 95.0F)
                                : RandomUtil.nextFloat(30.0F, 35.0F);
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                            targetYaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                            this.rotationTick = Math.max(this.rotationTick, 1);
                        }
                    }
                    if (this.isTowering()) {
                        float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getYaw());
                        targetYaw = RotationUtil
                                .quantizeAngle(event.getYaw() + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F));
                        targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
                        this.rotationTick = 3;
                        this.towering = true;
                    }
                    event.setRotation(targetYaw, targetPitch, 3);
                    if (this.moveFix.getValue() == 1) {
                        event.setPervRotation(targetYaw, 3);
                    }
                }
                if (blockData != null && hitVec != null && this.rotationTick <= 0) {
                    this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    if (this.multiplace.getValue()) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }
                            MovingObjectPosition mop = RotationUtil.rayTrace(this.yaw, this.pitch,
                                    mc.playerController.getBlockReachDistance(), 1.0F);
                            if (mop != null
                                    && mop.typeOfHit == MovingObjectType.BLOCK
                                    && mop.getBlockPos().equals(blockData.blockPos())
                                    && mop.sideHit == blockData.facing()) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            } else {
                                hitVec = BlockUtil.getClickVec(blockData.blockPos(), blockData.facing());
                                double dx = hitVec.xCoord - mc.thePlayer.posX;
                                double dy = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double dz = hitVec.zCoord - mc.thePlayer.posZ;
                                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(),
                                        event.getPitch());
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F)
                                        || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }
                                mop = RotationUtil.rayTrace(rotations[0], rotations[1],
                                        mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop == null
                                        || mop.typeOfHit != MovingObjectType.BLOCK
                                        || !mop.getBlockPos().equals(blockData.blockPos())
                                        || mop.sideHit != blockData.facing()) {
                                    break;
                                }
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            }
                        }
                    }
                }
                if (this.targetFacing != null) {
                    if (this.rotationTick <= 0) {
                        int playerBlockX = MathHelper.floor_double(mc.thePlayer.posX);
                        int playerBlockY = MathHelper.floor_double(mc.thePlayer.posY);
                        int playerBlockZ = MathHelper.floor_double(mc.thePlayer.posZ);
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }
                    this.targetFacing = null;
                } else if (this.keepY.getValue() == 2 && this.stage > 0 && !mc.thePlayer.onGround) {
                    int nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
                    if (nextBlockY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if (blockData != null && this.rotationTick <= 0) {
                            hitVec = BlockUtil.getHitVec(blockData.blockPos(), blockData.facing(), this.yaw,
                                    this.pitch);
                            this.place(blockData.blockPos(), blockData.facing(), hitVec);
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (!mc.thePlayer.isCollidedHorizontally
                    && mc.thePlayer.hurtTime <= 5
                    && !mc.thePlayer.isPotionActive(Potion.jump)
                    && mc.gameSettings.keyBindJump.isKeyDown()
                    && ItemUtil.isHoldingBlock()) {
                int yState = (int) (mc.thePlayer.posY % 1.0 * 100.0);
                switch (this.tower.getValue()) {
                    case 1:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    this.towerTick = 2;
                                    mc.thePlayer.motionY = 0.42F;
                                    if (MoveUtil.isForwardPressed()) {
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                    } else {
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                    }
                                    return;
                                } else {
                                    this.towerTick = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0;
                                return;
                            case 3:
                                this.towerTick = 1;
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                return;
                            default:
                                this.towerTick = 0;
                                return;
                        }
                    case 2:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    if (!MoveUtil.isForwardPressed()) {
                                        this.towerDelay = 2;
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                        EnumFacing facing = this
                                                .yawToFacing(MathHelper.wrapAngleTo180_float(this.yaw - 180.0F));
                                        double distance = this.distanceToEdge(facing);
                                        if (distance > 0.1) {
                                            if (mc.thePlayer.onGround) {
                                                Vec3i directionVec = facing.getDirectionVec();
                                                double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                                double jitter = RandomUtil.nextDouble(0.02, 0.03);
                                                AxisAlignedBB nextBox = mc.thePlayer
                                                        .getEntityBoundingBox()
                                                        .offset((double) directionVec.getX() * (offset - jitter), 0.0,
                                                                (double) directionVec.getZ() * (offset - jitter));
                                                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox)
                                                        .isEmpty()) {
                                                    mc.thePlayer.motionY = -0.0784000015258789;
                                                    mc.thePlayer
                                                            .setPosition(
                                                                    nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0,
                                                                    nextBox.minY,
                                                                    nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0);
                                                }
                                                return;
                                            }
                                        } else {
                                            this.towerTick = 2;
                                            this.targetFacing = facing;
                                            mc.thePlayer.motionY = 0.42F;
                                        }
                                        return;
                                    } else {
                                        this.towerTick = 2;
                                        this.towerDelay++;
                                        mc.thePlayer.motionY = 0.42F;
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                        return;
                                    }
                                } else {
                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = mc.thePlayer.motionY - RandomUtil.nextDouble(0.00101, 0.00109);
                                return;
                            case 3:
                                if (this.towerDelay >= 4) {
                                    this.towerTick = 4;
                                    this.towerDelay = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                }
                                return;
                            case 4:
                                this.towerTick = 5;
                                return;
                            case 5:
                                if (!PlayerUtil.isAirBelow()) {
                                    this.towerTick = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                }
                                return;
                            default:
                                this.towerTick = 0;
                                this.towerDelay = 0;
                                return;
                        }
                    case 3: // WATCHDOG
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.motionY = 0.41999998688698;
                            this.towerTick = 0;
                        } else if (this.towerTick == 0 && mc.thePlayer.motionY < 0.23 && mc.thePlayer.motionY > 0.15) {
                            this.towerTick = 1;
                        } else if (this.towerTick == 1 && mc.thePlayer.motionY < 0.05) {
                            mc.thePlayer.motionY = -mc.thePlayer.posY % 1;
                            this.towerTick = 0;
                        }
                        return;
                    case 4: // MATRIX
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        } else {
                            if (mc.thePlayer.motionY < 0 && mc.thePlayer.fallDistance > 1) {
                                mc.thePlayer.motionY = -1;
                            }
                        }
                        return;
                    default:
                        this.towerTick = 0;
                        this.towerDelay = 0;
                }
            } else {
                this.towerTick = 0;
                this.towerDelay = 0;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 3.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
            if (mc.thePlayer.onGround && this.stage > 0 && MoveUtil.isForwardPressed()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            float speed = this.getSpeed();
            if (speed != 1.0F) {
                if (mc.thePlayer.movementInput.moveForward != 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                    mc.thePlayer.movementInput.moveForward = mc.thePlayer.movementInput.moveForward
                            * (1.0F - Math.max(0.0F, Math.min(1.0F, speed + 0.15F)));
                    mc.thePlayer.movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe * speed;
                } else {
                    mc.thePlayer.movementInput.moveForward = mc.thePlayer.movementInput.moveForward * speed;
                    mc.thePlayer.movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe * speed;
                }
            }
            
            if (this.sprintMode.getValue() == 2) { // WATCHDOG_JUMP
                if (this.ticksOnGround > 1) {
                    if (!mc.gameSettings.keyBindJump.isKeyDown()) {
                        MoveUtil.setSpeed(0.0);
                    }
                    if (this.ticksOnGround > 10) {
                        float yaw = mc.thePlayer.rotationYaw;
                        float wrappedYaw = MathHelper.wrapAngleTo180_float(yaw);
                        boolean closeToMultipleOf90 = (Math.abs(wrappedYaw % 90) <= 10 || Math.abs(wrappedYaw % 90) >= 80);
                        if (!closeToMultipleOf90) {
                            mc.thePlayer.motionX *= .9895;
                            mc.thePlayer.motionZ *= .9895;
                        }
                    }
                }
            }
            if (this.shouldStopSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.safeWalk.getValue()) {
            if (mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0
                    && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (mc.thePlayer == null) return;

        long currentFrame = System.currentTimeMillis();
        float delta = (currentFrame - lastFrame) / 1000f;
        lastFrame = currentFrame;

        boolean shouldShow = this.isEnabled() && this.blockCounter.getValue();

        float target = shouldShow ? 1f : 0f;
        animationProgress += (target - animationProgress) * 12f * delta;
        animationProgress = Math.max(0f, Math.min(1f, animationProgress));

        if (animationProgress <= 0.01f) return;

        ItemStack itemStack = null;
        int count = 0;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof ItemBlock) {
            itemStack = held;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) {
                Item item = stack.getItem();
                if (item instanceof ItemBlock) {
                    Block block = ((ItemBlock) item).getBlock();
                    if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                        count += stack.stackSize;
                        if (itemStack == null) {
                            itemStack = stack;
                        }
                    }
                }
            }
        }

        if (itemStack == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        String amount = String.valueOf(count);
        String info = "Slot: " + (mc.thePlayer.inventory.currentItem + 1) + " | Blocks: " + amount;

        float textWidth = myau.util.font.Fonts.MAIN.get(18).width(info);
        float width = 16f + 8f + textWidth + 8f;
        float height = 22f;
        float x = (sr.getScaledWidth() - width) / 2f;
        float y = sr.getScaledHeight() - 90f;

        GlStateManager.pushMatrix();

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(animationProgress, animationProgress, 1f);
        GlStateManager.translate(-centerX, -centerY, 0);

        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        boolean shaders = hud != null && hud.shaders.getValue();

        if (shaders) {
            int blurP = hud.blurPasses.getValue();
            float blurR = hud.blurRadius.getValue();
            int bloomP = hud.bloomPasses.getValue();
            float bloomR = hud.bloomRadius.getValue();

            // Blur pass
            myau.util.shader.RenderSystem.renderBlur(blurR, blurP, () -> {
                RoundedUtils.drawRound(x, y, width, height, 4f, new Color(0, 0, 0, 150));
            });

            // Bloom pass
            myau.util.shader.RenderSystem.renderBloom(bloomR, bloomP, () -> {
                RoundedUtils.drawRound(x - 1, y - 1, width + 2, height + 2, 4f, new Color(81, 99, 149, 80));
            });
        }

        int bgAlpha = (int) (150 * animationProgress);
        RoundedUtils.drawRound(x, y, width, height, 4f, new Color(0, 0, 0, bgAlpha));

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, (int) x + 4, (int) y + 3);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
        int textAlpha = (int) (255 * animationProgress);
        float fontY = y + (height / 2f) - (myau.util.font.Fonts.MAIN.get(18).height() / 2f);
        float textX = x + 24f;

        myau.util.font.Fonts.MAIN.get(18).drawWithShadow(info, textX, fontY, new Color(200, 200, 200, textAlpha).getRGB());

        GlStateManager.popMatrix();
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    private void calculateSneaking() {
        if (this.bridgeMode.getValue() == 0) return;
        
        switch (this.bridgeMode.getValue()) {
            case 6: // EAGLE
                if (this.ticksOnAir >= 4 && MoveUtil.isForwardPressed()) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                }
                if (this.ticksOnGround == 1) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                }
                break;
            case 5: // TELLY
            case 3: // BREESILY
            case 2: // GODBRIDGE
                if (mc.thePlayer.onGround) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                }
                break;
        }
    }

    private void calculateRotations(UpdateEvent event, BlockData blockData, float currentYaw, float yawDiffTo180, float diagonalYaw) {
        if (this.bridgeMode.getValue() == 0) return;
        
        float rotationSpeedVal = this.rotationSpeed.getValue();
        float targetYawLocal = this.yaw;
        float targetPitchLocal = this.pitch;

        switch (this.bridgeMode.getValue()) {
            case 1: // NORMAL
                if (blockData != null && this.canPlace()) {
                    targetYawLocal = this.yaw;
                    targetPitchLocal = this.pitch;
                }
                break;

            case 3: // BREESILY
                if (blockData != null && this.canPlace()) {
                    if (blockData.facing() == EnumFacing.UP) {
                        targetPitchLocal = 90.0F;
                    } else {
                        double staticYaw = (Math.toDegrees(Math.atan2(blockData.facing().getFrontOffsetZ(), blockData.facing().getFrontOffsetX())) % 360) - 90;
                        double staticPitch = 80.0;
                        targetYawLocal = (float) staticYaw + this.yawDrift;
                        targetPitchLocal = (float) staticPitch + this.pitchDrift;
                    }
                } else if (Math.random() > 0.99 || targetPitchLocal % 90 == 0) {
                    this.yawDrift = (float) (Math.random() - 0.5);
                    this.pitchDrift = (float) (Math.random() - 0.5);
                }
                break;

            case 4: // SNAP
                if (this.ticksOnAir <= 0) {
                    targetYawLocal = (float) (Math.toDegrees(MoveUtil.getForwardValue() != 0 || MoveUtil.getLeftValue() != 0 ? 
                            Math.atan2(MoveUtil.getLeftValue(), MoveUtil.getForwardValue()) : 0)) - 180.0F;
                }
                break;

            case 6: // EAGLE
                float eagleYaw = (mc.thePlayer.rotationYaw + 10000000) % 360;
                float staticYaw = (eagleYaw - 180) - (eagleYaw % 90) + 45;
                float staticPitch = 78.0F;
                targetYawLocal = staticYaw + this.yawDrift / 2.0F;
                targetPitchLocal = staticPitch + this.pitchDrift / 2.0F;
                break;

            case 5: // TELLY
                if (this.recursion == 0) {
                    int time = this.ticksOnAir;
                    if (time >= 3 && this.ticksOnAir <= 7) {
                        targetYawLocal = this.yaw;
                        targetPitchLocal = this.pitch;
                    } else {
                        targetYawLocal = mc.thePlayer.rotationYaw;
                    }
                }
                break;

            case 2: // GODBRIDGE
                targetYawLocal = (mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw % 90) - 180 + 45 * (mc.thePlayer.rotationYaw > 0 ? 1 : -1);
                targetPitchLocal = 76.4F;

                this.directionalChange++;
                if (Math.abs(MathHelper.wrapAngleTo180_double(targetYawLocal - this.yaw)) > 10) {
                    this.directionalChange = (int) (Math.random() * 4);
                    this.yawDrift = (float) (Math.random() - 0.5F) / 10.0F;
                    this.pitchDrift = (float) (Math.random() - 0.5F) / 10.0F;
                }

                if (Math.random() > 0.99) {
                    this.yawDrift = (float) (Math.random() - 0.5F) / 10.0F;
                    this.pitchDrift = (float) (Math.random() - 0.5F) / 10.0F;
                }

                if (this.directionalChange <= 10) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                } else if (this.directionalChange == 11) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                }

                targetYawLocal += this.yawDrift;
                targetPitchLocal += this.pitchDrift;
                break;
        }

        if (rotationSpeedVal > 0) {
            float[] smoothed = RotationUtil.smooth(new float[]{this.yaw, this.pitch}, new float[]{targetYawLocal, targetPitchLocal}, rotationSpeedVal + Math.random(), null, 0);
            this.yaw = smoothed[0];
            this.pitch = smoothed[1];
        } else {
            this.yaw = targetYawLocal;
            this.pitch = targetPitchLocal;
        }
    }

    @Override
    public void onEnabled() {
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
    }

    @Override
    public void onDisabled() {
    }

    private int findBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0 && ItemUtil.isBlock(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }
}