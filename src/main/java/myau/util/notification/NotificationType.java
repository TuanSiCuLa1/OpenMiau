package myau.util.notification;

public enum NotificationType {
    SUCCESS(0xFF55FF55), // Xanh lá
    ERROR(0xFFFF5555),   // Đỏ
    INFO(0xFFFFFFFF),    // Trắng
    WARNING(0xFFFFFF55); // Vàng

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
