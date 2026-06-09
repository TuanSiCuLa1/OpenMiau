package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.util.ITruePosition;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import myau.util.TimerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
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
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Logger LOGGER = LogManager.getLogger("BackTrack");
    private static final String[] NON_DELAYED_SOUND_SUBSTRINGS = new String[]{"game.player.hurt", "game.player.die"};
    private static final long MAX_QUEUE_TIME = 1000L;

    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"LEGACY", "MODERN", "FAKE_PLAYER"});
    public final IntProperty nextBacktrackDelay = new IntProperty("next-backtrack-delay", 0, 0, 2000, () -> mode.getValue() == 1);
    public final IntProperty minMS = new IntProperty("min-ms", 80, 0, 2000, () -> mode.getValue() == 1);
    public final IntProperty maxMS = new IntProperty("max-ms", 80, 0, 2000, () -> mode.getValue() == 1);
    public final ModeProperty style = new ModeProperty("style", 1, new String[]{"PULSE", "SMOOTH"}, () -> mode.getValue() == 1);
    public final FloatProperty minDistance = new FloatProperty("min-distance", 2.0F, 0.0F, 6.0F, () -> mode.getValue() == 1);
    public final FloatProperty maxDistance = new FloatProperty("max-distance", 3.0F, 0.0F, 6.0F, () -> mode.getValue() == 1);
    public final BooleanProperty smart = new BooleanProperty("smart", true, () -> mode.getValue() == 1);
    public final ModeProperty legacyPos = new ModeProperty("caching-mode", 0, new String[]{"CLIENT_POS", "SERVER_POS"}, () -> mode.getValue() == 0);
    public final IntProperty maximumCachedPositions = new IntProperty("max-cached-positions", 10, 1, 20, () -> mode.getValue() == 0);
    public final ModeProperty espMode = new ModeProperty("esp", 1, new String[]{"NONE", "BOX", "MODEL", "WIREFRAME"}, () -> mode.getValue() == 1);
    public final FloatProperty wireframeWidth = new FloatProperty("wireframe-width", 1.0F, 0.5F, 5.0F, () -> mode.getValue() == 1 && espMode.getValue() == 3);
    public final ColorProperty espColor = new ColorProperty("color", 0xFF00FF00);
    public final IntProperty fakePlayerPulseDelay = new IntProperty("fake-player-pulse-delay", 200, 50, 500, () -> mode.getValue() == 2);
    public final IntProperty fakePlayerIntavePackets = new IntProperty("fake-player-intave-packets", 5, 1, 30, () -> mode.getValue() == 2);

    private final Queue<QueuedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<TimedPosition> positions = new ConcurrentLinkedQueue<>();
    private final Map<UUID, List<BacktrackData>> backtrackedPlayer = new ConcurrentHashMap<>();
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
        backtrackedPlayer.clear();
        removeFakePlayer();
        reset();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clearPackets(false, true);
        positions.clear();
        backtrackedPlayer.clear();
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
        if (mode.getValue() == 0) {
            updateLegacyMode();
            return;
        }
        if (mode.getValue() == 2) {
            updateFakePlayer();
            return;
        }
        updateModernMode();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 20) {
            clearPackets(false, true);
            reset();
            return;
        }

        Packet<?> packet = event.getPacket();
        if (mode.getValue() == 0) {
            handleLegacyPacket(packet);
            return;
        }
        if (mode.getValue() != 1 || event.getType() != EventType.RECEIVE) return;

        if (TickBase.duringTickModification) {
            clearPackets(true, false);
            return;
        }
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            clearPackets(true, true);
            reset();
            return;
        }
        if (packetQueue.isEmpty() && !shouldBacktrack()) return;
        if (isIgnoredPacket(packet) || isCriticalSyncPacket(packet)) return;
        if (isFlushPacket(packet)) {
            clearPackets(true, true);
            reset();
            return;
        }

        trackTruePosition(packet);
        event.setCancelled(true);
        packetQueue.offer(new QueuedPacket(packet, System.currentTimeMillis()));
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mode.getValue() == 2) {
            handleFakePlayerAttack(event);
            return;
        }
        if (mode.getValue() != 1) return;
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
        if (!this.isEnabled()) return;
        if (mode.getValue() == 0) {
            renderLegacyPaths();
            return;
        }
        if (mode.getValue() != 1 || !shouldBacktrack() || !shouldRender || target == null) return;
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

    private void updateLegacyMode() {
        long expiry = System.currentTimeMillis() - getSupposedDelay();
        Iterator<Map.Entry<UUID, List<BacktrackData>>> iterator = backtrackedPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<BacktrackData>> entry = iterator.next();
            entry.getValue().removeIf(data -> data.time + getSupposedDelay() < System.currentTimeMillis());
            if (entry.getValue().isEmpty()) iterator.remove();
        }
    }

    private void handleLegacyPacket(Packet<?> packet) {
        if (packet instanceof S0CPacketSpawnPlayer) {
            S0CPacketSpawnPlayer spawn = (S0CPacketSpawnPlayer) packet;
            addBacktrackData(spawn.getPlayer(), (double) spawn.getX() / 32.0D, (double) spawn.getY() / 32.0D,
                    (double) spawn.getZ() / 32.0D, System.currentTimeMillis());
            return;
        }
        if (legacyPos.getValue() != 1) return;

        int id = -1;
        if (packet instanceof S14PacketEntity) {
            Entity packetEntity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
            if (packetEntity != null) id = packetEntity.getEntityId();
        } else if (packet instanceof S18PacketEntityTeleport) {
            id = ((S18PacketEntityTeleport) packet).getEntityId();
        }
        if (id == -1 || mc.theWorld == null) return;
        Entity entity = mc.theWorld.getEntityByID(id);
        if (!(entity instanceof EntityPlayer) || !(entity instanceof ITruePosition)) return;
        ITruePosition accessor = (ITruePosition) entity;
        addBacktrackData(entity.getUniqueID(), accessor.getTrueX(), accessor.getTrueY(), accessor.getTrueZ(),
                System.currentTimeMillis());
    }

    private void renderLegacyPaths() {
        if (mc.theWorld == null) return;
        Color color = Color.RED;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            List<BacktrackData> data = backtrackedPlayer.get(entity.getUniqueID());
            if (data == null || data.isEmpty()) continue;

            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);
            for (BacktrackData point : data) {
                GL11.glVertex3d(point.x - mc.getRenderManager().viewerPosX,
                        point.y - mc.getRenderManager().viewerPosY,
                        point.z - mc.getRenderManager().viewerPosZ);
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    private void updateModernMode() {
        if (shouldBacktrack()) {
            double trueDist = getTrueDistance();
            double clientDist = mc.thePlayer.getDistanceToEntity(target);
            if (trueDist <= 6.0D && (!smart.getValue() || trueDist >= clientDist)
                    && (style.getValue() == 1 || !globalTimer.hasTimeElapsed(modernDelay))) {
                shouldRender = true;
                if (isTargetInConfiguredRange(getTrackedBoundingBox())) {
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
        flushExpiredPackets();
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
        flushExpiredPackets();
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

    private boolean isCriticalSyncPacket(Packet<?> packet) {
        if (packet instanceof S00PacketKeepAlive || packet instanceof S32PacketConfirmTransaction) return true;
        if (packet instanceof S12PacketEntityVelocity && mc.thePlayer != null) {
            return ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId();
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

    private void trackTruePosition(Packet<?> packet) {
        if (target == null || mc.theWorld == null) return;
        int entityId = -1;
        if (packet instanceof S14PacketEntity) {
            Entity entity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
            if (entity != null) entityId = entity.getEntityId();
        } else if (packet instanceof S18PacketEntityTeleport) {
            entityId = ((S18PacketEntityTeleport) packet).getEntityId();
        }
        if (entityId != target.getEntityId()) return;

        ITruePosition accessor = getTargetAccessor();
        if (accessor == null) return;
        positions.offer(new TimedPosition(new Vec3(accessor.getTrueX(), accessor.getTrueY(), accessor.getTrueZ()), System.currentTimeMillis()));
    }

    private double getTrueDistance() {
        ITruePosition accessor = getTargetAccessor();
        if (accessor != null && accessor.isTruePos() && mc.thePlayer != null) {
            return mc.thePlayer.getDistance(accessor.getTrueX(), accessor.getTrueY(), accessor.getTrueZ());
        }
        TimedPosition latest = getLatestPosition();
        if (latest == null || mc.thePlayer == null) return target.getDistanceToEntity(mc.thePlayer);
        return mc.thePlayer.getDistance(latest.position.xCoord, latest.position.yCoord, latest.position.zCoord);
    }

    private boolean isTargetInConfiguredRange(AxisAlignedBB box) {
        if (box == null) return false;
        double distance = RotationUtil.distanceToBox(box);
        float min = Math.min(minDistance.getValue(), maxDistance.getValue());
        float max = Math.max(minDistance.getValue(), maxDistance.getValue());
        return distance >= min && distance <= max;
    }

    private AxisAlignedBB getTrackedBoundingBox() {
        if (target == null) return null;
        TimedPosition position = getLatestPosition();
        if (position == null) return target.getEntityBoundingBox();
        return target.getEntityBoundingBox().offset(
                position.position.xCoord - target.posX,
                position.position.yCoord - target.posY,
                position.position.zCoord - target.posZ
        );
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
            ITruePosition accessor = getTargetAccessor();
            if (accessor != null && accessor.isTruePos()) {
                latest = new TimedPosition(new Vec3(accessor.getTrueX(), accessor.getTrueY(), accessor.getTrueZ()), System.currentTimeMillis());
            } else {
                latest = new TimedPosition(new Vec3(target.posX, target.posY, target.posZ), System.currentTimeMillis());
            }
        }
        return latest;
    }

    private ITruePosition getTargetAccessor() {
        return target instanceof ITruePosition ? (ITruePosition) target : null;
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
        if (packet == null || mc.getNetHandler() == null || mc.theWorld == null || mc.thePlayer == null) return;
        try {
            PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) packet);
        } catch (RuntimeException exception) {
            LOGGER.warn("Dropped unsafe delayed BackTrack packet {}", packet.getClass().getSimpleName(), exception);
        }
    }

    private void flushExpiredPackets() {
        long expiry = System.currentTimeMillis() - Math.max(MAX_QUEUE_TIME, Math.max(minMS.getValue(), maxMS.getValue()) + 250L);
        Iterator<QueuedPacket> packetIterator = packetQueue.iterator();
        while (packetIterator.hasNext()) {
            QueuedPacket data = packetIterator.next();
            if (data.time <= expiry) {
                receiveQueuedPacket(data.packet);
                packetIterator.remove();
            }
        }
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

    private int getSupposedDelay() {
        return mode.getValue() == 1 ? modernDelay : maxMS.getValue();
    }

    private void addBacktrackData(UUID id, double x, double y, double z, long time) {
        List<BacktrackData> data = backtrackedPlayer.get(id);
        if (data == null) {
            data = new ArrayList<>();
            backtrackedPlayer.put(id, data);
        }
        while (data.size() >= maximumCachedPositions.getValue()) {
            data.remove(0);
        }
        data.add(new BacktrackData(x, y, z, time));
    }

    private static class BacktrackData {
        private final double x;
        private final double y;
        private final double z;
        private final long time;

        BacktrackData(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
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
