package iuh.fit.notificationservice.presentation.response;

import java.util.List;

public record NotificationCatchUpResponse(List<NotificationResponse> items, boolean hasMore) {}
