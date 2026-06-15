package myau.util.animation;

/**
 * Animation utility for smooth value transitions.
 * Uses easing functions to provide smooth interpolation between values.
 */
public class Animation {
    private Easing easing;
    private long duration;
    private long startTime;

    private float startValue;
    private float destinationValue;
    private float value;
    private boolean finished;

    public Animation(Easing easing, long duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.startValue = 0f;
        this.destinationValue = 0f;
        this.value = 0f;
        this.finished = true;
    }

    /**
     * Updates the animation by using the easing function and time
     *
     * @param destinationValue the value that the animation is going to reach
     */
    public void run(float destinationValue) {
        if (this.destinationValue != destinationValue) {
            this.destinationValue = destinationValue;
            this.reset();
        } else {
            this.finished = System.currentTimeMillis() - this.duration > this.startTime;
            if (this.finished) {
                this.value = destinationValue;
                return;
            }
        }

        float result = (float) this.easing.apply(this.getProgress());
        if (this.value > destinationValue) {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        } else {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }
    }

    /**
     * Returns the progress of the animation
     *
     * @return value between 0 and 1
     */
    public double getProgress() {
        return (double) (System.currentTimeMillis() - this.startTime) / (double) this.duration;
    }

    /**
     * Resets the animation to the start value
     */
    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = value;
        this.finished = false;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public boolean isFinished() {
        return finished;
    }
}
