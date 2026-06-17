package myau.management;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.ClientInfo;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PresenceManager {
    private static final long PUBLISH_INTERVAL_MS = 30000L;
    private static final long STALE_AFTER_MS = 45000L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private volatile Set<String> miauPlayers = Collections.emptySet();
    private volatile long lastPublish;
    private volatile long lastSuccessfulRefresh;
    private volatile int onlineCount;
    private volatile boolean publishing;

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || this.mc.thePlayer == null || this.mc.theWorld == null) return;
        String server = this.getServerAddress();
        if (server == null) {
            this.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastSuccessfulRefresh > STALE_AFTER_MS) this.miauPlayers = Collections.emptySet();
        if (now - this.lastPublish >= PUBLISH_INTERVAL_MS && !this.publishing) {
            this.lastPublish = now;
            this.publishAsync(server);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clear();
        this.lastPublish = 0L;
    }

    public int getOnlineCount() { return this.onlineCount; }

    public boolean isMiauPlayer(String uuid, String username) {
        if (myau.Myau.peerDetector != null && myau.Myau.peerDetector.isMiauPeer(username)) return true;
        Set<String> players = this.miauPlayers;
        String uuidKey = normalizeKey(uuid);
        if (uuidKey != null && players.contains(uuidKey)) return true;
        String nameKey = normalizeKey(username);
        return nameKey != null && players.contains(nameKey);
    }

    private void publishAsync(final String server) {
        this.publishing = true;
        Thread thread = new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("server", server);
                body.addProperty("username", this.mc.getSession().getUsername());
                body.addProperty("uuid", this.mc.getSession().getPlayerID());
                body.addProperty("version", ClientInfo.VERSION);
                this.updateFromResponse(MyauAPI.publishPresence(body.toString()));
            } catch (Exception ignored) {
            } finally {
                this.publishing = false;
            }
        }, "Miau Presence Publish");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateFromResponse(String response) {
        JsonObject root = new JsonParser().parse(response).getAsJsonObject();
        JsonArray players = root.has("players") && root.get("players").isJsonArray() ? root.getAsJsonArray("players") : new JsonArray();
        Set<String> next = new HashSet<>();
        for (JsonElement element : players) {
            if (!element.isJsonObject()) continue;
            JsonObject player = element.getAsJsonObject();
            addKey(next, player, "uuid");
            addKey(next, player, "username");
        }
        this.miauPlayers = next;
        this.onlineCount = players.size();
        this.lastSuccessfulRefresh = System.currentTimeMillis();
    }

    private String getServerAddress() {
        ServerData serverData = this.mc.getCurrentServerData();
        if (serverData == null || serverData.serverIP == null) return null;
        String server = serverData.serverIP.trim().toLowerCase(Locale.ROOT);
        return server.isEmpty() ? null : server;
    }

    private void clear() {
        this.miauPlayers = Collections.emptySet();
        this.onlineCount = 0;
        this.lastSuccessfulRefresh = 0L;
    }

    private static void addKey(Set<String> keys, JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) return;
        String key = normalizeKey(object.get(field).getAsString());
        if (key != null) keys.add(key);
    }

    private static String normalizeKey(String value) {
        if (value == null) return null;
        String key = value.replace("-", "").trim().toLowerCase(Locale.ROOT);
        return key.isEmpty() ? null : key;
    }
}