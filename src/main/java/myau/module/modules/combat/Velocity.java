package myau.module.modules.combat;

import myau.module.modules.movement.LongJump;
import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean reverseFlag = false;
    private boolean delayActive = false;

    private boolean shouldJump = false;
    private int jumpCooldown = 0;
    private boolean hasReceivedVelocity = false;
    private int legitSmartJumpCount = 0;
    private int intaveTick = 0;
    private int intaveDamageTick = 0;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "DELAY", "REVERSE", "LEGIT_TEST", "LEGIT_SMART", "INTAVE_REDUCE", "GRIM_REDUCE", "JUMP_RESET"});
    public final IntProperty delayTicks = new IntProperty("delay-ticks", 3, 1, 20, () -> this.mode.getValue() == 1);
    public final PercentProperty delayChance = new PercentProperty("delay-chance", 100, () -> this.mode.getValue() == 1);
    public final IntProperty legitSmartJumpLimit = new IntProperty("legit-smart-jump-limit", 2, 1, 5, () -> this.mode.getValue() == 4);
    public final FloatProperty intaveReduceFactor = new FloatProperty("intave-reduce-factor", 0.6F, 0.6F, 1.0F, () -> this.mode.getValue() == 5);
    public final IntProperty intaveReduceHurtTime = new IntProperty("intave-reduce-hurt-time", 9, 1, 10, () -> this.mode.getValue() == 5);
    public final PercentProperty chance = new PercentProperty("chance", 100);
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0);
    public final PercentProperty vertical = new PercentProperty("vertical", 100);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100);
    public final IntProperty grimReduceJumpLimit = new IntProperty("grim-reduce-jump-limit", 2, 1, 5, () -> this.mode.getValue() == 6);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.thePlayer.onGround && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
        } else if (!this.allowNext || !(Boolean) this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                if (this.explosionHorizontal.getValue() > 0) {
                    event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
                    event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
                } else {
                    event.setX(mc.thePlayer.motionX);
                    event.setZ(mc.thePlayer.motionZ);
                }
                if (this.explosionVertical.getValue() > 0) {
                    event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
                } else {
                    event.setY(mc.thePlayer.motionY);
                }
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = this.mode.getValue() == 1 && event.getY() > 0.0;
                    this.delayActive = this.mode.getValue() == 1;
                    if (this.mode.getValue() == 6) {
                        this.hasReceivedVelocity = true;
                        return;
                    }
                    if (this.horizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.thePlayer.motionX);
                        event.setZ(mc.thePlayer.motionZ);
                    }
                    if (this.vertical.getValue() > 0) {
                        event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.thePlayer.motionY);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.reverseFlag
                    && (
                    this.canDelay()
                            || this.isInLiquidOrWeb()
                            || Myau.delayManager.getDelay() >= (long) this.delayTicks.getValue()
            )) {
                Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.reverseFlag = false;
            }
            if (this.delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                this.delayActive = false;
            }

            if (this.mode.getValue() == 3) {
                int hurtTime = mc.thePlayer.hurtTime;

                if (hurtTime >= 8) {
                    if (jumpCooldown <= 0) {
                        shouldJump = true;
                        jumpCooldown = 2;
                    }
                } else if (hurtTime <= 1) {
                    shouldJump = false;
                    jumpCooldown = 0;
                }

                if (shouldJump && mc.thePlayer.onGround && jumpCooldown <= 0) {
                    mc.thePlayer.jump();
                    shouldJump = false;
                }

                if (jumpCooldown > 0) {
                    jumpCooldown--;
                }
            }
            if (this.mode.getValue() == 4 && this.hasReceivedVelocity) {
                if (mc.thePlayer.onGround && mc.thePlayer.hurtTime == 9 && mc.thePlayer.isSprinting() && mc.currentScreen == null) {
                    if (this.legitSmartJumpCount > this.legitSmartJumpLimit.getValue()) {
                        this.legitSmartJumpCount = 0;
                    } else {
                        this.legitSmartJumpCount++;
                        if (mc.thePlayer.ticksExisted % 5 != 0) {
                            mc.thePlayer.jump();
                        }
                    }
                } else if (mc.thePlayer.hurtTime == 8) {
                    this.hasReceivedVelocity = false;
                    this.legitSmartJumpCount = 0;
                }
            }
            if (this.mode.getValue() == 5 && this.hasReceivedVelocity) {
                this.intaveTick++;
                if (mc.thePlayer.hurtTime == 2) {
                    this.intaveDamageTick++;
                    if (mc.thePlayer.onGround && this.intaveTick % 2 == 0 && this.intaveDamageTick <= 10) {
                        mc.thePlayer.jump();
                        this.intaveTick = 0;
                    }
                    this.hasReceivedVelocity = false;
                }
            }
            if (this.mode.getValue() == 6 && this.hasReceivedVelocity) {
                if (mc.thePlayer.onGround
                        && mc.thePlayer.hurtTime >= 8
                        && mc.thePlayer.isSprinting()
                        && mc.currentScreen == null
                        && !mc.thePlayer.isPotionActive(Potion.jump)
                        && !this.isInLiquidOrWeb()) {
                    if (this.legitSmartJumpCount >= this.grimReduceJumpLimit.getValue()) {
                        this.legitSmartJumpCount = 0;
                        this.hasReceivedVelocity = false;
                    } else {
                        this.legitSmartJumpCount++;
                        mc.thePlayer.movementInput.jump = true;
                    }
                } else if (mc.thePlayer.hurtTime <= 1) {
                    this.hasReceivedVelocity = false;
                    this.legitSmartJumpCount = 0;
                }
            }
            if (this.mode.getValue() == 7) {
                if (mc.thePlayer.onGround
                        && mc.thePlayer.hurtTime >= 9
                        && !this.isInLiquidOrWeb()
                        && myau.util.RandomUtil.nextInt(1, 100) <= this.chance.getValue()) {
                    mc.thePlayer.jump();
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
                    if (this.mode.getValue() == 1
                            && !this.reverseFlag
                            && !this.canDelay()
                            && !this.isInLiquidOrWeb()
                            && !this.pendingExplosion
                            && (!this.allowNext || !(Boolean) this.fakeCheck.getValue())
                            && (!longJump.isEnabled() || !longJump.canStartJump())) {
                        this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.reverseFlag = true;
                            return;
                        }
                    }
                    if (this.mode.getValue() == 4 || this.mode.getValue() == 5 || this.mode.getValue() == 6) {
                        this.hasReceivedVelocity = true;
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.display(
                                String.format(
                                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        (double) packet.getMotionX() / 8000.0,
                                        (double) packet.getMotionY() / 8000.0,
                                        (double) packet.getMotionZ() / 8000.0
                                )
                        );
                    }
                }
            } else if (!(event.getPacket() instanceof S27PacketExplosion)) {
                if (event.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                    Entity entity = packet.getEntity(mc.theWorld);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.display(
                                String.format(
                                        "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        mc.thePlayer.motionX + (double) packet.func_149149_c(),
                                        mc.thePlayer.motionY + (double) packet.func_149144_d(),
                                        mc.thePlayer.motionZ + (double) packet.func_149147_e()
                                )
                        );
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.shouldJump = false;
        this.jumpCooldown = 0;
        this.hasReceivedVelocity = false;
        this.legitSmartJumpCount = 0;
        this.intaveTick = 0;
        this.intaveDamageTick = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
