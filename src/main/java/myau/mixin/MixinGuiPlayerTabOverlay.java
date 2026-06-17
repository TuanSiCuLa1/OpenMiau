package myau.mixin;

import myau.Myau;
import myau.enums.ChatColors;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void addMiauPrefix(NetworkPlayerInfo info, CallbackInfoReturnable<String> callbackInfo) {
        if (Myau.presenceManager == null || info == null || info.getGameProfile() == null) {
            return;
        }

        String uuid = info.getGameProfile().getId() == null ? null : info.getGameProfile().getId().toString();
        String username = info.getGameProfile().getName();
        if (!Myau.presenceManager.isMiauPlayer(uuid, username)) {
            return;
        }

        String name = callbackInfo.getReturnValue();
        String prefix = ChatColors.formatColor(Myau.clientName);
        if (name != null && !name.startsWith(prefix)) {
            callbackInfo.setReturnValue(prefix + name);
        }
    }
}