package myau.ui.clickgui.components.impl;

import myau.ui.clickgui.components.Component;
import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;
<<<<<<< HEAD
import myau.util.render.RenderUtil;
=======
import myau.util.render.RenderUtils;
>>>>>>> 746610b90671b5ee596a876af938a43584190552
import myau.util.font.Fonts;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    public Property<?> property;
    private ModuleComponent moduleComponent;
    public float o;
    public float x;
    private float y;
    public boolean heldDown = false;
    private double width;
    public float xOffset;

    private double targetValue;
    private double displayedValue;
    private static final double SLIDER_SPEED = 0.6;

    public SliderComponent(Property<?> property, ModuleComponent moduleComponent, float o) {
        this.property = property;
        this.moduleComponent = moduleComponent;
        this.o = o;

        double initial = getValue();
        this.targetValue = initial;
        this.displayedValue = initial;
        double range = getMax() - getMin();
        this.width = range == 0 ? 0 : (double) (this.moduleComponent.categoryComponent.getWidth() - 8) * (initial - getMin()) / range;
    }

    public double getValue() {
        if (property instanceof FloatProperty) {
            return ((FloatProperty) property).getValue().doubleValue();
        } else if (property instanceof IntProperty) {
            return ((IntProperty) property).getValue().doubleValue();
        } else if (property instanceof PercentProperty) {
            return ((PercentProperty) property).getValue().doubleValue();
        } else if (property instanceof ModeProperty) {
            return ((ModeProperty) property).getValue().doubleValue();
        }
        return 0;
    }

    public double getMin() {
        if (property instanceof FloatProperty) {
            return ((FloatProperty) property).getMinimum().doubleValue();
        } else if (property instanceof IntProperty) {
            return ((IntProperty) property).getMinimum().doubleValue();
        } else if (property instanceof PercentProperty) {
            return ((PercentProperty) property).getMinimum().doubleValue();
        }
        return 0;
    }

    public double getMax() {
        if (property instanceof FloatProperty) {
            return ((FloatProperty) property).getMaximum().doubleValue();
        } else if (property instanceof IntProperty) {
            return ((IntProperty) property).getMaximum().doubleValue();
        } else if (property instanceof PercentProperty) {
            return ((PercentProperty) property).getMaximum().doubleValue();
        } else if (property instanceof ModeProperty) {
            return ((ModeProperty) property).getModes().length - 1;
        }
        return 1;
    }

    public void setValue(double newValue) {
        newValue = Math.max(getMin(), Math.min(getMax(), newValue));
        if (property instanceof FloatProperty) {
            property.setValue((float) newValue);
        } else if (property instanceof IntProperty) {
            property.setValue((int) Math.round(newValue));
        } else if (property instanceof PercentProperty) {
            property.setValue((int) Math.round(newValue));
        } else if (property instanceof ModeProperty) {
            property.setValue((int) Math.round(newValue));
        }
    }

    public boolean isString() {
        return property instanceof ModeProperty;
    }

    public String[] getOptions() {
        if (property instanceof ModeProperty) {
            return ((ModeProperty) property).getModes();
        }
        return null;
    }

    public String getSuffix() {
        if (property instanceof PercentProperty) {
            return "%";
        }
        return "";
    }

    @Override
    public void render() {
        RenderUtil.drawRoundedRectangle(
            this.moduleComponent.categoryComponent.getX() + 4 + (xOffset / 2),
            this.moduleComponent.categoryComponent.getY() + this.o + 11,
            this.moduleComponent.categoryComponent.getX() + 4 + this.moduleComponent.categoryComponent.getWidth() - 8,
            this.moduleComponent.categoryComponent.getY() + this.o + 15,
            4,
            -12302777
        );

        float left = this.moduleComponent.categoryComponent.getX() + 4 + (xOffset / 2);
        float right = (float) (left + this.width);

        if (right - left > 84) {
            right = left + 84;
        }

        RenderUtil.drawRoundedRectangle(
            left,
            this.moduleComponent.categoryComponent.getY() + this.o + 11,
            right,
            this.moduleComponent.categoryComponent.getY() + this.o + 15,
            4,
            Color.getHSBColor((float) (System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F).getRGB()
        );

        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        double input = getValue();
        String suffix = getSuffix();
        String valueText;

        if (isString()) {
            int idx = (int) Math.round(input);
            String[] opts = getOptions();
            if (opts != null && opts.length > 0) {
                idx = Math.max(0, Math.min(idx, opts.length - 1));
                valueText = opts[idx];
            } else {
                valueText = "";
            }
        } else {
            if (property instanceof IntProperty || property instanceof PercentProperty) {
                valueText = String.valueOf((int) Math.round(input));
            } else {
                valueText = String.format("%.2f", input);
            }
        }

        float labelX = (float) ((this.moduleComponent.categoryComponent.getX() + 4) * 2) + xOffset;
        float labelY = (float) ((this.moduleComponent.categoryComponent.getY() + this.o + 3) * 2);

        Fonts.MINECRAFT.get(18).draw(
            this.property.getName() + ": " + (isString() ? "\u00a7e" : "\u00a7b") + valueText + suffix,
            labelX,
            labelY,
            -1,
            true
        );

        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();

        if (this.heldDown) {
            double d = Math.min(this.moduleComponent.categoryComponent.getWidth() - 8, Math.max(0, mouseX - this.x));
            double range = getMax() - getMin();
            double n = getMin() + (d / (double) (this.moduleComponent.categoryComponent.getWidth() - 8)) * range;
            this.targetValue = roundToInterval(n, 4);
            this.displayedValue = displayedValue + (targetValue - displayedValue) * SLIDER_SPEED;

            setValue(this.targetValue);

            if (range == 0) {
                this.width = 0;
            } else {
                double fraction = (this.displayedValue - getMin()) / range;
                this.width = (this.moduleComponent.categoryComponent.getWidth() - 8) * fraction;
            }
        }
    }

    public void onSliderChange() {
        double initial = getValue();
        this.targetValue = initial;
        this.displayedValue = initial;
        double range = getMax() - getMin();
        this.width = range == 0 ? 0 : (double) (this.moduleComponent.categoryComponent.getWidth() - 8) * (initial - getMin()) / range;
    }

    private static double roundToInterval(double value, int places) {
        if (places < 0) {
            return 0.0D;
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if ((u(mouseX, mouseY) || i(mouseX, mouseY)) && button == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            this.heldDown = true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.heldDown = false;
    }

    public boolean u(int mouseX, int mouseY) {
        return mouseX > this.x && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() / 2 + 1 && mouseY > this.y && mouseY < this.y + 16;
    }

    public boolean i(int mouseX, int mouseY) {
        return mouseX > this.x + this.moduleComponent.categoryComponent.getWidth() / 2 && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() && mouseY > this.y && mouseY < this.y + 16;
    }

    @Override
    public void onGuiClosed() {
        this.heldDown = false;
    }

    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.property.isVisible();
    }
}
