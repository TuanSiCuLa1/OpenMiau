package myau.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.Myau;
import myau.module.Module;
import myau.module.modules.RPC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class DiscordRichPresence {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private RandomAccessFile pipe;
    private boolean running;
    private long startTimestamp;
    private long lastUpdate;
    private long lastConfigFetch;
    private RpcConfig config = new RpcConfig();

    public boolean isRunning() {
        return this.running;
    }

    public void start(RPC rpc) {
        if (this.running || rpc == null) {
            return;
        }
        this.refreshConfig(rpc, true);
        String clientId = this.config.clientId.trim();
        if (clientId.isEmpty() || clientId.equals("0") || !this.config.enabled) {
            return;
        }
        try {
            this.pipe = this.openPipe();
            this.running = true;
            this.startTimestamp = Instant.now().getEpochSecond();
            this.lastUpdate = 0L;
            this.write(OP_HANDSHAKE, "{\"v\":1,\"client_id\":\"" + escape(clientId) + "\"}");
            this.update(rpc, true);
        } catch (Throwable ignored) {
            this.stop();
        }
    }

    public void stop() {
        this.running = false;
        if (this.pipe != null) {
            try {
                this.write(OP_CLOSE, "{}");
            } catch (Throwable ignored) {
            }
            try {
                this.pipe.close();
            } catch (IOException ignored) {
            }
            this.pipe = null;
        }
    }

    public void update(RPC rpc) {
        this.update(rpc, false);
    }

    private void update(RPC rpc, boolean force) {
        if (!this.running || this.pipe == null || rpc == null) {
            return;
        }
        this.refreshConfig(rpc, false);
        if (!this.config.enabled) {
            this.stop();
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - this.lastUpdate < 1000L) {
            return;
        }
        this.lastUpdate = now;

        try {
            String nonce = UUID.randomUUID().toString();
            String payload = "{"
                    + "\"cmd\":\"SET_ACTIVITY\","
                    + "\"args\":{"
                    + "\"pid\":" + getPid() + ","
                    + "\"activity\":" + this.buildActivity(rpc)
                    + "},"
                    + "\"nonce\":\"" + nonce + "\""
                    + "}";
            this.write(OP_FRAME, payload);
        } catch (Throwable ignored) {
            this.stop();
        }
    }

    private String buildActivity(RPC rpc) {
        String details = this.config.details.trim();
        if (rpc.showServer.getValue()) {
            details = this.getServerText();
        } else if (details.isEmpty()) {
            details = "Playing Myau Client";
        }

        String state = rpc.showModulesCount.getValue() ? this.getModulesText() : this.config.state.trim();

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"details\":\"").append(escape(details)).append("\",");
        if (!state.isEmpty()) {
            builder.append("\"state\":\"").append(escape(state)).append("\",");
        }
        builder.append("\"timestamps\":{\"start\":").append(this.startTimestamp).append("}");

        String largeImage = this.config.largeImage.trim();
        String largeText = this.config.largeImageText.trim();
        String smallImage = this.getSmallImage();
        String smallText = this.getSmallImageText();
        if (!largeImage.isEmpty() || !smallImage.isEmpty()) {
            builder.append(",\"assets\":{");
            boolean hasAsset = false;
            if (!largeImage.isEmpty()) {
                builder.append("\"large_image\":\"").append(escape(largeImage)).append("\"");
                hasAsset = true;
                if (!largeText.isEmpty()) {
                    builder.append(",\"large_text\":\"").append(escape(largeText)).append("\"");
                }
            }
            if (!smallImage.isEmpty()) {
                if (hasAsset) {
                    builder.append(",");
                }
                builder.append("\"small_image\":\"").append(escape(smallImage)).append("\"");
                if (!smallText.isEmpty()) {
                    builder.append(",\"small_text\":\"").append(escape(smallText)).append("\"");
                }
            }
            builder.append("}");
        }
        builder.append("}");
        return builder.toString();
    }

    private void refreshConfig(RPC rpc, boolean force) {
        if (rpc == null) {
            this.config = new RpcConfig();
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - this.lastConfigFetch < 30000L) {
            return;
        }
        this.lastConfigFetch = now;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(rpc.getConfigUrl()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) {
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JsonObject root = new JsonParser().parse(response.toString()).getAsJsonObject();
            JsonObject rpcObject = root.has("rpc") ? root.getAsJsonObject("rpc") : root;
            RpcConfig next = new RpcConfig();
            next.enabled = getBoolean(rpcObject, "enabled", true);
            next.clientId = getString(rpcObject, "clientId", "0");
            next.details = getString(rpcObject, "details", "Playing Myau Client");
            next.state = getString(rpcObject, "state", "");
            next.largeImage = getString(rpcObject, "largeImage", "logo");
            next.largeImageText = getString(rpcObject, "largeImageText", "Myau Client");
            next.smallImage = getString(rpcObject, "smallImage", "steve");
            next.smallImageText = getString(rpcObject, "smallImageText", "Minecraft Player");
            next.showServer = getBoolean(rpcObject, "showServer", true);
            next.showModulesCount = getBoolean(rpcObject, "showModulesCount", true);
            this.config = next;
        } catch (Throwable ignored) {
        }
    }

    private String getServerText() {
        ServerData data = mc.getCurrentServerData();
        if (mc.isIntegratedServerRunning() || data == null) {
            return "Server: Singleplayer";
        }
        return "Server: " + data.serverIP;
    }

    private String getModulesText() {
        int enabled = 0;
        int total = 0;
        for (Module module : Myau.moduleManager.modules.values()) {
            total++;
            if (module.isEnabled()) {
                enabled++;
            }
        }
        return "Enabled " + enabled + " of " + total + " modules";
    }

    private RandomAccessFile openPipe() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            for (int i = 0; i < 10; i++) {
                File pipeFile = new File("\\\\?\\pipe\\discord-ipc-" + i);
                if (pipeFile.exists()) {
                    return new RandomAccessFile(pipeFile, "rw");
                }
            }
            throw new IOException("Discord IPC pipe not found");
        }

        String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
        String tmpDir = System.getProperty("java.io.tmpdir");
        String[] dirs = new String[]{runtimeDir, tmpDir, "/tmp"};
        for (String dir : dirs) {
            if (dir == null) continue;
            for (int i = 0; i < 10; i++) {
                File pipeFile = new File(dir, "discord-ipc-" + i);
                if (pipeFile.exists()) {
                    return new RandomAccessFile(pipeFile, "rw");
                }
            }
        }
        throw new IOException("Discord IPC pipe not found");
    }

    private void write(int opCode, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opCode);
        header.putInt(data.length);
        this.pipe.write(header.array());
        this.pipe.write(data);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static long getPid() {
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int index = name.indexOf('@');
        if (index > 0) {
            try {
                return Long.parseLong(name.substring(0, index));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private String getSmallImage() {
        if (mc.thePlayer != null && mc.thePlayer.getName() != null && !mc.thePlayer.getName().trim().isEmpty()) {
            return "https://minotar.net/avatar/" + escapeUrl(mc.thePlayer.getName()) + "/128.png";
        }
        return this.config.smallImage.trim();
    }

    private String getSmallImageText() {
        if (mc.thePlayer != null && mc.thePlayer.getName() != null && !mc.thePlayer.getName().trim().isEmpty()) {
            return mc.thePlayer.getName();
        }
        return this.config.smallImageText.trim();
    }

    private static String escapeUrl(String value) {
        return value.replace(" ", "%20");
    }

    private static String getString(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : fallback;
    }

    private static class RpcConfig {
        private boolean enabled = true;
        private String clientId = "0";
        private String details = "Playing Myau Client";
        private String state = "";
        private String largeImage = "logo";
        private String largeImageText = "Myau Client";
        private String smallImage = "steve";
        private String smallImageText = "Minecraft Player";
        private boolean showServer = true;
        private boolean showModulesCount = true;
    }
}
