package myau.events;

import myau.event.events.Event;

public class StrafeEvent implements Event {
    private float strafe;
    private float forward;
    private float friction;
    private float yaw;

    public StrafeEvent(float strafe, float forward, float friction) {
        this.strafe = strafe;
        this.forward = forward;
        this.friction = friction;
        this.yaw = 0.0F;
    }

    public StrafeEvent(float strafe, float forward, float friction, float yaw) {
        this.strafe = strafe;
        this.forward = forward;
        this.friction = friction;
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public float getForward() {
        return this.forward;
    }

    public float getFriction() {
        return this.friction;
    }

    public void setStrafe(float float1) {
        this.strafe = float1;
    }

    public void setForward(float float1) {
        this.forward = float1;
    }

    public void setFriction(float float1) {
        this.friction = float1;
    }
}
