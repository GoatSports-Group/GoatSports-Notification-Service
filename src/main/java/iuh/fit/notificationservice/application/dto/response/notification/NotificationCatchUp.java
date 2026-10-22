package iuh.fit.notificationservice.application.dto.response.notification;

import iuh.fit.notificationservice.domain.entity.Notification;

import java.util.List;

/** Thông báo cũ nhất trước; còn nữa thì gọi tiếp với mốc là phần tử cuối. */
public record NotificationCatchUp(List<Notification> items, boolean hasMore) {}
