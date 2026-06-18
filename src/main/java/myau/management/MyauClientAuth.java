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
        0x4A, 0x1E, 0x65, 0x15, 0x08, (byte)0xE2, 0x53, (byte)0xE5,
        0x69, 0x1E, 0x26, 0x08, 0x76, (byte)0xEE, 0x7C, (byte)0xD6,
        0x6D, 0x1B, 0x66, 0x30, 0x69, (byte)0xB2, 0x7F, (byte)0xD2,
        0x71, 0x32, 0x3F, 0x10, 0x31, (byte)0xCA, 0x61, (byte)0xF6,
        0x69, 0x43, 0x55, 0x39, 0x21, (byte)0xBC, 0x1E, (byte)0x99,
        0x0E, 0x4A, 0x26, 0x73, 0x32, (byte)0xBD, 0x79, (byte)0x95,
        0x72, 0x19, 0x43, 0x1F, 0x00, (byte)0xBA, 0x4B, (byte)0xFB,
        0x07, 0x09, 0x4B, 0x09, 0x2B, (byte)0xEE, 0x71, (byte)0xD8
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
    private static volatile String cachedUsername;
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
            if (existing != null && !isCachedAccountCurrent()) {
                cachedToken = null;
                cachedExpiresAt = 0L;
                cachedUid = null;
                cachedUsername = null;
                existing = null;
            }
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
                cachedUsername = handshake.username;
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
            cachedUsername = null;
        }
    }

    public static String getCachedUid() {
        return cachedUid;
    }

    public static String getAuthenticatedUid() {
        String token = getToken();
        return token == null ? null : cachedUid;
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
        h.username = json.has("username") ? json.get("username").getAsString() : username;
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
        h.username = json.has("username") ? json.get("username").getAsString() : username;
        return h;
    }

    private static boolean isCachedAccountCurrent() {
        String username = cachedUsername;
        if (username == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        Session session = mc.getSession();
        return session != null && username.equalsIgnoreCase(session.getUsername());
    }

    private static String randomServerId() {
        return UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(RNG.nextLong() & Long.MAX_VALUE);
    }

    private static final class Handshake {
        String token;
        long expiresAt;
        String uid;
        String username;
    }
}
