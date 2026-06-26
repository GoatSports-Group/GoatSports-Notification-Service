package iuh.fit.notificationservice.application.dto;

public record PageFilter(
    String filter,
    int page,
    int size
) {}
