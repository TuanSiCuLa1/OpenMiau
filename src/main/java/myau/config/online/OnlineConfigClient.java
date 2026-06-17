package myau.config.online;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import myau.management.MyauAPI;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OnlineConfigClient {
    private static final Gson GSON = new Gson();

    public List<OnlineConfigEntry> list() throws Exception {
        return parseEntries(MyauAPI.listOnlineConfigs());
    }

    public String load(String settingId) throws Exception {
        return MyauAPI.loadOnlineConfig(settingId);
    }

    public List<OnlineConfigEntry> listUserConfigs() throws Exception {
        return parseEntries(MyauAPI.listUserConfigs());
    }

    public String loadUserConfig(String configId) throws Exception {
        return MyauAPI.loadUserConfig(configId);
    }

    private List<OnlineConfigEntry> parseEntries(String json) {
        JsonElement element = new JsonParser().parse(json);
        if (!element.isJsonArray()) {
            return Collections.emptyList();
        }
        OnlineConfigEntry[] entries = GSON.fromJson(element, OnlineConfigEntry[].class);
        return entries == null ? Collections.emptyList() : Arrays.asList(entries);
    }
}