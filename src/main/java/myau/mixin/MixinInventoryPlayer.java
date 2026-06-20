package myau.mixin;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import myau.util.player.IInventoryPlayerAccessor;

@SideOnly(Side.CLIENT)
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer implements IInventoryPlayerAccessor {
    @Unique
    private boolean myau$alternativeSlot = false;
    
    @Unique
    private int myau$alternativeCurrentItem = 0;

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
