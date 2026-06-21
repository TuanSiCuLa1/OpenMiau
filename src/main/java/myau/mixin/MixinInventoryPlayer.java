package myau.mixin;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import myau.util.player.IInventoryPlayerAccessor;

@SideOnly(Side.CLIENT)
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer implements IInventoryPlayerAccessor {
    @Shadow
    public ItemStack[] mainInventory;

    @Shadow
    public int currentItem;

    @Shadow
    public EntityPlayer player;

    @Unique
    private boolean myau$alternativeSlot = false;
    
    @Unique
    private int myau$alternativeCurrentItem = 0;

    @Inject(method = "getCurrentItem", at = @At("HEAD"), cancellable = true)
    private void myau$getCurrentItem(CallbackInfoReturnable<ItemStack> cir) {
        if (this.player == net.minecraft.client.Minecraft.getMinecraft().thePlayer) {
            int slot = this.myau$getAlternativeSlot() ? this.myau$getAlternativeCurrentItem() : this.currentItem;
            cir.setReturnValue(slot < 9 && slot >= 0 ? this.mainInventory[slot] : null);
        }
    }

    @Inject(method = "getStrVsBlock", at = @At("HEAD"), cancellable = true)
    private void myau$getStrVsBlock(Block blockIn, CallbackInfoReturnable<Float> cir) {
        if (this.player == net.minecraft.client.Minecraft.getMinecraft().thePlayer) {
            int slot = this.myau$getAlternativeSlot() ? this.myau$getAlternativeCurrentItem() : this.currentItem;
            float f = 1.0F;
            if (slot < 9 && slot >= 0 && this.mainInventory[slot] != null) {
                f *= this.mainInventory[slot].getStrVsBlock(blockIn);
            }
            cir.setReturnValue(f);
        }
    }

    @Override
    public boolean myau$getAlternativeSlot() {
        return this.myau$alternativeSlot;
    }

    @Override
    public void myau$setAlternativeSlot(boolean value) {
        this.myau$alternativeSlot = value;
    }

    @Override
    public int myau$getAlternativeCurrentItem() {
        return this.myau$alternativeCurrentItem;
    }

    @Override
    public void myau$setAlternativeCurrentItem(int value) {
        this.myau$alternativeCurrentItem = value;
    }
}

