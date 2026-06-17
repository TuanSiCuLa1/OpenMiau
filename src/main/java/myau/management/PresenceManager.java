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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PresenceManager {
    private static final String API_BASE = "https://api.rinbounce.wtf/api/v1/client/myau/presence";
    private static final int TIMEOUT_MS = 5000;
    private static final long PUBLISH_INTERVAL_MS = 30000L;
    private static final long REFRESH_INTERVAL_MS = 15000L;
    private static final long STALE_AFTER_MS = 45000L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private volatile Set<String> miauPlayers = Collections.emptySet();
    private volatile long lastPublish;
    private volatile long lastRefresh;
    private volatile long lastSuccessfulRefresh;
    private volatile boolean publishing;
    private volatile boolean refreshing;

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || this.mc.thePlayer == null || this.mc.theWorld == null) {
            return;
        }

        String server = this.getServerAddress();
        if (server == null) {
            this.clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastSuccessfulRefresh > STALE_AFTER_MS) {
            this.miauPlayers = Collections.emptySet();
        }
        if (now - this.lastPublish >= PUBLISH_INTERVAL_MS && !this.publishing) {
            this.lastPublish = now;
            this.publishAsync(server);
        }
        if (now - this.lastRefresh >= REFRESH_INTERVAL_MS && !this.refreshing) {
            this.lastRefresh = now;
            this.refreshAsync(server);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clear();
        this.lastPublish = 0L;
        this.lastRefresh = 0L;
    }

    public boolean isMiauPlayer(String uuid, String username) {
        Set<String> players = this.miauPlayers;
        String uuidKey = normalizeKey(uuid);
        if (uuidKey != null && players.contains(uuidKey)) {
            return true;
        }
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
                request("POST", API_BASE, body.toString());
            } catch (Exception ignored) {
            } finally {
                this.publishing = false;
            }
        }, "Miau Presence Publish");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshAsync(final String server) {
        this.refreshing = true;
        Thread thread = new Thread(() -> {
            try {
                String response = request("GET", API_BASE + "?server=" + encode(server), null);
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
                this.lastSuccessfulRefresh = System.currentTimeMillis();
            } catch (Exception ignored) {
            } finally {
                this.refreshing = false;
            }
        }, "Miau Presence Refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private String getServerAddress() {
        ServerData serverData = this.mc.getCurrentServerData();
        if (serverData == null || serverData.serverIP == null) {
            return null;
        }
        String server = serverData.serverIP.trim().toLowerCase(Locale.ROOT);
        return server.isEmpty() ? null : server;
    }

    private void clear() {
        this.miauPlayers = Collections.emptySet();
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

    private static String request(String method, String url, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Miau/Presence");
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(bytes);
                }
            }

            int code = connection.getResponseCode();
            String response = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + ": " + response);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }
}
