package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.util.client.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(new ArrayList<>(Arrays.asList("help", "commands")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Myau.moduleManager.modules.isEmpty()) {
            ChatUtil.display("%sCommands:&r");
            for (Command command : Myau.commandManager.commands) {
                if (!(command instanceof ModuleCommand)) {
                    ChatUtil.display("&7»&r .%s&r", String.join(" &7/&r .", command.names));
                }
            }
        }
    }
}
