package myau.util;

import myau.module.modules.MouseRawInput;
import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {
    @Override
    public void mouseXYChange() {
        this.deltaX = MouseRawInput.consumeDeltaX();
        this.deltaY = -MouseRawInput.consumeDeltaY();
    }
}
