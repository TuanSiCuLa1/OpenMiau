package myau.module.modules.combat;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.player.MoveUtil;
import myau.util.player.TeamUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

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
    private static final long ARROW_FADE_MS = 250L;
    private static final double ARROW_FORWARD_GAP = 0.24D;
    private static final double ARROW_BODY_LENGTH = 0.74D;
    private static final double ARROW_BODY_HALF_HEIGHT = 0.08D;
    private static final double ARROW_HEAD_BACKSET = 0.18D;
    private static final double ARROW_HEAD_LENGTH = 0.52D;
    private static final double ARROW_HEAD_HALF_HEIGHT = 0.30D;
    private static final double[] VOID_SCAN_X = new double[VOID_SCAN_DIRECTIONS];
    private static final double[] VOID_SCAN_Z = new double[VOID_SCAN_DIRECTIONS];

    static {
        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double angle = Math.PI * 2.0D * (double) i / (double) VOID_SCAN_DIRECTIONS;
            VOID_SCAN_X[i] = Math.cos(angle);
            VOID_SCAN_Z[i] = Math.sin(angle);
        }
    }

    public final ModeProperty dynamicAngle = new ModeProperty("dynamic-angle", 0, new String[]{"STATIC", "DYNAMIC"});
    public final FloatProperty yawOffset = new FloatProperty("yaw-offset", 90.0F, 0.0F, 180.0F, () -> this.dynamicAngle.getValue() == 0);
    public final FloatProperty delay = new FloatProperty("delay-ms", 0.0F, 0.0F, 500.0F);
    public final ModeProperty direction = new ModeProperty("direction", 0, new String[]{"LEFT", "RIGHT"}, () -> this.dynamicAngle.getValue() == 0);
    public final BooleanProperty showDirection = new BooleanProperty("show-direction", true);
    public final BooleanProperty findVoid = new BooleanProperty("find-void", false, () -> this.dynamicAngle.getValue() == 0);
    public final BooleanProperty blink = new BooleanProperty("blink", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);
    public final BooleanProperty hasKnockback = new BooleanProperty("has-knockback", false);

    private boolean displaceThisTick;
    private boolean active;
    private boolean hasKB;
    private boolean compensateNextTick;
    private boolean displaceLeft;
    private boolean wasDisplacingLastTick;
    private boolean releaseBlinkNextTick;
    private Float renderDisplaceYaw;
    private EntityPlayer renderTarget;
    private Float fadingDisplaceYaw;
    private EntityPlayer fadingTarget;
    private long arrowFadeStartMs;
    private Float lastRenderedDisplaceYaw;
    private EntityPlayer lastRenderedTarget;
    private long lastRenderedArrowMs;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    public Displace() {
        super("Displace", false);
    }

    @Override
    public void onEnabled() {
        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
        this.releaseBlink();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{Math.round(this.delay.getValue()) + "ms"};
    }

    private void resetState() {
        this.displaceThisTick = false;
        this.active = false;
        this.hasKB = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextTick = false;
        this.renderDisplaceYaw = null;
        this.renderTarget = null;
        this.clearArrowState();
        this.tickCounter = 0;
        this.targetWindowStartTicks.clear();
    }

    private void releaseBlink() {
        if (Myau.blinkManager != null && Myau.blinkManager.getBlinkingModule() == BlinkModules.DISPLACE) {
            Myau.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
        }
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

    private static int msToTicks(double ms) {
        return ms <= 0.0D ? 0 : (int) Math.ceil(ms / 50.0D);
    }

    private EntityPlayer findClosestTarget(double range) {
        EntityPlayer best = null;
        double bestDist = range * range;
        for (Object object : mc.theWorld.playerEntities) {
            if (!(object instanceof EntityPlayer)) {
                continue;
            }
            EntityPlayer player = (EntityPlayer) object;
            if (player == mc.thePlayer || player.isDead || player.deathTime != 0) {
                continue;
            }
            if (this.ignoreTeammates.getValue() && TeamUtil.isSameTeam(player)) {
                continue;
            }
            if (TeamUtil.isBot(player) || TeamUtil.isFriend(player)) {
                continue;
            }
            double dist = mc.thePlayer.getDistanceSqToEntity(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private Float findStaticVoidYaw(EntityPlayer target) {
        double bestX = 0.0D;
        double bestZ = 0.0D;
        double bestScore = Double.MAX_VALUE;
        for (int ring = 1; ring <= VOID_SCAN_RINGS; ring++) {
            double radius = (double) ring * VOID_SCAN_STEP;
            boolean foundInRing = false;
            for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
                double x = target.posX + VOID_SCAN_X[i] * radius;
                double z = target.posZ + VOID_SCAN_Z[i] * radius;
                if (!this.isVoidColumn(x, target.posY, z)) {
                    continue;
                }
                double dx = x - mc.thePlayer.posX;
                double dz = z - mc.thePlayer.posZ;
                double score = radius * radius * 1000.0D + dx * dx + dz * dz;
                if (score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                    foundInRing = true;
                }
            }
            if (foundInRing) {
                break;
            }
        }
        if (bestScore == Double.MAX_VALUE) {
            return null;
        }
        this.updateDisplaceSide(target, bestX, bestZ);
        return this.yawTo(target.posX, target.posY + target.getEyeHeight() * 0.5D, target.posZ, bestX, target.posY + target.getEyeHeight() * 0.5D, bestZ);
    }

    private Float findDynamicVoidYaw(EntityPlayer target) {
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
        if (bestScore <= 0.0D) {
            return null;
        }
        this.updateDisplaceSide(target, target.posX + bestForwardX, target.posZ + bestForwardZ);
        return (float) (Math.toDegrees(Math.atan2(bestForwardZ, bestForwardX)) - 90.0D);
    }

    private float yawTo(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private double scoreVoidPath(EntityPlayer target, double forwardX, double forwardZ) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double score = 0.0D;
        double checkedForward = 0.0D;
        int consecutiveCenterVoid = 0;
        AxisAlignedBB baseBox = target.getEntityBoundingBox().contract(DYNAMIC_COLLISION_INSET, 0.0D, DYNAMIC_COLLISION_INSET);
        for (int step = 1; step <= (int) (DYNAMIC_SCAN_DISTANCE / DYNAMIC_SCAN_STEP); step++) {
            double forward = (double) step * DYNAMIC_SCAN_STEP;
            if (!this.isDynamicPathClear(target, baseBox, forwardX, forwardZ, checkedForward, forward)) {
                break;
            }
            checkedForward = forward;
            boolean centerVoid = false;
            for (int side = -1; side <= 1; side++) {
                double sideOffset = (double) side * DYNAMIC_SCAN_SIDE_STEP;
                double x = target.posX + forwardX * forward + sideX * sideOffset;
                double z = target.posZ + forwardZ * forward + sideZ * sideOffset;
                if (this.isVoidColumn(x, target.posY, z)) {
                    score += (side == 0 ? 1.4D : 1.0D) * (DYNAMIC_SCAN_DISTANCE + DYNAMIC_SCAN_STEP - forward);
                    centerVoid |= side == 0;
                }
            }
            if (centerVoid) {
                score += ++consecutiveCenterVoid * 2.0D;
            } else {
                consecutiveCenterVoid = 0;
            }
        }
        return score;
    }

    private boolean isDynamicPathClear(EntityPlayer target, AxisAlignedBB baseBox, double forwardX, double forwardZ, double fromForward, double toForward) {
        for (double forward = fromForward + DYNAMIC_WALL_CHECK_STEP; forward <= toForward + 1.0E-4D; forward += DYNAMIC_WALL_CHECK_STEP) {
            if (this.hasBlockCollision(target, baseBox.offset(forwardX * forward, 0.0D, forwardZ * forward))) {
                return false;
            }
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
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (!mc.theWorld.isBlockLoaded(blockPos.set(x, 64, z))) {
                    return true;
                }
                for (int y = minY; y < maxY; y++) {
                    if (y < 0 || y >= 256) {
                        return true;
                    }
                    blockPos.set(x, y, z);
                    IBlockState state = mc.theWorld.getBlockState(blockPos);
                    state.getBlock().addCollisionBoxesToList(mc.theWorld, blockPos, state, box, collisions, target);
                    if (!collisions.isEmpty()) {
                        return true;
                    }
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
            if (!mc.theWorld.isAirBlock(new BlockPos(blockX, blockY, blockZ))) {
                return false;
            }
        }
        return true;
    }

    private void updateDisplaceSide(EntityPlayer target, double voidX, double voidZ) {
        double targetDx = target.posX - mc.thePlayer.posX;
        double targetDz = target.posZ - mc.thePlayer.posZ;
        double voidDx = voidX - mc.thePlayer.posX;
        double voidDz = voidZ - mc.thePlayer.posZ;
        this.displaceLeft = targetDx * voidDz - targetDz * voidDx < 0.0D;
    }

    private float getFixedDisplaceYaw() {
        return this.displaceLeft ? mc.thePlayer.rotationYaw - this.yawOffset.getValue() : mc.thePlayer.rotationYaw + this.yawOffset.getValue();
    }

    private void clearActiveState() {
        this.startArrowFade();
        this.active = false;
        this.displaceThisTick = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.renderDisplaceYaw = null;
        this.renderTarget = null;
    }

    private void clearArrowState() {
        this.fadingDisplaceYaw = null;
        this.fadingTarget = null;
        this.arrowFadeStartMs = 0L;
        this.lastRenderedDisplaceYaw = null;
        this.lastRenderedTarget = null;
        this.lastRenderedArrowMs = 0L;
    }

    private void startArrowFade() {
        long nowMs = System.currentTimeMillis();
        if (this.lastRenderedDisplaceYaw != null && this.lastRenderedTarget != null && !this.lastRenderedTarget.isDead
                && nowMs - this.lastRenderedArrowMs <= ARROW_FADE_MS) {
            this.fadingDisplaceYaw = this.lastRenderedDisplaceYaw;
            this.fadingTarget = this.lastRenderedTarget;
            this.arrowFadeStartMs = nowMs;
        }
        this.lastRenderedDisplaceYaw = null;
        this.lastRenderedTarget = null;
        this.lastRenderedArrowMs = 0L;
    }

    private void pruneTargetDelayStates() {
        Iterator<Map.Entry<Integer, Integer>> iterator = this.targetWindowStartTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Entity entity = mc.theWorld.getEntityByID(iterator.next().getKey());
            if (!(entity instanceof EntityPlayer) || entity.isDead || ((EntityPlayer) entity).deathTime != 0) {
                iterator.remove();
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        int targetId = target.getEntityId();
        Integer windowStartTick = this.targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            this.targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }
        int delayTicks = msToTicks(this.delay.getValue());
        return delayTicks <= 0 || currentTick - windowStartTick >= delayTicks;
    }

    @EventTarget(Priority.HIGH)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (this.releaseBlinkNextTick) {
            this.releaseBlink();
            this.releaseBlinkNextTick = false;
        }
    }

    @EventTarget(Priority.LOW)
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled() || !this.active) {
            this.compensateNextTick = false;
            return;
        }
        if (this.compensateNextTick && !this.displaceThisTick) {
            this.compensateNextTick = false;
            event.setStrafe(this.displaceLeft ? -1.0F : 1.0F);
            return;
        }
        if (!this.displaceThisTick || this.hasKB || !this.anyMovementKey()) {
            return;
        }
        event.setForward(1.0F);
        this.compensateNextTick = true;
    }

    @EventTarget(Priority.HIGH)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !this.blink.getValue() || !this.active || !this.displaceThisTick || this.releaseBlinkNextTick) {
            return;
        }
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C03PacketPlayer && Myau.blinkManager != null) {
            Myau.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
            this.releaseBlinkNextTick = true;
        }
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null || mc.theWorld == null) {
            this.clearActiveState();
            return;
        }
        this.tickCounter++;
        this.pruneTargetDelayStates();

        if (this.hasKnockback.getValue() && EnchantmentHelper.getKnockbackModifier(mc.thePlayer) <= 0) {
            this.clearActiveState();
            return;
        }

        boolean attacking = mc.gameSettings.keyBindAttack.isKeyDown();
        EntityPlayer target = attacking ? this.findClosestTarget(9.0D) : null;
        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        this.active = target != null && (hasKBEnchant || this.anyMovementKey());
        if (!this.active) {
            this.clearActiveState();
            return;
        }

        Float dynamicVoidYaw = this.isDynamicAngle() ? this.findDynamicVoidYaw(target) : this.findVoid.getValue() ? this.findStaticVoidYaw(target) : null;
        if (dynamicVoidYaw == null && !this.isDynamicAngle()) {
            this.displaceLeft = this.direction.getValue() == 0;
        }
        this.renderDisplaceYaw = dynamicVoidYaw != null ? dynamicVoidYaw : this.isDynamicAngle() ? null : this.getFixedDisplaceYaw();
        this.renderTarget = this.renderDisplaceYaw != null ? target : null;
        if (this.renderDisplaceYaw == null) {
            this.clearActiveState();
            return;
        }

        this.hasKB = hasKBEnchant;
        this.displaceThisTick = !this.displaceThisTick;
        if (this.displaceThisTick && !this.shouldDisplaceInCurrentWindow(target, this.tickCounter)) {
            this.clearActiveState();
            return;
        }

        if (!this.displaceThisTick && this.wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) {
                KeyBinding.onTick(key);
            }
        }
        this.wasDisplacingLastTick = this.displaceThisTick;
        if (this.displaceThisTick) {
            event.setRotation(this.renderDisplaceYaw, event.getNewPitch(), 3);
            MoveUtil.fixMovement(this.renderDisplaceYaw);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !this.showDirection.getValue() || mc.thePlayer == null || mc.theWorld == null) {
            this.clearArrowState();
            return;
        }
        long nowMs = System.currentTimeMillis();
        boolean activeArrow = this.active && this.renderDisplaceYaw != null && this.renderTarget != null && !this.renderTarget.isDead;
        Float arrowYaw = this.renderDisplaceYaw;
        EntityPlayer arrowTarget = this.renderTarget;
        float alpha = 1.0F;
        if (!activeArrow) {
            if (this.fadingDisplaceYaw == null || this.fadingTarget == null || this.fadingTarget.isDead) {
                return;
            }
            long fadeElapsed = nowMs - this.arrowFadeStartMs;
            if (fadeElapsed >= ARROW_FADE_MS) {
                this.clearArrowState();
                return;
            }
            arrowYaw = this.fadingDisplaceYaw;
            arrowTarget = this.fadingTarget;
            alpha = 1.0F - (float) fadeElapsed / (float) ARROW_FADE_MS;
        }
        this.renderArrow(arrowTarget, arrowYaw, alpha, event.getPartialTicks());
        if (activeArrow) {
            this.lastRenderedDisplaceYaw = arrowYaw;
            this.lastRenderedTarget = arrowTarget;
            this.lastRenderedArrowMs = nowMs;
        }
    }

    private void renderArrow(EntityPlayer target, float yaw, float alpha, float partialTicks) {
        double centerX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double centerY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.5D;
        double centerZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;
        double yawRad = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double baseOffset = target.width * 0.5D + ARROW_FORWARD_GAP;
        double tailX = centerX + forwardX * baseOffset;
        double tailZ = centerZ + forwardZ * baseOffset;
        double bodyEndX = tailX + forwardX * ARROW_BODY_LENGTH;
        double bodyEndZ = tailZ + forwardZ * ARROW_BODY_LENGTH;
        double headBackX = tailX + forwardX * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double headBackZ = tailZ + forwardZ * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double tipX = bodyEndX + forwardX * ARROW_HEAD_LENGTH;
        double tipZ = bodyEndZ + forwardZ * ARROW_HEAD_LENGTH;
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.82F * alpha);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(tailX - viewerX, centerY - viewerY, tailZ - viewerZ);
        this.arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        this.arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        this.arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        this.arrowVertex(headBackX, centerY, headBackZ, -ARROW_HEAD_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        this.arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        this.arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void arrowVertex(double x, double y, double z, double verticalOffset, double viewerX, double viewerY, double viewerZ) {
        GL11.glVertex3d(x - viewerX, y + verticalOffset - viewerY, z - viewerZ);
    }
}
