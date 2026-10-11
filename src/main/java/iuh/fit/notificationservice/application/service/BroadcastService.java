package iuh.fit.notificationservice.application.service;

import iuh.fit.notificationservice.application.dto.request.broadcast.SendBroadcastCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.domain.entity.Broadcast;

import java.util.List;
import java.util.UUID;

public interface BroadcastService {
    /** Lưu và xếp lịch gửi. Gửi lại cùng Idempotency-Key trả về thông báo đã tạo, không gửi lần hai. */
    Broadcast send(SendBroadcastCommand command) throws ServiceException;

    PageResult<Broadcast> getBroadcasts(int page, int size);

    long countAudience(String audience) throws ServiceException;

    /** Gửi theo lô từ mốc đã lưu; bỏ qua nếu đã có tiến trình khác đang gửi. */
    void deliver(UUID broadcastId);

    /** Thông báo đang gửi dở mà không ai giữ (service khởi động lại giữa chừng, lần trước lỗi). */
    List<UUID> findStalled();
}
