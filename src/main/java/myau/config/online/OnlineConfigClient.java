package myau.config.online;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OnlineConfigClient {
    private static final String API_BASE = "https://api.rinbounce.wtf/api/v1";
    private static final String BRANCH = "myau";
    private static final int TIMEOUT_MS = 10000;
    private static final Gson GSON = new Gson();

    public List<OnlineConfigEntry> list() throws Exception {
        JsonElement element = new JsonParser().parse(get("settings"));
        if (!element.isJsonArray()) {
            return Collections.emptyList();
        }
        OnlineConfigEntry[] entries = GSON.fromJson(element, OnlineConfigEntry[].class);
        return entries == null ? Collections.emptyList() : Arrays.asList(entries);
    }

    public String load(String settingId) throws Exception {
        return get("settings/" + encode(settingId));
    }

    private static String get(String path) throws Exception {
        String url = API_BASE + "/client/" + encode(BRANCH) + "/" + path;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Miau/OnlineConfig");

            int code = connection.getResponseCode();
            String body = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new Exception(formatError(code, body));
            }
            return body;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    private static String formatError(int code, String body) {
        String text = body == null ? ""
                : body
                        .replaceAll("(?is)<style.*?</style>", " ")
                        .replaceAll("(?is)<script.*?</script>", " ")
                        .replaceAll("(?is)<[^>]+>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
        if (text.length() > 180) {
            text = text.substring(0, 180) + "...";
        }
        return text.isEmpty() ? "HTTP " + code : "HTTP " + code + ": " + text;
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }
}
