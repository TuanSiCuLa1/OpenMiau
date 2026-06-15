package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static String getPrefix() {
        return "§9M§bi§3a§1u §8» §f";
    }

    public static void send(IChatComponent iChatComponent) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(iChatComponent);
        }
    }

    public static void sendFormatted(String string) {
        send(new ChatComponentText(getPrefix() + string));
    }

    public static void sendRaw(String string) {
        send(new ChatComponentText(string));
    }

    public static void sendMessage(String string) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(string);
        }
    }
}

