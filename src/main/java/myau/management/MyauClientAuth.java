package myau.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.security.SecureRandom;
import java.util.UUID;

public final class MyauClientAuth {
    private static final long REFRESH_BEFORE_EXPIRY_MS = 5 * 60_000L;
    private static final long FAILURE_BACKOFF_MS = 60_000L;
    private static final SecureRandom RNG = new SecureRandom();

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
        if (profile == null || profile.getId() == null || profile.getName() == null) return null;

        String accessToken = session.getToken();
        if (accessToken == null || accessToken.isEmpty() || "0".equals(accessToken)) {
            return null;
        }

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