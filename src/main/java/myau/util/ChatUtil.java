package myau.util; 

import myau.enums.ChatColors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void display(String message, Object... objects) {
        if (mc.thePlayer != null) {
            String text = String.format(message, objects);
            String prefix = ChatColors.getDynamicPrefix();
            String finalMessage = ChatColors.formatColor(prefix + text);
            mc.thePlayer.addChatMessage(new ChatComponentText(finalMessage));
        }
    }

    public static void displayNoPrefix(String message, Object... objects) {
        if (mc.thePlayer != null) {
            String text = String.format(message, objects);
            mc.thePlayer.addChatMessage(new ChatComponentText(ChatColors.formatColor(text)));
        }
    }

    public static void sendRaw(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(ChatColors.formatColor(message)));
        }
    }

    public static void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C01PacketChatMessage(message));
        }
    }

    public static void send(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C01PacketChatMessage(message));
        }
    }

    public static void send(IChatComponent component) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(component);
        }
    }

    public static void sendFormatted(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(ChatColors.formatColor(message)));
        }
    }

    public static void sendFormatted(IChatComponent component) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(component);
        }
    }
}