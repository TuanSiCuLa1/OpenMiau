package myau.notification;

public class Notification {
    private final NotificationType type;
    private final String title;
    private final String description;
    private final int duration;
    private final long startTime;
    
    // Animation fields
    public float x;
    public float y;
    public float targetY;
    public boolean firstFrame = true;

    public Notification(NotificationType type, String title, String description, int duration) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() - startTime >= duration;
    }

    public long getRemainingTime() {
        return Math.max(0, duration - (System.currentTimeMillis() - startTime));
    }
}
