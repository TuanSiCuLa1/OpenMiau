package myau.module.modules.combat;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil clickTimer = new TimerUtil();
    public final FloatProperty range = new FloatProperty("range", 5.0F, 1.0F, 10.0F);
    public final FloatProperty speed = new FloatProperty("speed", 1.0F, 1.0F, 60.0F);
    public final FloatProperty compliSpeed = new FloatProperty("compli-speed", 1.0F, 1.0F, 50.0F);
    public final FloatProperty fov = new FloatProperty("fov", 180.0F, 1.0F, 180.0F);
    public final BooleanProperty faceCheck = new BooleanProperty("face-check", false);
    public final BooleanProperty mouseDown = new BooleanProperty("mouse-down", true);
    public final BooleanProperty allowBreakBlock = new BooleanProperty("allow-break-block", false);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);

    public AimAssist() {
        super("AimAssist", false);
        this.clickTimer.reset();
    }

    private boolean isValidTarget(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer || player == mc.thePlayer.ridingEntity) {
            return false;
        }
        if (player == mc.getRenderViewEntity() || player == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        if (player.deathTime > 0 || player.isDead || !player.isEntityAlive()) {
            return false;
        }
        if (RotationUtil.distanceToEntity(player) > this.range.getValue()) {
            return false;
        }
        if (RotationUtil.angleToEntity(player) > this.fov.getValue()) {
            return false;
        }
        if (!mc.thePlayer.canEntityBeSeen(player)) {
            return false;
        }
        if (TeamUtil.isFriend(player)) {
            return false;
        }
        return (!this.team.getValue() || !TeamUtil.isSameTeam(player)) && (!this.botChecks.getValue() || !TeamUtil.isBot(player));
    }

    private boolean shouldSkipForBlockBreak() {
        if (!this.allowBreakBlock.getValue() || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) {
            return false;
        }
        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
        if (blockPos == null || mc.theWorld == null) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block != Blocks.air && !(block instanceof BlockLiquid);
    }

    private boolean shouldRunWeaponCheck() {
        return !this.weaponOnly.getValue() || ItemUtil.hasRawUnbreakingEnchant() || this.allowTools.getValue() && ItemUtil.isHoldingTool();
    }

    private boolean isFaced(Entity entity) {
        return RotationUtil.rayTrace(entity) == null && RotationUtil.angleToEntity(entity) <= 1.0F;
    }

    private float fovToTarget(Entity entity) {
        double x = entity.posX - mc.thePlayer.posX;
        double z = entity.posZ - mc.thePlayer.posZ;
        double yaw = Math.atan2(x, z) * 57.2957795D;
        return (float) (yaw * -1.0D);
    }

    private double fovFromTarget(Entity entity) {
        return ((mc.thePlayer.rotationYaw - fovToTarget(entity)) % 360.0D + 540.0D) % 360.0D - 180.0D;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.currentScreen != null || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (!shouldRunWeaponCheck() || shouldSkipForBlockBreak()) {
            return;
        }
        if (PlayerUtil.isAttacking()) {
            this.clickTimer.reset();
        }
        if (this.mouseDown.getValue() && this.clickTimer.hasTimeElapsed(100L)) {
            return;
        }

        List<EntityPlayer> targets = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(RotationUtil::angleToEntity))
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            return;
        }

        EntityPlayer target = targets.get(0);
        if (this.faceCheck.getValue() && isFaced(target)) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double normalSpeed = Math.max(1.0D, this.speed.getValue());
        double compli = Math.max(1.0D, this.compliSpeed.getValue());
        double fovFromTarget = fovFromTarget(target);
        double compliFactor = random.nextDouble(compli - 1.47328D, compli + 2.48293D) / 100.0D;
        double normalDivisor = 101.0D - random.nextDouble(normalSpeed - 4.723847D, normalSpeed);
        mc.thePlayer.rotationYaw += (float) (-(fovFromTarget * compliFactor + fovFromTarget / normalDivisor));
        mc.thePlayer.rotationYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.clickTimer.reset();
        }
    }
}