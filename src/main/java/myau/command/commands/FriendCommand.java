package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.client.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class FriendCommand extends Command {
    public FriendCommand() {
        super(new ArrayList<>(Arrays.asList("friend", "f")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            String subCommand = args.get(1).toLowerCase(Locale.ROOT);
            switch (subCommand) {
                case "a":
                case "add":
                    if (args.size() < 3) {
                        ChatUtil.display("%sUsage: .%s add <&oname&r> [&oname&r] ...&r", args.get(0).toLowerCase(Locale.ROOT));
                        return;
                    }
                    for (String name: args.subList(2, args.size())) {
                        String added = Myau.friendManager.add(name);
                        if (added == null) {
                            ChatUtil.display("%s&o%s&r is already in your friend list&r", name);
                        } else {
                            ChatUtil.display("%sAdded &o%s&r to your friend list&r", added);
                        }
                    }
                    return;
                case "r":
                case "remove":
                    if (args.size() < 3) {
                        ChatUtil.display(
                                String.format("%sUsage: .%s remove <&oname&r> [&oname&r] ...&r", args.get(0).toLowerCase(Locale.ROOT))
                        );
                        return;
                    }
                    for (String name: args.subList(2, args.size())){
                        String removed = Myau.friendManager.remove(name);
                        if (removed == null) {
                            ChatUtil.display("%s&o%s&r is not in your friend list&r", name);
                        } else {
                            ChatUtil.display("%sRemoved &o%s&r from your friend list&r", removed);
                        }
                    }
                    return;
                case "l":
                case "list":
                    ArrayList<String> list = Myau.friendManager.getPlayers();
                    if (list.isEmpty()) {
                        ChatUtil.display("%sNo friends&r");
                        return;
                    }
                    ChatUtil.display("%sFriends:&r");
                    for (String friend : list) {
                        ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r"), friend));
                    }
                    return;
                case "c":
                case "clear":
                    Myau.friendManager.clear();
                    ChatUtil.display("%sCleared your friend list&r");
                    return;
                default:
                    if (args.size() == 2) {
                        if (Myau.friendManager.isFriend(args.get(1))) {
                            runCommand(new ArrayList<>(Arrays.asList(args.get(0), "remove", args.get(1))));
                        } else {
                            runCommand(new ArrayList<>(Arrays.asList(args.get(0), "add", args.get(1))));
                        }
                        return;
                    }
            }
        }
        ChatUtil.display("%sUsage: .%s <&oa(dd)&r/&or(emove)&r/&ol(ist)&r/&oc(lear)&r>&r", args.get(0).toLowerCase(Locale.ROOT));
    }
}
