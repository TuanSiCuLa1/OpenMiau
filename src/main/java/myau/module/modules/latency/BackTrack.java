package myau.module.modules.latency;

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
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
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
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Logger LOGGER = LogManager.getLogger("BackTrack");
    private static final String[] NON_DELAYED_SOUND_SUBSTRINGS = new String[]{"game.player.hurt", "game.player.die"};

    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"LEGACY", "MODERN", "FAKE_PLAYER"});

    public final IntProperty nextBacktrackDelay = new IntProperty("next-backtrack-delay", 0, 0, 2000, () -> mode.getValue() == 1);
    public final IntProperty minMS = new IntProperty("min-delay", 80, 0, 2000, () -> mode.getValue() == 1);
    public final IntProperty maxMS = new IntProperty("max-delay", 80, 0, 2000, () -> mode.getValue() == 1);

    public final ModeProperty style = new ModeProperty("style", 1, new String[]{"PULSE", "SMOOTH"}, () -> mode.getValue() == 1);
    public final FloatProperty minDistance = new FloatProperty("min-distance", 2.0F, 0.0F, 10.0F, () -> mode.getValue() == 1);
    public final FloatProperty maxDistance = new FloatProperty("max-distance", 3.0F, 0.0F, 10.0F, () -> mode.getValue() == 1);
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

    public EntityLivingBase target;
    private boolean shouldRender = true;
    private boolean ignoreWholeTick = false;
    private long delayForNextBacktrack = 0L;

    private int modernDelayValue = 80;
    private boolean modernDelayBoolean = false;

    private EntityOtherPlayerMP fakePlayer;
    private EntityLivingBase currentTarget;
    private boolean fakeShown;

    public BackTrack() {
        super("BackTrack", false);
    }

    private int getSupposedDelay() {
        return mode.getValue() == 1 ? modernDelayValue : maxMS.getValue();
    }

    @Override
    public void onEnabled() {
        reset();
        modernDelayValue = randomInt(minMS.getValue(), maxMS.getValue());
        modernDelayBoolean = false;
    }

    @Override
    public void onDisabled() {
        clearPackets(true, true);
        backtrackedPlayer.clear();
        removeFakePlayer();
        reset();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (mode.getValue() == 1) {
            clearPackets(false, true);
            target = null;
        }
        removeFakePlayer();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;

        if (mode.getValue() == 0) {
            Iterator<Map.Entry<UUID, List<BacktrackData>>> iterator = backtrackedPlayer.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, List<BacktrackData>> entry = iterator.next();
                entry.getValue().removeIf(data -> data.time + getSupposedDelay() < System.currentTimeMillis());
                if (entry.getValue().isEmpty()) iterator.remove();
            }
        }
        else if (mode.getValue() == 2) {
            updateFakePlayer();
            return;
        }

        if (mode.getValue() == 1) {
            if (shouldBacktrack() && target instanceof ITruePosition) {
                ITruePosition targetMixin = (ITruePosition) target;
                if (targetMixin.isTruePos()) {
                    double trueDist = mc.thePlayer.getDistance(targetMixin.getTrueX(), targetMixin.getTrueY(), targetMixin.getTrueZ());
                    double dist = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ);

                    if (trueDist <= 6f && (!smart.getValue() || trueDist >= dist) && (style.getValue() == 0 || !globalTimer.hasTimeElapsed(getSupposedDelay()))) {
                        shouldRender = true;

                        double currentClientDist = mc.thePlayer.getDistanceToEntity(target);
                        if (currentClientDist >= minDistance.getValue() && currentClientDist <= maxDistance.getValue()) {
                            handlePackets();
                        } else {
                            handlePacketsRange();
                        }
                    } else {
                        clear();
                    }
                }
            } else {
                clear();
            }
        }
        ignoreWholeTick = false;

        updateDelayCooldown();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) return;
        Packet<?> packet = event.getPacket();

        if (mode.getValue() == 0) {
            if (packet instanceof S0CPacketSpawnPlayer) {
                S0CPacketSpawnPlayer spawn = (S0CPacketSpawnPlayer) packet;
                addBacktrackData(spawn.getPlayer(), (double) spawn.getX() / 32.0D, (double) spawn.getY() / 32.0D, (double) spawn.getZ() / 32.0D, System.currentTimeMillis());
            } else if (legacyPos.getValue() == 1) {
                int id = -1;
                if (packet instanceof S14PacketEntity) id = ((S14PacketEntity) packet).getEntity(mc.theWorld) != null ? ((S14PacketEntity) packet).getEntity(mc.theWorld).getEntityId() : -1;
                else if (packet instanceof S18PacketEntityTeleport) id = ((S18PacketEntityTeleport) packet).getEntityId();

                if (id != -1 && mc.theWorld != null) {
                    Entity entity = mc.theWorld.getEntityByID(id);
                    if (entity instanceof ITruePosition) {
                        ITruePosition tp = (ITruePosition) entity;
                        addBacktrackData(entity.getUniqueID(), tp.getTrueX(), tp.getTrueY(), tp.getTrueZ(), System.currentTimeMillis());
                    }
                }
            }
            return;
        }

        if (mode.getValue() == 1) {
            if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
                clearPackets(true, false);
                return;
            }

            if (packetQueue.isEmpty() && !shouldBacktrack()) return;

            if (packet instanceof C00Handshake || packet instanceof C00PacketServerQuery || packet instanceof S02PacketChat || packet instanceof S01PacketPong) return;

            if (packet instanceof S29PacketSoundEffect) {
                String soundName = ((S29PacketSoundEffect) packet).getSoundName();
                for (String s : NON_DELAYED_SOUND_SUBSTRINGS) {
                    if (soundName.contains(s)) return;
                }
            }

            if (packet instanceof S06PacketUpdateHealth && ((S06PacketUpdateHealth) packet).getHealth() <= 0) {
                clearPackets(true, true);
                return;
            }

            if (packet instanceof S13PacketDestroyEntities && target != null) {
                for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                    if (id == target.getEntityId()) {
                        clearPackets(true, true);
                        reset();
                        return;
                    }
                }
            }

            if (packet instanceof S1CPacketEntityMetadata && target != null && ((S1CPacketEntityMetadata) packet).getEntityId() == target.getEntityId()) {
                if (isDeadMetadata((S1CPacketEntityMetadata) packet)) {
                    clearPackets(true, true);
                    reset();
                    return;
                }
                return;
            }

            if (packet instanceof S19PacketEntityStatus && target != null) {
                Entity entity = ((S19PacketEntityStatus) packet).getEntity(mc.theWorld);
                if (entity != null && entity.getEntityId() == target.getEntityId()) {
                    return;
                }
            }

            if (event.getType() == EventType.RECEIVE) {
                if (packet instanceof S14PacketEntity && target != null) {
                    Entity entity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
                    if (entity != null && entity.getEntityId() == target.getEntityId()) {
                        ITruePosition tp = (ITruePosition) target;
                        positions.add(new TimedPosition(new Vec3(tp.getTrueX(), tp.getTrueY(), tp.getTrueZ()), System.currentTimeMillis()));
                    }
                } else if (packet instanceof S18PacketEntityTeleport && target != null && ((S18PacketEntityTeleport) packet).getEntityId() == target.getEntityId()) {
                    ITruePosition tp = (ITruePosition) target;
                    positions.add(new TimedPosition(new Vec3(tp.getTrueX(), tp.getTrueY(), tp.getTrueZ()), System.currentTimeMillis()));
                }

                event.setCancelled(true);
                packetQueue.add(new QueuedPacket(packet, System.currentTimeMillis()));
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mode.getValue() == 2) {
            handleFakePlayerAttack(event);
            return;
        }

        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        if (target != event.getTarget()) {
            clearPackets(true, true);
            reset();
        }
        target = (EntityLivingBase) event.getTarget();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.getRenderManager() == null) return;

        if (mode.getValue() == 0) {
            renderLegacyPaths();
            return;
        }

        if (mode.getValue() == 1) {
            if (!shouldBacktrack() || !shouldRender || target == null) return;

            ITruePosition targetMixin = (ITruePosition) target;
            double x = targetMixin.getTrueX() - mc.getRenderManager().viewerPosX;
            double y = targetMixin.getTrueY() - mc.getRenderManager().viewerPosY;
            double z = targetMixin.getTrueZ() - mc.getRenderManager().viewerPosZ;

            if (targetMixin.isTruePos()) {
                Color color = new Color(espColor.getValue());

                switch (espMode.getValue()) {
                    case 1: 
                        AxisAlignedBB box = target.getEntityBoundingBox().offset(
                                targetMixin.getTrueX() - target.posX,
                                targetMixin.getTrueY() - target.posY,
                                targetMixin.getTrueZ() - target.posZ
                        );
                        drawBacktrackBox(box, color);
                        break;
                    case 2: 
                        GlStateManager.pushMatrix();
                        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                        GlStateManager.color(0.6F, 0.6F, 0.6F, 1.0F);
                        mc.getRenderManager().doRenderEntity(target, x, y, z,
                                target.prevRotationYaw + (target.rotationYaw - target.prevRotationYaw) * event.getPartialTicks(),
                                event.getPartialTicks(), true);
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

                        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
                        mc.getRenderManager().doRenderEntity(target, x, y, z,
                                target.prevRotationYaw + (target.rotationYaw - target.prevRotationYaw) * event.getPartialTicks(),
                                event.getPartialTicks(), true);

                        GL11.glPopAttrib();
                        GlStateManager.popMatrix();
                        break;
                }
            }
        }
    }

    private void handlePackets() {
        long delay = getSupposedDelay();
        packetQueue.removeIf(queuedPacket -> {
            if (queuedPacket.time <= System.currentTimeMillis() - delay) {
                receiveQueuedPacket(queuedPacket.packet);
                return true;
            }
            return false;
        });

        positions.removeIf(pos -> pos.time < System.currentTimeMillis() - delay);
    }

    private void handlePacketsRange() {
        long time = getRangeTime();
        if (time == -1L) {
            clearPackets(true, true);
            return;
        }

        packetQueue.removeIf(queuedPacket -> {
            if (queuedPacket.time <= time) {
                receiveQueuedPacket(queuedPacket.packet);
                return true;
            }
            return false;
        });

        positions.removeIf(pos -> pos.time < time);
    }

    private long getRangeTime() {
        if (target == null) return -1L;
        long time = 0L;
        boolean found = false;

        for (TimedPosition data : positions) {
            time = data.time;
            AxisAlignedBB targetBox = target.getEntityBoundingBox().offset(
                    data.position.xCoord - target.posX,
                    data.position.yCoord - target.posY,
                    data.position.zCoord - target.posZ
            );

            double distance = RotationUtil.distanceToBox(targetBox);
            if (distance >= minDistance.getValue() && distance <= maxDistance.getValue()) {
                found = true;
                break;
            }
        }
        return found ? time : -1L;
    }

    private void clearPackets(boolean handlePackets, boolean stopRendering) {
        packetQueue.removeIf(queuedPacket -> {
            if (handlePackets) {
                receiveQueuedPacket(queuedPacket.packet);
            }
            return true;
        });

        positions.clear();

        if (stopRendering) {
            shouldRender = false;
            ignoreWholeTick = true;
        }
    }

    private void updateDelayCooldown() {
        boolean shouldChangeDelay = packetQueue.isEmpty();
        if (!shouldChangeDelay) {
            modernDelayBoolean = false;
        }
        if (shouldChangeDelay && !modernDelayBoolean && !shouldBacktrack()) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay.getValue();
            modernDelayValue = randomInt(minMS.getValue(), maxMS.getValue());
            modernDelayBoolean = true;
        }
    }

    private void clear() {
        clearPackets(true, true);
        globalTimer.reset();
    }

    private void reset() {
        target = null;
        globalTimer.reset();
    }

    private boolean shouldBacktrack() {
        return mc.thePlayer != null && mc.theWorld != null && target != null
                && mc.thePlayer.getHealth() > 0
                && (Float.isNaN(target.getHealth()) || target.getHealth() > 0)
                && mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR
                && System.currentTimeMillis() >= delayForNextBacktrack
                && mc.thePlayer.ticksExisted > 20
                && !ignoreWholeTick;
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
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void receiveQueuedPacket(Packet<?> packet) {
        if (packet == null || mc.getNetHandler() == null || mc.theWorld == null || mc.thePlayer == null) return;
        try {
            PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) packet);
        } catch (RuntimeException exception) {
            LOGGER.warn("Dropped unsafe delayed BackTrack packet {}", packet.getClass().getSimpleName(), exception);
        }
    }

    private void drawBacktrackBox(AxisAlignedBB box, Color color) {
        GlStateManager.pushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        RenderGlobal.drawOutlinedBoundingBox(
                new AxisAlignedBB(
                        box.minX - mc.getRenderManager().viewerPosX, box.minY - mc.getRenderManager().viewerPosY, box.minZ - mc.getRenderManager().viewerPosZ,
                        box.maxX - mc.getRenderManager().viewerPosX, box.maxY - mc.getRenderManager().viewerPosY, box.maxZ - mc.getRenderManager().viewerPosZ
                ),
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()
        );
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GlStateManager.popMatrix();
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
                GL11.glVertex3d(point.x - mc.getRenderManager().viewerPosX, point.y - mc.getRenderManager().viewerPosY, point.z - mc.getRenderManager().viewerPosZ);
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

    private void addBacktrackData(UUID id, double x, double y, double z, long time) {
        List<BacktrackData> data = backtrackedPlayer.computeIfAbsent(id, k -> new ArrayList<>());
        while (data.size() >= maximumCachedPositions.getValue()) {
            data.remove(0);
        }
        data.add(new BacktrackData(x, y, z, time));
    }

    private static int randomInt(int min, int max) {
        return RandomUtil.nextInt(Math.min(min, max), Math.max(min, max));
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
        if (fakePlayer != null && mc.theWorld != null) mc.theWorld.removeEntity(fakePlayer);
        fakePlayer = null;
        currentTarget = null;
        fakeShown = false;
    }

    private void handleFakePlayerAttack(AttackEvent event) {
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase attacked = (EntityLivingBase) event.getTarget();

        if (fakePlayer != null && attacked.getEntityId() == fakePlayer.getEntityId()) {
            if (currentTarget != null) {
                mc.thePlayer.swingItem();
                PacketUtil.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                if (mc.playerController != null) mc.thePlayer.attackTargetEntityWithCurrentItem(currentTarget);
            }
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

        if (mc.thePlayer.ticksExisted % Math.max(fakePlayerIntavePackets.getValue(), 1) == 0 || fakePulseTimer.hasTimeElapsed(fakePlayerPulseDelay.getValue())) {
            fakePlayer.rotationYawHead = currentTarget.rotationYawHead;
            fakePlayer.renderYawOffset = currentTarget.renderYawOffset;
            fakePlayer.copyLocationAndAnglesFrom(currentTarget);
            fakePulseTimer.reset();
        }
    }

    private void copyEquipment(EntityLivingBase src, EntityLivingBase dst) {
        for (int i = 0; i <= 4; i++) dst.setCurrentItemOrArmor(i, src.getEquipmentInSlot(i) == null ? null : src.getEquipmentInSlot(i).copy());
    }

    private static class BacktrackData {
        private final double x, y, z;
        private final long time;
        BacktrackData(double x, double y, double z, long time) { this.x = x; this.y = y; this.z = z; this.time = time; }
    }

    private static class QueuedPacket {
        private final Packet<?> packet;
        private final long time;
        QueuedPacket(Packet<?> packet, long time) { this.packet = packet; this.time = time; }
    }

    private static class TimedPosition {
        private final Vec3 position;
        private final long time;
        TimedPosition(Vec3 position, long time) { this.position = position; this.time = time; }
    }
}