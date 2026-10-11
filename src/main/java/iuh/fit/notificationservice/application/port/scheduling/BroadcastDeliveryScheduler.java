package iuh.fit.notificationservice.application.port.scheduling;

import java.util.UUID;

/** Chạy việc gửi thông báo hệ thống ở nền, sau khi bản ghi đã được lưu (commit). */
public interface BroadcastDeliveryScheduler {
    void schedule(UUID broadcastId);
}
