package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.config.online.OnlineConfigApplier;
import myau.config.online.OnlineConfigClient;
import myau.config.online.OnlineConfigEntry;
import myau.util.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineConfigCommand extends Command {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "OpenMyau OnlineConfig");
        thread.setDaemon(true);
        return thread;
    });

    private final OnlineConfigClient client = new OnlineConfigClient();
    private volatile List<OnlineConfigEntry> cache = Collections.emptyList();

    public OnlineConfigCommand() {
        super(new ArrayList<>(Arrays.asList("onlineconfig", "onlinecfg", "ocfg", "online")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            usage();
            return;
        }
        String sub = args.get(1).toLowerCase(Locale.ROOT);
        if (sub.equals("list") || sub.equals("l")) {
            async(this::listConfigs);
        } else if (sub.equals("load")) {
            if (args.size() < 3) {
                ChatUtil.sendFormatted(Myau.clientName + "Missing online config id/name&r");
                return;
            }
            async(() -> loadConfig(String.join(" ", args.subList(2, args.size()))));
        } else {
            usage();
        }
    }

    private void listConfigs() {
        try {
            List<OnlineConfigEntry> entries = Collections.unmodifiableList(new ArrayList<>(client.list()));
            runOnClientThread(() -> {
                cache = entries;
                ChatUtil.sendFormatted(
                        Myau.clientName + (entries.isEmpty() ? "No online configs found&r" : "Online configs:&r"));
                for (OnlineConfigEntry entry : entries) {
                    sendEntry(entry);
                }
            });
        } catch (Exception e) {
            runOnClientThread(() -> ChatUtil
                    .sendFormatted(Myau.clientName + "Failed to list online configs: &c" + e.getMessage() + "&r"));
        }
    }

    private void loadConfig(String input) {
        try {
            OnlineConfigEntry entry = findEntry(input);
            if (entry == null) {
                runOnClientThread(
                        () -> ChatUtil.sendFormatted(Myau.clientName + "Online config not found (&o" + input + "&r)&r"));
                return;
            }
            String json = client.load(entry.getId());
            runOnClientThread(() -> applyConfig(entry, json));
        } catch (Exception e) {
            runOnClientThread(() -> ChatUtil
                    .sendFormatted(Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r"));
        }
    }

    private void applyConfig(OnlineConfigEntry entry, String json) {
        try {
            showMetadata(entry);
            int applied = new OnlineConfigApplier().apply(json);
            ChatUtil.sendFormatted(String.format("%sOnline config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                    Myau.clientName, entry.getName(), applied));
        } catch (Exception e) {
            ChatUtil.sendFormatted(Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r");
        }
    }

    private OnlineConfigEntry findEntry(String input) throws Exception {
        List<OnlineConfigEntry> entries = cache;
        if (entries.isEmpty()) {
            entries = Collections.unmodifiableList(new ArrayList<>(client.list()));
            cache = entries;
        }
        for (OnlineConfigEntry entry : entries) {
            if (entry.getId().equalsIgnoreCase(input) || entry.getName().equalsIgnoreCase(input)) {
                return entry;
            }
        }
        return null;
    }

    private void sendEntry(OnlineConfigEntry entry) {
        String command = ".onlineconfig load " + entry.getId();
        String line = String.format("§7» §f%s §7[§b%s§7] §7by §a%s", entry.getName(), safe(entry.setting_type),
                entry.getAuthor());
        ChatUtil.send(new ChatComponentText(line).setChatStyle(new ChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(command + "\n§7id: §f" + entry.getId() + "\n§7status: §f"
                                + safe(entry.status_type))))));
    }

    private void showMetadata(OnlineConfigEntry entry) {
        ChatUtil.sendFormatted(Myau.clientName + "Loading online config...&r");
        ChatUtil.sendFormatted("&fName: &a" + entry.getName() + "&r");
        ChatUtil.sendFormatted("&fUpload time: &b" + safe(entry.date) + "&r");
        ChatUtil.sendFormatted("&fAuthor: &a" + entry.getAuthor() + "&r");
        ChatUtil.sendFormatted("&fType: &b" + safe(entry.setting_type) + "&r");
        ChatUtil.sendFormatted("&fStatus: &e" + safe(entry.status_type) + "&r");
        if (entry.description != null && !entry.description.trim().isEmpty()) {
            ChatUtil.sendFormatted("&fDescription: &7" + entry.description + "&r");
        }
    }

    private void usage() {
        ChatUtil.sendFormatted(
                Myau.clientName + "Usage: .onlineconfig &olist&r | .onlineconfig &oload&r <&oid/name&r>");
    }

    private void async(Runnable task) {
        EXECUTOR.execute(task);
    }

    private void runOnClientThread(Runnable task) {
        net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(task);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }
}
