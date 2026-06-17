package myau.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

public final class MyauClientAuth {
    private static final long REFRESH_BEFORE_EXPIRY_MS = 5 * 60_000L;
    private static final long FAILURE_BACKOFF_MS = 60_000L;
    private static final SecureRandom RNG = new SecureRandom();


    private static final byte[] __k = { 0x3F, 0x7A, 0x12, 0x5E, 0x44, (byte)0x8B, 0x29, (byte)0xA1 };
    private static final byte[] __s = {
        0x47, 0x1D, 0x63, 0x27, 0x75, (byte)0xFF, 0x43, (byte)0xC2,
        0x52, 0x08, 0x62, 0x31, 0x25, (byte)0xF8, 0x5C, (byte)0xC8,
        0x54, 0x4D, 0x2B, 0x29, 0x26, (byte)0xB8, 0x45, (byte)0xC4,
        0x59, 0x0C, 0x2A, 0x36, 0x71, (byte)0xB9, 0x4D, (byte)0x97,
        0x0F, 0x14, 0x26, 0x24
    };

    private static String __cs() {
        byte[] d = new byte[__s.length];
        for (int i = 0; i < __s.length; i++) d[i] = (byte)(__s[i] ^ __k[i % __k.length]);
        return new String(d, StandardCharsets.US_ASCII);
    }

    private static final Object LOCK = new Object();
    private static volatile String cachedToken;
    private static volatile long cachedExpiresAt;
    private static volatile String cachedUid;
    private static volatile long nextAttemptAt;

    private MyauClientAuth() {
    }

    public static String getToken() {
        long now = System.currentTimeMillis();

        String existing = cachedToken;
        if (existing != null && cachedExpiresAt - now > REFRESH_BEFORE_EXPIRY_MS) {
            return existing;
        }

        synchronized (LOCK) {
            now = System.currentTimeMillis();
            existing = cachedToken;
            if (existing != null && cachedExpiresAt - now > REFRESH_BEFORE_EXPIRY_MS) {
                return existing;
            }
            if (now < nextAttemptAt) {
                return null;
            }

            try {
                Handshake handshake = handshake();
                if (handshake == null) {
                    nextAttemptAt = now + FAILURE_BACKOFF_MS;
                    return null;
                }
                cachedToken = handshake.token;
                cachedExpiresAt = handshake.expiresAt;
                cachedUid = handshake.uid;
                nextAttemptAt = 0L;
                return handshake.token;
            } catch (Exception e) {
                nextAttemptAt = now + FAILURE_BACKOFF_MS;
                return null;
            }
        }
    }

    public static void invalidate() {
        synchronized (LOCK) {
            cachedToken = null;
            cachedExpiresAt = 0L;
            cachedUid = null;
        }
    }

    public static String getCachedUid() {
        return cachedUid;
    }

    private static Handshake handshake() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Session session = mc.getSession();
        if (session == null) return null;

        GameProfile profile = session.getProfile();
        if (profile == null || profile.getName() == null) return null;

        String accessToken = session.getToken();
        boolean isCracked = accessToken == null || accessToken.isEmpty() || "0".equals(accessToken);

        if (isCracked) {
            return crackedHandshake(profile.getName());
        }

        if (profile.getId() == null) return null;

        MinecraftSessionService sessionService = mc.getSessionService();
        if (sessionService == null) return null;

        String serverId = randomServerId();
        try {
            sessionService.joinServer(profile, accessToken, serverId);
        } catch (AuthenticationException e) {
            return null;
        }

        String uid = profile.getId().toString().replace("-", "");
        String username = profile.getName();

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("uid", uid);
        body.addProperty("serverId", serverId);

        String response = MyauAPI.requestAuthHandshake(body.toString());
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        if (!json.has("token") || !json.has("expiresAt")) return null;

        Handshake h = new Handshake();
        h.token = json.get("token").getAsString();
        h.expiresAt = json.get("expiresAt").getAsLong();
        h.uid = uid;
        return h;
    }

    /**
     * Auth path for cracked (offline-mode) users.
     * Computes the offline UUID the same way Minecraft does:
     *   UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(UTF_8))
     * then sends the shared client secret to the backend instead of a Mojang serverId.
     */
    private static Handshake crackedHandshake(String username) throws Exception {
        UUID offlineId = UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        String uid = offlineId.toString().replace("-", "");

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("uid", uid);
        body.addProperty("cracked", true);
        body.addProperty("clientSecret", __cs());

        String response = MyauAPI.requestAuthHandshake(body.toString());
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        if (!json.has("token") || !json.has("expiresAt")) return null;

        Handshake h = new Handshake();
        h.token = json.get("token").getAsString();
        h.expiresAt = json.get("expiresAt").getAsLong();
        h.uid = uid;
        return h;
    }

    private static String randomServerId() {
        return UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(RNG.nextLong() & Long.MAX_VALUE);
    }

    private static final class Handshake {
        String token;
        long expiresAt;
        String uid;
    }
}