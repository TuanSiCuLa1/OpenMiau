package myau.mixin;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {GuiNewChat.class}, priority = 9999)
public abstract class MixinGuiNewChat {
    private static final int OPENMYAU_DUPLICATE_CHAT_ID = 873420;
    private String openMyauLastMessage = null;
    private int openMyauDuplicateCount = 1;
    private boolean openMyauReplacingDuplicate = false;

    @Shadow
    public abstract void deleteChatLine(int id);

    @Shadow
    public abstract void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId);

    @Inject(method = {"printChatMessageWithOptionalDeletion"}, at = @At("HEAD"), cancellable = true)
    private void openMyau$compactDuplicateChat(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        if (openMyauReplacingDuplicate || chatComponent == null || chatLineId != 0) {
            return;
        }

        String message = chatComponent.getUnformattedText();
        if (message == null || message.isEmpty()) {
            openMyauLastMessage = message;
            openMyauDuplicateCount = 1;
            return;
        }

        if (message.equals(openMyauLastMessage)) {
            openMyauDuplicateCount++;
            IChatComponent compacted = chatComponent.createCopy();
            compacted.appendText(" §7[x" + openMyauDuplicateCount + "]");

            ci.cancel();
            openMyauReplacingDuplicate = true;
            this.deleteChatLine(OPENMYAU_DUPLICATE_CHAT_ID);
            this.printChatMessageWithOptionalDeletion(compacted, OPENMYAU_DUPLICATE_CHAT_ID);
            openMyauReplacingDuplicate = false;
        } else {
            if (openMyauDuplicateCount > 1) {
                this.deleteChatLine(OPENMYAU_DUPLICATE_CHAT_ID);
            }
            openMyauLastMessage = message;
            openMyauDuplicateCount = 1;
        }
    }
}
