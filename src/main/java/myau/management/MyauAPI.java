package myau.management;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MyauAPI {
    public static final String APIBASE = "https://api.rinbounce.wtf/";
    public static final String BRANCH = "myau";

    private static final String API_V1 = APIBASE + "api/v1";
    private static final int TIMEOUT_MS = 10000;

    public static String listOnlineConfigs() throws Exception { return get(clientPath("settings"), "Miau/OnlineConfig"); }
    public static String loadOnlineConfig(String settingId) throws Exception { return get(clientPath("settings/" + encode(settingId)), "Miau/OnlineConfig"); }
    public static String listUserConfigs() throws Exception { return get(clientPath("user-configs"), "Miau/UserConfig"); }
    public static String loadUserConfig(String configId) throws Exception { return get(clientPath("user-configs/" + encode(configId)), "Miau/UserConfig"); }
    public static String rpcConfig() throws Exception { return get(APIBASE + "api/rpc", "Miau/RPC"); }
    public static String publishPresence(String json) throws Exception { return post(clientPath("presence"), json, "Miau/Presence"); }
    public static String encode(String value) throws Exception { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }

    private static String clientPath(String path) throws Exception { return API_V1 + "/client/" + encode(BRANCH) + "/" + path; }
    private static String get(String url, String userAgent) throws Exception { return request("GET", url, null, userAgent); }
    private static String post(String url, String body, String userAgent) throws Exception { return request("POST", url, body, userAgent); }

    private static String request(String method, String url, String body, String userAgent) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", userAgent);
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                try (OutputStream outputStream = connection.getOutputStream()) { outputStream.write(bytes); }
            }
            int code = connection.getResponseCode();
            String response = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (code < 200 || code >= 300) throw new Exception(formatError(code, response));
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
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
            return builder.toString();
        }
    }

    private static String formatError(int code, String body) {
        String text = body == null ? "" : body.replaceAll("(?is)<style.*?</style>", " ").replaceAll("(?is)<script.*?</script>", " ").replaceAll("(?is)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() > 180) text = text.substring(0, 180) + "...";
        return text.isEmpty() ? "HTTP " + code : "HTTP " + code + ": " + text;
    }
}