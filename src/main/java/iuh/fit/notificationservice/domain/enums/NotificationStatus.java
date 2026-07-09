package iuh.fit.notificationservice.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    UNREAD("Chưa đọc"),
    READ("Đã đọc");

    private final String value;
    NotificationStatus(String value) {
        this.value = value;
    }
}
