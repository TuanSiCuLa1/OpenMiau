package myau.util.client;

import myau.module.modules.misc.MouseRawInput;
import net.minecraft.util.MouseHelper;
import myau.util.math.*;
import myau.util.time.*;
import myau.util.player.*;
import myau.util.world.*;
import myau.util.network.*;
import myau.util.client.*;
import myau.util.misc.*;
import myau.util.render.*;
import myau.util.animation.*;

public class RawMouseHelper extends MouseHelper {
    @Override
    public void mouseXYChange() {
        int rawDeltaX = MouseRawInput.consumeDeltaX();
        int rawDeltaY = MouseRawInput.consumeDeltaY();

        if (rawDeltaX == 0 && rawDeltaY == 0) {
            super.mouseXYChange();
            return;
        }

        this.deltaX = rawDeltaX;
        this.deltaY = -rawDeltaY;
    }
}
