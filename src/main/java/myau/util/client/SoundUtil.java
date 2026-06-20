package myau.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public class SoundUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void playSound(String soundName) {
        SoundHandler soundHandler = mc.getSoundHandler();
        if (soundHandler != null) {
            PositionedSoundRecord positionedSoundRecord = PositionedSoundRecord.create(new ResourceLocation(soundName));
            soundHandler.playSound(positionedSoundRecord);
        }
    }
}


