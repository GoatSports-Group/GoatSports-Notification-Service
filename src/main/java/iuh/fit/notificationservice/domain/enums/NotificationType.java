package iuh.fit.notificationservice.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    OWNER_APPLICATION("Đơn đăng ký"),
    SYSTEM("Hệ thống"),
    USER("Người dùng"),
    BOOKING("Đặt lịch"),
    PAYMENT("Thanh toán");

    private final String value;
    NotificationType(String value) {
        this.value = value;
    }
}
