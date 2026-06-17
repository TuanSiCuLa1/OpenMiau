package myau.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.Session;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.Minecraft;

public class MiauPeerDetector {

    private static final String CHANNEL = "miau:detect";
    private static final String REGISTER_CHANNEL = "REGISTER";
    private static final long HELLO_INTERVAL_MS = 15_000L;

    private static final byte[] __hk = { 0x1A, (byte)0xC3, 0x77, 0x4F, (byte)0xB2, 0x08, (byte)0xE5, 0x3D };
    private static final byte[] __hs = {
        0x7B, (byte)0xB6, 0x17, 0x2A, (byte)0xD7, 0x6C, (byte)0x8E, 0x4E,
        0x74, (byte)0xA4, 0x05, 0x3F, (byte)0xC4, 0x7B, (byte)0x97, 0x5A,
        0x6C, (byte)0xBB, 0x13, 0x22, (byte)0xCB, 0x68, (byte)0x88, 0x53
    };

    private static String __hmk() {
        byte[] d = new byte[__hs.length];
        for (int i = 0; i < __hs.length; i++) d[i] = (byte)(__hs[i] ^ __hk[i % __hk.length]);
        return new String(d, StandardCharsets.US_ASCII);
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> miauPeers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private volatile long lastHello = 0L;

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        miauPeers.clear();
        registered.set(false);
        lastHello = 0L;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        if (!registered.get()) {
            sendRegister();
            registered.set(true);
        }

        long now = System.currentTimeMillis();
        if (now - lastHello >= HELLO_INTERVAL_MS) {
            lastHello = now;
            sendHello();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (!(event.getPacket() instanceof S3FPacketCustomPayload)) return;

        S3FPacketCustomPayload pkt = (S3FPacketCustomPayload) event.getPacket();
        if (!CHANNEL.equals(pkt.getChannelName())) return;

        try {
            byte[] data = new byte[pkt.getBufferData().readableBytes()];
            pkt.getBufferData().readBytes(data);
            String json = new String(data, StandardCharsets.UTF_8);
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();

            String username = obj.get("u").getAsString();
            long ts = obj.get("t").getAsLong();
            String sig = obj.get("s").getAsString();

            if (Math.abs(System.currentTimeMillis() - ts) > 60_000L) return;

            String expected = hmac(username + ":" + ts);
            if (expected == null || !expected.equals(sig)) return;

            if (mc.thePlayer != null && !username.equals(mc.thePlayer.getGameProfile().getName())) {
                miauPeers.add(username.toLowerCase());
            }
        } catch (Exception ignored) {}
    }

    public boolean isMiauPeer(String username) {
        if (username == null) return false;
        return miauPeers.contains(username.toLowerCase());
    }

    public Set<String> getPeers() {
        return Collections.unmodifiableSet(miauPeers);
    }

    private void sendRegister() {
        try {
            byte[] channelBytes = (CHANNEL + "\0").getBytes(StandardCharsets.UTF_8);
            PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(channelBytes));
            mc.getNetHandler().getNetworkManager().sendPacket(new C17PacketCustomPayload(REGISTER_CHANNEL, buf));
        } catch (Exception ignored) {}
    }

    private void sendHello() {
        try {
            Session session = mc.getSession();
            if (session == null) return;
            String username = session.getUsername();
            long ts = System.currentTimeMillis();
            String sig = hmac(username + ":" + ts);
            if (sig == null) return;

            JsonObject obj = new JsonObject();
            obj.addProperty("u", username);
            obj.addProperty("t", ts);
            obj.addProperty("s", sig);

            byte[] payload = obj.toString().getBytes(StandardCharsets.UTF_8);
            PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(payload));
            mc.getNetHandler().getNetworkManager().sendPacket(new C17PacketCustomPayload(CHANNEL, buf));
        } catch (Exception ignored) {}
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(__hmk().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16);
        } catch (Exception e) {
            return null;
        }
    }
}
