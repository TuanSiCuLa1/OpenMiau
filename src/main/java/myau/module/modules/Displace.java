package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.RotationUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Displace extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DISPLACE_WINDOW_TICKS = 10;
    private static final int VOID_SCAN_DIRECTIONS = 32;
    private static final int VOID_SCAN_RINGS = 12;
    private static final int VOID_SCAN_DEPTH = 10;
    private static final double VOID_SCAN_STEP = 0.5D;
    private static final double DYNAMIC_SCAN_STEP = 0.5D;
    private static final double DYNAMIC_SCAN_DISTANCE = 6.0D;
    private static final double DYNAMIC_SCAN_SIDE_STEP = 0.45D;
    private static final double DYNAMIC_WALL_CHECK_STEP = 0.25D;
    private static final double DYNAMIC_COLLISION_INSET = 0.03D;
    private static final double[] VOID_SCAN_X = new double[VOID_SCAN_DIRECTIONS];
    private static final double[] VOID_SCAN_Z = new double[VOID_SCAN_DIRECTIONS];

    public final ModeProperty dynamicAngle = new ModeProperty("Dynamic-Angle", 0, new String[]{"Static", "Dynamic"});
    public final FloatProperty yawOffset = new FloatProperty("Yaw-Offset", 90.0F, 0.0F, 180.0F, () -> !this.isDynamicAngle());
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 500);
    public final ModeProperty direction = new ModeProperty("Direction", 0, new String[]{"Left", "Right"}, () -> !this.isDynamicAngle());
    public final BooleanProperty findVoid = new BooleanProperty("Find-Void", false, () -> !this.isDynamicAngle());
    public final BooleanProperty blink = new BooleanProperty("Blink", false);
    public final BooleanProperty hasKnockback = new BooleanProperty("Has-Knockback", false);

    private boolean displaceThisTick;
    private boolean active;
    private boolean hasKB;
    private boolean compensateNextTick;
    private boolean displaceLeft;
    private boolean wasDisplacingLastTick;
    private boolean releaseBlinkNextTick;
    private Float dynamicVoidYaw;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    static {
        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double angle = Math.PI * 2.0D * (double) i / (double) VOID_SCAN_DIRECTIONS;
            VOID_SCAN_X[i] = Math.cos(angle);
            VOID_SCAN_Z[i] = Math.sin(angle);
        }
    }

    public Displace() {
        super("Displace", false);
    }

    @Override
    public void onEnabled() {
        this.displaceThisTick = false;
        this.active = false;
        this.hasKB = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextTick = false;
        this.dynamicVoidYaw = null;
        this.tickCounter = 0;
        this.targetWindowStartTicks.clear();
        this.releaseBlink();
    }

    @Override
    public void onDisabled() {
        this.active = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextTick = false;
        this.dynamicVoidYaw = null;
        this.targetWindowStartTicks.clear();
        this.releaseBlink();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue() + "ms"};
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            this.clearActiveState();
            return;
        }

        if (event.getType() != EventType.PRE) return;
        if (this.releaseBlinkNextTick) {
            this.releaseBlink();
            this.releaseBlinkNextTick = false;
        }

        this.tickCounter++;
        int currentTick = this.tickCounter;
        this.pruneTargetDelayStates();

        if (this.hasKnockback.getValue() && EnchantmentHelper.getKnockbackModifier(mc.thePlayer) <= 0) {
            this.clearActiveState();
            return;
        }

        EntityPlayer target = null;
        boolean attacking = mc.gameSettings.keyBindAttack.isKeyDown()
                || Mouse.isButtonDown(0)
                || (Myau.moduleManager.getModule(KillAura.class) instanceof KillAura && ((KillAura) Myau.moduleManager.getModule(KillAura.class)).isEnabled());
        if (attacking) {
            target = this.findClosestTarget(9.0D);
        }

        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        this.active = target != null && (hasKBEnchant || this.anyMovementKey());
        if (!this.active) {
            this.clearActiveState();
            return;
        }

        this.dynamicVoidYaw = this.isDynamicAngle()
                ? this.findDynamicVoidYaw(target)
                : this.findVoid.getValue() ? this.findStaticVoidYaw(target) : null;
        if (this.dynamicVoidYaw == null && !this.isDynamicAngle()) {
            this.displaceLeft = this.direction.getValue() == 0;
        }

        Float displaceYaw = this.dynamicVoidYaw != null ? this.dynamicVoidYaw : this.isDynamicAngle() ? null : this.getFixedDisplaceYaw();
        if (displaceYaw == null) {
            this.clearActiveState();
            return;
        }

        this.hasKB = hasKBEnchant;
        this.displaceThisTick = !this.displaceThisTick;
        if (this.displaceThisTick && !this.shouldDisplaceInCurrentWindow(target, currentTick)) {
            this.displaceThisTick = false;
            this.compensateNextTick = false;
            this.wasDisplacingLastTick = false;
            this.dynamicVoidYaw = null;
            return;
        }

        if (!this.displaceThisTick && this.wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) {
                KeyBinding.onTick(key);
            }
        }
        this.wasDisplacingLastTick = this.displaceThisTick;

        if (this.compensateNextTick && !this.displaceThisTick) {
            this.compensateNextTick = false;
            mc.thePlayer.movementInput.moveStrafe = this.displaceLeft ? -1.0F : 1.0F;
            return;
        }

        if (!this.displaceThisTick) return;

        if (!this.hasKB && this.anyMovementKey()) {
            mc.thePlayer.movementInput.moveForward = 1.0F;
            this.compensateNextTick = true;
        }

        event.setRotation(displaceYaw, mc.thePlayer.rotationPitch, 0);
        if (this.blink.getValue()) {
            Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
            Myau.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
            this.releaseBlinkNextTick = true;
        }
    }

    private static int msToTicks(int ms) {
        if (ms <= 0) return 0;
        return (int) Math.ceil(ms / 50.0D);
    }

    private boolean anyMovementKey() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private boolean isDynamicAngle() {
        return this.dynamicAngle.getValue() == 1;
    }

    private Float findStaticVoidYaw(EntityPlayer target) {
        if (target == null || mc.thePlayer == null || mc.theWorld == null) return null;
        double bestX = 0.0D;
        double bestZ = 0.0D;
        double bestScore = Double.MAX_VALUE;

        for (int ring = 1; ring <= VOID_SCAN_RINGS; ring++) {
            double radius = (double) ring * VOID_SCAN_STEP;
            boolean foundInRing = false;
            for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
                double x = target.posX + VOID_SCAN_X[i] * radius;
                double z = target.posZ + VOID_SCAN_Z[i] * radius;
                if (!this.isVoidColumn(x, target.posY, z)) continue;

                double playerDx = x - mc.thePlayer.posX;
                double playerDz = z - mc.thePlayer.posZ;
                double playerDistSq = playerDx * playerDx + playerDz * playerDz;
                double score = radius * radius * 1000.0D + playerDistSq;
                if (score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                    foundInRing = true;
                }
            }
            if (foundInRing) break;
        }

        if (bestScore == Double.MAX_VALUE) return null;
        this.updateDisplaceSide(target, bestX, bestZ);

        double dx = bestX - target.posX;
        double dz = bestZ - target.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001D) return null;

        double aimRadius = Math.min(dist, Math.max(0.35D, (double) target.width * 0.5D + 0.15D));
        double aimX = target.posX + dx / dist * aimRadius;
        double aimZ = target.posZ + dz / dist * aimRadius;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        return RotationUtil.getRotationsTo(aimX - eyes.xCoord, target.posY + (double) target.getEyeHeight() * 0.5D - eyes.yCoord, aimZ - eyes.zCoord, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)[0];
    }

    private Float findDynamicVoidYaw(EntityPlayer target) {
        if (target == null || mc.thePlayer == null || mc.theWorld == null) return null;
        double bestForwardX = 0.0D;
        double bestForwardZ = 0.0D;
        double bestScore = 0.0D;

        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double forwardX = VOID_SCAN_X[i];
            double forwardZ = VOID_SCAN_Z[i];
            double score = this.scoreVoidPath(target, forwardX, forwardZ);
            if (score > bestScore) {
                bestScore = score;
                bestForwardX = forwardX;
                bestForwardZ = forwardZ;
            }
        }

        if (bestScore <= 0.0D) return null;
        this.updateDisplaceSide(target, target.posX + bestForwardX, target.posZ + bestForwardZ);
        return this.yawFromForward(bestForwardX, bestForwardZ);
    }

    private float yawFromForward(double forwardX, double forwardZ) {
        return (float) (Math.toDegrees(Math.atan2(forwardZ, forwardX)) - 90.0D);
    }

    private double scoreVoidPath(EntityPlayer target, double forwardX, double forwardZ) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double score = 0.0D;
        double checkedForward = 0.0D;
        int consecutiveCenterVoid = 0;
        AxisAlignedBB baseCollisionBox = target.getEntityBoundingBox().contract(DYNAMIC_COLLISION_INSET, 0.0D, DYNAMIC_COLLISION_INSET);

        for (int step = 1; step <= (int) (DYNAMIC_SCAN_DISTANCE / DYNAMIC_SCAN_STEP); step++) {
            double forward = (double) step * DYNAMIC_SCAN_STEP;
            if (!this.isDynamicPathClear(target, baseCollisionBox, forwardX, forwardZ, checkedForward, forward)) break;
            checkedForward = forward;
            boolean centerVoid = false;

            for (int side = -1; side <= 1; side++) {
                double sideOffset = (double) side * DYNAMIC_SCAN_SIDE_STEP;
                double x = target.posX + forwardX * forward + sideX * sideOffset;
                double z = target.posZ + forwardZ * forward + sideZ * sideOffset;
                if (this.isVoidColumn(x, target.posY, z)) {
                    double laneWeight = side == 0 ? 1.4D : 1.0D;
                    score += laneWeight * (DYNAMIC_SCAN_DISTANCE + DYNAMIC_SCAN_STEP - forward);
                    centerVoid |= side == 0;
                }
            }

            if (centerVoid) {
                consecutiveCenterVoid++;
                score += consecutiveCenterVoid * 2.0D;
            } else {
                consecutiveCenterVoid = 0;
            }
        }
        return score;
    }

    private boolean isDynamicPathClear(EntityPlayer target, AxisAlignedBB baseCollisionBox, double forwardX, double forwardZ, double fromForward, double toForward) {
        for (double forward = fromForward + DYNAMIC_WALL_CHECK_STEP; forward <= toForward + 1.0E-4D; forward += DYNAMIC_WALL_CHECK_STEP) {
            AxisAlignedBB checkBox = baseCollisionBox.offset(forwardX * forward, 0.0D, forwardZ * forward);
            if (this.hasBlockCollision(target, checkBox)) return false;
        }
        return true;
    }

    private boolean hasBlockCollision(EntityPlayer target, AxisAlignedBB box) {
        int minX = MathHelper.floor_double(box.minX);
        int maxX = MathHelper.floor_double(box.maxX + 1.0D);
        int minY = MathHelper.floor_double(box.minY);
        int maxY = MathHelper.floor_double(box.maxY + 1.0D);
        int minZ = MathHelper.floor_double(box.minZ);
        int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

        List<AxisAlignedBB> collisions = new ArrayList<>();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int blockX = minX; blockX < maxX; blockX++) {
            for (int blockZ = minZ; blockZ < maxZ; blockZ++) {
                if (!mc.theWorld.isBlockLoaded(blockPos.set(blockX, 64, blockZ))) return true;
                for (int blockY = minY; blockY < maxY; blockY++) {
                    if (blockY < 0 || blockY >= 256) return true;
                    blockPos.set(blockX, blockY, blockZ);
                    IBlockState state = mc.theWorld.getBlockState(blockPos);
                    state.getBlock().addCollisionBoxesToList(mc.theWorld, blockPos, state, box, collisions, target);
                    if (!collisions.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    private boolean isVoidColumn(double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockZ = MathHelper.floor_double(z);
        int startY = MathHelper.floor_double(y) - 1;
        int endY = Math.max(0, startY - VOID_SCAN_DEPTH);
        for (int blockY = startY; blockY >= endY; blockY--) {
            if (!mc.theWorld.isAirBlock(new BlockPos(blockX, blockY, blockZ))) return false;
        }
        return true;
    }

    private void updateDisplaceSide(EntityPlayer target, double voidX, double voidZ) {
        double targetDx = target.posX - mc.thePlayer.posX;
        double targetDz = target.posZ - mc.thePlayer.posZ;
        double voidDx = voidX - mc.thePlayer.posX;
        double voidDz = voidZ - mc.thePlayer.posZ;
        double cross = targetDx * voidDz - targetDz * voidDx;
        this.displaceLeft = cross < 0.0D;
    }

    private float getFixedDisplaceYaw() {
        float baseYaw = mc.thePlayer.rotationYaw;
        float offset = this.yawOffset.getValue();
        return this.displaceLeft ? baseYaw - offset : baseYaw + offset;
    }

    private void clearActiveState() {
        this.active = false;
        this.displaceThisTick = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.dynamicVoidYaw = null;
    }

    private void pruneTargetDelayStates() {
        if (mc.theWorld == null) {
            this.targetWindowStartTicks.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = this.targetWindowStartTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            Entity entity = mc.theWorld.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityPlayer) || entity.isDead || ((EntityPlayer) entity).deathTime != 0) {
                iterator.remove();
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) return true;
        int targetId = target.getEntityId();
        Integer windowStartTick = this.targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            this.targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }

        int delayTicks = msToTicks(this.delay.getValue());
        if (delayTicks <= 0) return true;
        int elapsed = currentTick - windowStartTick;
        return elapsed >= delayTicks;
    }

    private void releaseBlink() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
    }

    private EntityPlayer findClosestTarget(double range) {
        EntityPlayer best = null;
        double bestDist = range * range;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || player.deathTime != 0 || player.getHealth() <= 0.0F) continue;
            double dist = mc.thePlayer.getDistanceSqToEntity(player);
            if (dist <= bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }
}
