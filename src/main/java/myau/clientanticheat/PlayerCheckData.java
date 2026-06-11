package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class PlayerCheckData {
    private static final double TELEPORT_DISTANCE_THRESHOLD = 8.0D;
    private static final int TELEPORT_EXEMPT_TICKS = 4;
    private static final int MAX_SINCE_HURT_TICKS = 999;
    private static final int RECENT_HURT_TICKS = 10;
    private static final int INITIAL_EXEMPT_TICKS = 5;
    private static final double GROUND_HORIZONTAL_LIMIT = 0.36D;
    private static final double AIR_HORIZONTAL_LIMIT = 0.62D;
    private static final double SPRINT_LIMIT_BONUS = 0.08D;
    private static final double SNEAK_LIMIT_BONUS = 0.02D;
    private static final double USING_ITEM_LIMIT_BONUS = 0.02D;
    private static final double SPEED_POTION_LIMIT_BONUS = 0.075D;
    private static final double JUMP_POTION_LIMIT_BONUS = 0.04D;
    private static final double RECENT_HURT_LIMIT_BONUS = 0.35D;

    public final String name;

    public double lastX;
    public double lastY;
    public double lastZ;
    public double x;
    public double y;
    public double z;
    public double deltaX;
    public double deltaY;
    public double deltaZ;
    public double horizontalDelta;
    public double lastHorizontalDelta;

    public float yaw;
    public float pitch;
    public float lastYaw;
    public float lastPitch;
    public float yawDelta;
    public float pitchDelta;

    public boolean onGround;
    public boolean lastOnGround;
    public int groundTicks;
    public int airTicks;
    public int hurtTicks;
    public int sinceHurtTicks = 999;
    public int teleportTicks;
    public int existedTicks;
    public float observedFallDistance;

    public PlayerCheckData(EntityPlayer player) {
        this.name = player.getName();
        this.x = this.lastX = player.posX;
        this.y = this.lastY = player.posY;
        this.z = this.lastZ = player.posZ;
        this.yaw = this.lastYaw = player.rotationYaw;
        this.pitch = this.lastPitch = player.rotationPitch;
        this.onGround = this.lastOnGround = player.onGround;
    }

    public void update(EntityPlayer player) {
        this.existedTicks++;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastOnGround = this.onGround;
        this.lastHorizontalDelta = this.horizontalDelta;

        this.x = player.posX;
        this.y = player.posY;
        this.z = player.posZ;
        this.yaw = player.rotationYaw;
        this.pitch = player.rotationPitch;
        this.onGround = player.onGround;

        this.deltaX = this.x - this.lastX;
        this.deltaY = this.y - this.lastY;
        this.deltaZ = this.z - this.lastZ;
        this.horizontalDelta = Math.hypot(this.deltaX, this.deltaZ);
        this.yawDelta = Math.abs(MathHelper.wrapAngleTo180_float(this.yaw - this.lastYaw));
        this.pitchDelta = Math.abs(this.pitch - this.lastPitch);

        double totalDelta = Math.sqrt(this.deltaX * this.deltaX + this.deltaY * this.deltaY + this.deltaZ * this.deltaZ);
        this.teleportTicks = totalDelta > TELEPORT_DISTANCE_THRESHOLD ? TELEPORT_EXEMPT_TICKS : Math.max(0, this.teleportTicks - 1);
        this.groundTicks = this.onGround ? this.groundTicks + 1 : 0;
        this.airTicks = this.onGround ? 0 : this.airTicks + 1;
        this.hurtTicks = player.hurtTime > 0 ? player.hurtTime : Math.max(0, this.hurtTicks - 1);
        this.sinceHurtTicks = player.hurtTime > 0 ? 0 : Math.min(MAX_SINCE_HURT_TICKS, this.sinceHurtTicks + 1);

        if (!this.onGround && this.deltaY < 0.0D) {
            this.observedFallDistance += (float) -this.deltaY;
        } else if (this.onGround) {
            this.observedFallDistance = 0.0F;
        }
    }

    public boolean recentlyTeleported() {
        return this.teleportTicks > 0 || this.existedTicks < INITIAL_EXEMPT_TICKS;
    }

    public boolean recentlyHurt() {
        return this.sinceHurtTicks <= RECENT_HURT_TICKS || this.hurtTicks > 0;
    }

    public double predictedHorizontalLimit(EntityPlayer player) {
        double limit = this.onGround ? GROUND_HORIZONTAL_LIMIT : AIR_HORIZONTAL_LIMIT;
        if (player.isSprinting()) limit += SPRINT_LIMIT_BONUS;
        if (player.isSneaking()) limit += SNEAK_LIMIT_BONUS;
        if (player.isUsingItem()) limit += USING_ITEM_LIMIT_BONUS;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            limit += amplifier * SPEED_POTION_LIMIT_BONUS;
        }
        if (player.isPotionActive(Potion.jump)) limit += JUMP_POTION_LIMIT_BONUS;
        if (this.recentlyHurt()) limit += RECENT_HURT_LIMIT_BONUS;
        return limit;
    }
}
