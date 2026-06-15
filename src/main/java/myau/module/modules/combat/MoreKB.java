package myau.module.modules.combat;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

public class MoreKB extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // Thêm các mode của LiquidBounce (CCBlueX) vào cuối mảng
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{
            "LEGIT", "LEGIT_FAST", "LESS_PACKET", "PACKET", "DOUBLE_PACKET",
            "WTap", "SprintTap", "Silent", "SneakPacket", "SpamS"
    });

    public final BooleanProperty intelligent = new BooleanProperty("intelligent", false);
    public final BooleanProperty onlyGround = new BooleanProperty("only-ground", true);

    // Properties mượn từ CCBlueX cho mode SpamS
    public final FloatProperty spamSDistance = new FloatProperty("spams-distance", 3.0F, 0.0F, 6.0F, () -> this.mode.getValue() == 9);
    public final IntProperty spamSTick = new IntProperty("spams-tick", 2, 0, 10, () -> this.mode.getValue() == 9);

    private EntityLivingBase target;

    // Trạng thái cho CCBlueX modes
    private int ticks = 0;
    private int spamSActiveTicks = 0;
    private int wTapTicks = 0;

    public MoreKB() {
        super("MoreKB", false);
        this.target = null;
    }

    public void onDisable() {
        this.target = null;
        this.ticks = 0;
        this.spamSActiveTicks = 0;
        this.wTapTicks = 0;
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;

        Entity targetEntity = event.getTarget();
        if (targetEntity instanceof EntityLivingBase) {
            this.target = (EntityLivingBase) targetEntity;

            int currentMode = this.mode.getValue();
            if (currentMode >= 5 && mc.thePlayer != null) {
                if (this.onlyGround.getValue() && !mc.thePlayer.onGround) return;

                double distance = mc.thePlayer.getDistanceToEntity(this.target);

                switch (currentMode) {
                    case 5: // WTap
                        if (mc.thePlayer.isSprinting()) {
                            this.wTapTicks = 2;
                        }
                        break;
                    case 6:
                    case 7:
                        if (mc.thePlayer.isSprinting()) {
                            this.ticks = 2;
                        }
                        break;
                    case 8: // SneakPacket
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                        break;
                    case 9: // SpamS
                        if (distance <= (Float) this.spamSDistance.getValue()) {
                            this.spamSActiveTicks = (Integer) this.spamSTick.getValue();
                        }
                        break;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        // Logic gửi Packet ngầm cho mode Silent
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C03PacketPlayer) {
            if (this.mode.getValue() == 7) { // Silent
                if (this.ticks == 2) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    this.ticks--;
                } else if (this.ticks == 1 && mc.thePlayer.isSprinting()) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    this.ticks--;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        int currentMode = this.mode.getValue();

        // ---------------- CCBlueX UPDATE LOGIC ----------------
        if (currentMode >= 5) {
            // Mode WTap
            if (currentMode == 5) {
                if (this.wTapTicks > 0) {
                    mc.thePlayer.setSprinting(false);
                    // Ép chặn di chuyển tới trước như cách CCBlueX chặn input
                    mc.thePlayer.movementInput.moveForward = 0.0F;
                    this.wTapTicks--;
                }
            }
            // Mode SprintTap
            else if (currentMode == 6) {
                if (this.ticks == 2) {
                    mc.thePlayer.setSprinting(false);
                    this.ticks--;
                } else if (this.ticks == 1) {
                    if (mc.thePlayer.movementInput.moveForward > 0.8F) {
                        mc.thePlayer.setSprinting(true);
                    }
                    this.ticks--;
                }
            }
            // Mode SpamS
            else if (currentMode == 9) {
                boolean realBackState = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
                if (this.spamSActiveTicks > 0) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                    this.spamSActiveTicks--;
                }
            }
            return;
        }

        if (currentMode == 1) { // LEGIT_FAST
            if (this.target != null && this.isMoving()) {
                if ((this.onlyGround.getValue() && mc.thePlayer.onGround) || !this.onlyGround.getValue()) {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                this.target = null;
            }
            return;
        }

        EntityLivingBase entity = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }

        if (entity == null) return;

        double x = mc.thePlayer.posX - entity.posX;
        double z = mc.thePlayer.posZ - entity.posZ;
        float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));

        if (this.intelligent.getValue() && diffY > 120.0F) {
            return;
        }

        // Chạy packet đẩy KB ở hurtTime = 10 (Chuẩn OpenMiau cũ)
        if (entity.hurtTime == 10) {
            switch (currentMode) {
                case 0: // LEGIT
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                        mc.thePlayer.setSprinting(true);
                    }
                    break;
                case 2: // LESS_PACKET
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 3: // PACKET
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 4: // DOUBLE_PACKET
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
            }
        }
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    @Override
    public String[] getSuffix() {
        String[] modeNames = new String[]{
                "Legit", "LegitFast", "LessPacket", "Packet", "DoublePacket",
                "WTap", "SprintTap", "Silent", "SneakPacket", "SpamS"
        };
        return new String[]{modeNames[this.mode.getValue()]};
    }
}