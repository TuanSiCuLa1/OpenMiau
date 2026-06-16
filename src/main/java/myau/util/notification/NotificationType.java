package myau.util.notification;

public enum NotificationType {
    SUCCESS(0xFF55FF55), 
    ERROR(0xFFFF5555),   
    INFO(0xFFFFFFFF),    
    WARNING(0xFFFFFF55); 

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
