package myau.module.modules.player;

import myau.module.modules.combat.KillAura;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.PacketUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.Comparator;

public class    AutoRod extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty onlyWhileKillAura = new BooleanProperty("only-while-killaura", false);
    public final FloatProperty range = new FloatProperty("range", 10.0F, 5.0F, 15.0F, () -> !onlyWhileKillAura.getValue());
    public final FloatProperty aimSpeed = new FloatProperty("aim-speed", 10.0F, 5.0F, 20.0F);
    public final BooleanProperty prediction = new BooleanProperty("prediction", false);
    public final BooleanProperty smart = new BooleanProperty("smart", true, () -> prediction.getValue());
    public final IntProperty predictionTicks = new IntProperty("prediction-ticks", 2, 0, 10, () -> prediction.getValue() && !smart.getValue());
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);
    public final IntProperty clickDelay = new IntProperty("click-delay", 150, 0, 1000);

    private int fromSlot = -1;
    private EntityLivingBase target;
    private long lastClick;

    public AutoRod() {
        super("AutoRod", false);
    }

    @Override
    public void onEnabled() {
        this.fromSlot = -1;
        this.target = null;
        this.lastClick = 0L;
    }

    @Override
    public void onDisabled() {
        restoreSlot();
        this.target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (event.getType() == EventType.PRE) {
            this.target = findTarget();
            if (this.target != null) {
                int rodSlot = getRodSlot();
                if (rodSlot == -1 || mc.currentScreen != null) {
                    restoreSlot();
                    return;
                }
                if (this.fromSlot == -1) this.fromSlot = mc.thePlayer.inventory.currentItem;
                switchSlot(rodSlot);

                Vec3 pos = getPredictedEye(this.target);
                Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
                float[] rotations = RotationUtil.getRotationsTo(pos.xCoord - eyes.xCoord, pos.yCoord - eyes.yCoord, pos.zCoord - eyes.zCoord, event.getYaw(), event.getPitch());
                float yaw = smooth(event.getYaw(), rotations[0], this.aimSpeed.getValue());
                float pitch = smooth(event.getPitch(), rotations[1], this.aimSpeed.getValue());
                event.setRotation(yaw, pitch, 2);

                if (System.currentTimeMillis() - this.lastClick >= this.clickDelay.getValue()) {
                    ItemStack held = mc.thePlayer.getHeldItem();
                    if (held != null && held.getItem() instanceof ItemFishingRod) {
                        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(held));
                        this.lastClick = System.currentTimeMillis();
                        restoreSlot();
                    }
                }
            } else {
                restoreSlot();
            }
        }
    }

    private EntityLivingBase findTarget() {
        if (this.onlyWhileKillAura.getValue() && !myau.Myau.moduleManager.getModule(KillAura.class).isEnabled()) return null;
        double maxRange = this.onlyWhileKillAura.getValue() ? 12.0D : this.range.getValue();
        return mc.theWorld.playerEntities.stream()
                .filter(p -> p != mc.thePlayer)
                .filter(p -> !p.isDead && p.getHealth() > 0.0F)
                .filter(p -> !this.ignoreTeammates.getValue() || !TeamUtil.isSameTeam(p))
                .filter(p -> mc.thePlayer.canEntityBeSeen(p))
                .filter(p -> mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                        || mc.objectMouseOver.entityHit != p)
                .filter(p -> mc.thePlayer.getDistanceToEntity(p) <= maxRange)
                .min(Comparator.comparingDouble(p -> mc.thePlayer.getDistanceToEntity(p)))
                .orElse(null);
    }

    private Vec3 getPredictedEye(EntityLivingBase entity) {
        int ticks;
        if (!prediction.getValue()) {
            ticks = 0;
        } else if (smart.getValue() && mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
            ticks = Math.max(0, mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime() / 50);
        } else {
            ticks = predictionTicks.getValue();
        }
        double x = entity.posX + (entity.posX - entity.lastTickPosX) * ticks;
        double y = entity.posY + entity.getEyeHeight() + (entity.posY - entity.lastTickPosY) * ticks;
        double z = entity.posZ + (entity.posZ - entity.lastTickPosZ) * ticks;
        return new Vec3(x, y, z);
    }

    private int getRodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.fishing_rod) return i;
        }
        return -1;
    }

    private void switchSlot(int slot) {
        if (mc.thePlayer.inventory.currentItem != slot) {
            mc.thePlayer.inventory.currentItem = slot;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
        }
    }

    private void restoreSlot() {
        if (mc.thePlayer != null && this.fromSlot != -1) {
            switchSlot(this.fromSlot);
            this.fromSlot = -1;
        }
    }

    private float smooth(float current, float target, float speed) {
        float delta = net.minecraft.util.MathHelper.wrapAngleTo180_float(target - current);
        float step = Math.max(1.0F, speed);
        if (delta > step) delta = step;
        if (delta < -step) delta = -step;
        return current + delta;
    }
}
