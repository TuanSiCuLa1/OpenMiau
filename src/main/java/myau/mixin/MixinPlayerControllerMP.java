package myau.mixin;

import myau.event.EventManager;
import myau.events.AttackEvent;
import myau.events.CancelUseEvent;
import myau.events.WindowClickEvent;
import myau.events.BlockDamageEvent;
import myau.events.BlockBreakEvent;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {PlayerControllerMP.class}, priority = 9999)
public abstract class MixinPlayerControllerMP {
    @Inject(
            method = "attackEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"))
    private void attackEntity(
            EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo callbackInfo
    ) {
        AttackEvent event = new AttackEvent(targetEntity);
        EventManager.call(event);
    }
    @Inject(
            method = {"windowClick"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void windowClick(
            int windowId, int slotId, int mouseButtonClicked, int mode, EntityPlayer entityPlayer, CallbackInfoReturnable<ItemStack> callbackInfoReturnable
    ) {
        WindowClickEvent event = new WindowClickEvent(windowId, slotId, mouseButtonClicked, mode);
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(
            method = {"onStoppedUsingItem"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onStoppedUsingItem(CallbackInfo callbackInfo) {
        CancelUseEvent event = new CancelUseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"onPlayerRightClick"},
            at = {@At("RETURN")}
    )
    private void myau$playPlaceSound(
            EntityPlayerSP player, WorldClient worldIn, ItemStack heldStack,
            BlockPos hitPos, EnumFacing side, Vec3 hitVec,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() || heldStack == null || worldIn == null) {
            return;
        }
        if (!(heldStack.getItem() instanceof ItemBlock)) {
            return;
        }
        Block target = ((ItemBlock) heldStack.getItem()).getBlock();
        if (target == null || target == Blocks.air) {
            return;
        }
        // Confirm a block of the held type was actually placed (client places optimistically).
        // This avoids false positives such as opening a container while holding a block.
        BlockPos placedPos;
        BlockPos sidePos = hitPos.offset(side);
        if (worldIn.getBlockState(sidePos).getBlock() == target) {
            placedPos = sidePos;
        } else if (worldIn.getBlockState(hitPos).getBlock() == target) {
            placedPos = hitPos;
        } else {
            return;
        }
        Block.SoundType sound = target.stepSound;
        if (sound == null) {
            return;
        }
        worldIn.playSound(
                placedPos.getX() + 0.5,
                placedPos.getY() + 0.5,
                placedPos.getZ() + 0.5,
                sound.getPlaceSound(),
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getFrequency() * 0.8F,
                false
        );
    }
}