package myau.util.notification;

public class Notification {
    public String title;
    public String description;
    public float timer;
    public float maxTime;
    public NotificationType type;
    
    public float x, y, targetX, targetY;
    public float alpha;
    public long lastTime;

    public Notification(String title, String description, float maxTime, NotificationType type) {
        this.title = title;
        this.description = description;
        this.maxTime = maxTime;
        this.timer = maxTime;
        this.type = type;
        
        this.alpha = 0; // Start invisible, fade in
        this.lastTime = System.currentTimeMillis();
    }
}
