package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] NON_DELAYED_SOUND_SUBSTRINGS = new String[]{"game.player.hurt", "game.player.die"};

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"PACKET", "FAKE_PLAYER"});
    public final IntProperty nextBacktrackDelay = new IntProperty("next-backtrack-delay", 0, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty minMS = new IntProperty("min-ms", 80, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty maxMS = new IntProperty("max-ms", 80, 0, 2000, () -> mode.getValue() == 0);
    public final ModeProperty style = new ModeProperty("style", 1, new String[]{"PULSE", "SMOOTH"}, () -> mode.getValue() == 0);
    public final FloatProperty minDistance = new FloatProperty("min-distance", 2.0F, 0.0F, 6.0F, () -> mode.getValue() == 0);
    public final FloatProperty maxDistance = new FloatProperty("max-distance", 3.0F, 0.0F, 6.0F, () -> mode.getValue() == 0);
    public final BooleanProperty smart = new BooleanProperty("smart", true, () -> mode.getValue() == 0);
    public final ModeProperty espMode = new ModeProperty("esp", 1, new String[]{"NONE", "BOX", "MODEL", "WIREFRAME"}, () -> mode.getValue() == 0);
    public final FloatProperty wireframeWidth = new FloatProperty("wireframe-width", 1.0F, 0.5F, 5.0F, () -> mode.getValue() == 0 && espMode.getValue() == 3);
    public final ColorProperty espColor = new ColorProperty("color", 0xFF00FF00);
    public final IntProperty fakePlayerPulseDelay = new IntProperty("fake-player-pulse-delay", 200, 50, 500, () -> mode.getValue() == 1);
    public final IntProperty fakePlayerIntavePackets = new IntProperty("fake-player-intave-packets", 5, 1, 30, () -> mode.getValue() == 1);

    private final Queue<QueuedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<TimedPosition> positions = new ConcurrentLinkedQueue<>();
    private final TimerUtil globalTimer = new TimerUtil();
    private final TimerUtil fakePulseTimer = new TimerUtil();

    private EntityLivingBase target;
    private boolean shouldRender;
    private boolean ignoreWholeTick;
    private long delayForNextBacktrack;
    private int modernDelay;
    private boolean delayChanged;
    private EntityOtherPlayerMP fakePlayer;
    private EntityLivingBase currentTarget;
    private boolean fakeShown;

    public BackTrack() {
        super("BackTrack", false);
    }

    @Override
    public void onEnabled() {
        reset();
        modernDelay = randomLatency();
        delayChanged = false;
    }

    @Override
    public void onDisabled() {
        clearPackets(true, true);
        positions.clear();
        removeFakePlayer();
        reset();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clearPackets(false, true);
        positions.clear();
        removeFakePlayer();
        reset();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 20) {
            clearPackets(false, true);
            reset();
            removeFakePlayer();
            return;
        }
        if (mode.getValue() == 1) {
            updateFakePlayer();
            return;
        }
        updatePacketMode();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) return;
        if (mode.getValue() != 0 || event.getType() != EventType.RECEIVE) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 20) {
            clearPackets(false, true);
            reset();
            return;
        }

        if (TickBase.duringTickModification) {
            clearPackets(true, false);
            return;
        }

        Packet<?> packet = event.getPacket();
        if (isIgnoredPacket(packet)) return;
        if (isFlushPacket(packet)) {
            clearPackets(true, true);
            reset();
            return;
        }
        if (packetQueue.isEmpty() && !shouldBacktrack()) return;

        trackPosition(packet);
        event.setCancelled(true);
        packetQueue.offer(new QueuedPacket(packet, System.currentTimeMillis()));
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;
        if (mode.getValue() == 1) {
            handleFakePlayerAttack(event);
            return;
        }
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase attacked = (EntityLivingBase) event.getTarget();
        if (target != attacked) {
            clearPackets(true, true);
            reset();
        }
        target = attacked;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mode.getValue() != 0 || !shouldBacktrack() || !shouldRender || target == null) return;
        if (espMode.getValue() == 0) return;
        TimedPosition renderPos = getLatestPosition();
        if (renderPos == null) return;

        double x = renderPos.position.xCoord - mc.getRenderManager().viewerPosX;
        double y = renderPos.position.yCoord - mc.getRenderManager().viewerPosY;
        double z = renderPos.position.zCoord - mc.getRenderManager().viewerPosZ;
        Color color = new Color(espColor.getValue(), true);

        switch (espMode.getValue()) {
            case 1:
                drawBacktrackBox(createRenderBox(renderPos.position), color);
                break;
            case 2:
                GlStateManager.pushMatrix();
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GlStateManager.color(0.6F, 0.6F, 0.6F, 1.0F);
                mc.getRenderManager().doRenderEntity(
                        target,
                        x,
                        y,
                        z,
                        target.prevRotationYaw + (target.rotationYaw - target.prevRotationYaw) * event.getPartialTicks(),
                        event.getPartialTicks(),
                        true
                );
                GL11.glPopAttrib();
                GlStateManager.popMatrix();
                break;
            case 3:
                GlStateManager.pushMatrix();
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glLineWidth(wireframeWidth.getValue());
                GL11.glTranslated(x - (target.posX - mc.getRenderManager().viewerPosX), y - (target.posY - mc.getRenderManager().viewerPosY), z - (target.posZ - mc.getRenderManager().viewerPosZ));
                GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
                mc.getRenderManager().renderEntityStatic(target, event.getPartialTicks(), true);
                GL11.glPopAttrib();
                GlStateManager.popMatrix();
                break;
            default:
                break;
        }
    }

    private void updatePacketMode() {
        if (shouldBacktrack()) {
            double trueDist = getTrueDistance();
            double clientDist = mc.thePlayer.getDistanceToEntity(target);
            if (trueDist <= 6.0D && (!smart.getValue() || trueDist >= clientDist)
                    && (style.getValue() == 1 || !globalTimer.hasTimeElapsed(modernDelay))) {
                shouldRender = true;
                if (isTargetInConfiguredRange(target.getEntityBoundingBox())) {
                    handlePackets();
                } else {
                    handlePacketsRange();
                }
            } else {
                clear();
            }
        } else {
            clear();
        }
        updateDelayCooldown();
        ignoreWholeTick = false;
    }

    private void handlePackets() {
        long releaseTime = System.currentTimeMillis() - modernDelay;
        Iterator<QueuedPacket> packetIterator = packetQueue.iterator();
        while (packetIterator.hasNext()) {
            QueuedPacket data = packetIterator.next();
            if (data.time <= releaseTime) {
                receiveQueuedPacket(data.packet);
                packetIterator.remove();
            }
        }
        positions.removeIf(position -> position.time < releaseTime);
    }

    private void handlePacketsRange() {
        long time = getRangeTime();
        if (time == -1L) {
            clearPackets(true, true);
            return;
        }
        Iterator<QueuedPacket> packetIterator = packetQueue.iterator();
        while (packetIterator.hasNext()) {
            QueuedPacket data = packetIterator.next();
            if (data.time <= time) {
                receiveQueuedPacket(data.packet);
                packetIterator.remove();
            }
        }
        positions.removeIf(position -> position.time < time);
    }

    private long getRangeTime() {
        if (target == null) return 0L;
        long time = 0L;
        boolean found = false;
        for (TimedPosition data : positions) {
            time = data.time;
            AxisAlignedBB targetBox = target.getEntityBoundingBox().offset(
                    data.position.xCoord - target.posX,
                    data.position.yCoord - target.posY,
                    data.position.zCoord - target.posZ
            );
            if (isTargetInConfiguredRange(targetBox)) {
                found = true;
                break;
            }
        }
        return found ? time : -1L;
    }

    private boolean shouldBacktrack() {
        return mc.thePlayer != null
                && mc.theWorld != null
                && target != null
                && mc.thePlayer.getHealth() > 0.0F
                && (target.getHealth() > 0.0F || Float.isNaN(target.getHealth()))
                && mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR
                && System.currentTimeMillis() >= delayForNextBacktrack
                && mc.thePlayer.ticksExisted > 20
                && !target.isDead
                && target != mc.thePlayer
                && !ignoreWholeTick;
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        if (packet instanceof C00Handshake || packet instanceof C00PacketServerQuery || packet instanceof S02PacketChat || packet instanceof S01PacketPong) {
            return true;
        }
        if (packet instanceof S29PacketSoundEffect) {
            String sound = ((S29PacketSoundEffect) packet).getSoundName();
            for (String ignored : NON_DELAYED_SOUND_SUBSTRINGS) {
                if (sound.contains(ignored)) return true;
            }
        }
        if (packet instanceof S19PacketEntityStatus && target != null) {
            Entity entity = ((S19PacketEntityStatus) packet).getEntity(mc.theWorld);
            return entity != null && entity.getEntityId() == target.getEntityId();
        }
        return false;
    }

    private boolean isFlushPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) return true;
        if (packet instanceof S06PacketUpdateHealth && ((S06PacketUpdateHealth) packet).getHealth() <= 0.0F) return true;
        if (packet instanceof S13PacketDestroyEntities && target != null) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                if (id == target.getEntityId()) return true;
            }
        }
        if (packet instanceof S1CPacketEntityMetadata && target != null && ((S1CPacketEntityMetadata) packet).getEntityId() == target.getEntityId()) {
            return isDeadMetadata((S1CPacketEntityMetadata) packet);
        }
        return false;
    }

    private boolean isDeadMetadata(S1CPacketEntityMetadata packet) {
        if (packet.func_149376_c() == null) return false;
        for (Object watchedObject : packet.func_149376_c()) {
            if (!(watchedObject instanceof net.minecraft.entity.DataWatcher.WatchableObject)) continue;
            net.minecraft.entity.DataWatcher.WatchableObject data = (net.minecraft.entity.DataWatcher.WatchableObject) watchedObject;
            if (data.getDataValueId() != 6 || data.getObject() == null) continue;
            try {
                double value = Double.parseDouble(data.getObject().toString());
                if (!Double.isNaN(value) && value <= 0.0D) return true;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private void trackPosition(Packet<?> packet) {
        if (target == null) return;
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity movement = (S14PacketEntity) packet;
            Entity entity = movement.getEntity(mc.theWorld);
            if (entity != null && entity.getEntityId() == target.getEntityId()) {
                double x = (target.serverPosX + movement.func_149062_c()) / 32.0D;
                double y = (target.serverPosY + movement.func_149061_d()) / 32.0D;
                double z = (target.serverPosZ + movement.func_149064_e()) / 32.0D;
                positions.offer(new TimedPosition(new Vec3(x, y, z), System.currentTimeMillis()));
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport teleport = (S18PacketEntityTeleport) packet;
            if (teleport.getEntityId() == target.getEntityId()) {
                positions.offer(new TimedPosition(new Vec3(teleport.getX() / 32.0D, teleport.getY() / 32.0D, teleport.getZ() / 32.0D), System.currentTimeMillis()));
            }
        }
    }

    private double getTrueDistance() {
        TimedPosition latest = getLatestPosition();
        if (latest == null || mc.thePlayer == null) return target.getDistanceToEntity(mc.thePlayer);
        return mc.thePlayer.getDistance(latest.position.xCoord, latest.position.yCoord, latest.position.zCoord);
    }

    private boolean isTargetInConfiguredRange(AxisAlignedBB box) {
        double distance = RotationUtil.distanceToBox(box);
        float min = Math.min(minDistance.getValue(), maxDistance.getValue());
        float max = Math.max(minDistance.getValue(), maxDistance.getValue());
        return distance >= min && distance <= max;
    }

    private AxisAlignedBB createRenderBox(Vec3 position) {
        float border = target.getCollisionBorderSize();
        AxisAlignedBB box = target.getEntityBoundingBox().expand(border, border, border);
        return box.offset(
                position.xCoord - target.posX - mc.getRenderManager().viewerPosX,
                position.yCoord - target.posY - mc.getRenderManager().viewerPosY,
                position.zCoord - target.posZ - mc.getRenderManager().viewerPosZ
        );
    }

    private TimedPosition getLatestPosition() {
        TimedPosition latest = null;
        for (TimedPosition position : positions) {
            latest = position;
        }
        if (latest == null && target != null) {
            latest = new TimedPosition(new Vec3(target.posX, target.posY, target.posZ), System.currentTimeMillis());
        }
        return latest;
    }

    private void drawBacktrackBox(AxisAlignedBB box, Color color) {
        GlStateManager.pushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        RenderGlobal.drawOutlinedBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GlStateManager.popMatrix();
    }

    private void clear() {
        clearPackets(true, true);
        globalTimer.reset();
    }

    private void clearPackets(boolean handlePackets, boolean stopRendering) {
        Iterator<QueuedPacket> iterator = packetQueue.iterator();
        while (iterator.hasNext()) {
            QueuedPacket data = iterator.next();
            if (handlePackets) receiveQueuedPacket(data.packet);
            iterator.remove();
        }
        positions.clear();
        if (stopRendering) {
            shouldRender = false;
            ignoreWholeTick = true;
        }
    }

    private void updateDelayCooldown() {
        boolean shouldChangeDelay = packetQueue.isEmpty();
        if (!shouldChangeDelay) {
            delayChanged = false;
        }
        if (shouldChangeDelay && !delayChanged && !shouldBacktrack()) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay.getValue();
            modernDelay = randomLatency();
            delayChanged = true;
        }
    }

    private void reset() {
        target = null;
        globalTimer.reset();
    }

    private void receiveQueuedPacket(Packet<?> packet) {
        if (packet == null || mc.getNetHandler() == null) return;
        PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) packet);
    }

    private int randomLatency() {
        return randomInt(minMS.getValue(), maxMS.getValue());
    }

    private static int randomInt(int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return RandomUtil.nextInt(low, high);
    }

    public Vec3 getTrackedPositionForDebug(EntityLivingBase entity) {
        TimedPosition position = getLatestPosition();
        if (!this.isEnabled() || mode.getValue() != 0 || entity == null || target == null || position == null) return null;
        if (entity.getEntityId() != target.getEntityId()) return null;
        return position.position;
    }

    private void attackRealTarget(EntityLivingBase entity) {
        if (entity == null || mc.thePlayer == null || mc.getNetHandler() == null) return;
        mc.thePlayer.swingItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
        if (mc.playerController != null) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(entity);
        }
    }

    private void createFakePlayer(EntityLivingBase target) {
        if (mc.theWorld == null || mc.getNetHandler() == null || !(target instanceof EntityPlayer)) return;
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(target.getUniqueID());
        if (playerInfo == null) return;

        EntityOtherPlayerMP faker = new EntityOtherPlayerMP(mc.theWorld, playerInfo.getGameProfile());
        faker.rotationYawHead = target.rotationYawHead;
        faker.renderYawOffset = target.renderYawOffset;
        faker.copyLocationAndAnglesFrom(target);
        faker.setHealth(target.getHealth());
        copyEquipment(target, faker);
        mc.theWorld.addEntityToWorld(-1337, faker);
        fakePlayer = faker;
        fakeShown = true;
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(fakePlayer);
        }
        fakePlayer = null;
        currentTarget = null;
        fakeShown = false;
    }

    private void handleFakePlayerAttack(AttackEvent event) {
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase attacked = (EntityLivingBase) event.getTarget();

        if (fakePlayer != null && attacked.getEntityId() == fakePlayer.getEntityId()) {
            attackRealTarget(currentTarget);
            event.setCancelled(true);
            return;
        }

        if (attacked == mc.thePlayer) return;
        if (fakePlayer == null || attacked != currentTarget) {
            removeFakePlayer();
            currentTarget = attacked;
            createFakePlayer(attacked);
            fakePulseTimer.reset();
        }
    }

    private void updateFakePlayer() {
        if (currentTarget == null || fakePlayer == null) {
            if (!fakeShown && currentTarget != null) createFakePlayer(currentTarget);
            return;
        }

        if (currentTarget.isDead || !currentTarget.isEntityAlive() || !fakePlayer.isEntityAlive()) {
            removeFakePlayer();
            return;
        }

        fakePlayer.setHealth(currentTarget.getHealth());
        copyEquipment(currentTarget, fakePlayer);

        boolean shouldPulse = mc.thePlayer.ticksExisted % Math.max(fakePlayerIntavePackets.getValue(), 1) == 0
                || fakePulseTimer.hasTimeElapsed(fakePlayerPulseDelay.getValue());
        if (shouldPulse) {
            fakePlayer.rotationYawHead = currentTarget.rotationYawHead;
            fakePlayer.renderYawOffset = currentTarget.renderYawOffset;
            fakePlayer.copyLocationAndAnglesFrom(currentTarget);
            fakePulseTimer.reset();
        }
    }

    private void copyEquipment(EntityLivingBase source, EntityLivingBase destination) {
        for (int index = 0; index <= 4; index++) {
            ItemStack stack = source.getEquipmentInSlot(index);
            destination.setCurrentItemOrArmor(index, stack == null ? null : stack.copy());
        }
    }

    private static class QueuedPacket {
        private final Packet<?> packet;
        private final long time;

        QueuedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class TimedPosition {
        private final Vec3 position;
        private final long time;

        TimedPosition(Vec3 position, long time) {
            this.position = position;
            this.time = time;
        }
    }
}
