package iuh.fit.notificationservice.presentation.controller.admin;

import iuh.fit.notificationservice.application.dto.request.broadcast.SendBroadcastCommand;
import iuh.fit.notificationservice.application.dto.response.base.BaseListResponse;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.port.security.CurrentUserProvider;
import iuh.fit.notificationservice.application.service.BroadcastService;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.presentation.request.SendBroadcastRequest;
import iuh.fit.notificationservice.presentation.response.BroadcastResponse;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thông báo hệ thống: admin soạn một lần, hệ thống gửi theo lô tới từng người trong đối tượng nhận. */
@RestController
@RequestMapping("/api/v1/admin/broadcasts")
@RequiredArgsConstructor
public class BroadcastAdminController {
    private final BroadcastService broadcastService;
    private final CurrentUserProvider currentUserProvider;

    /** 202: đã lưu và đang gửi ở nền; trạng thái theo dõi ở danh sách. Bấm lại cùng Idempotency-Key không gửi lần hai. */
    @PostMapping
    @PreAuthorize("hasAuthority('SEND_BROADCAST')")
    @AuditLog(action = "SEND_BROADCAST", description = "Gửi thông báo hệ thống tới #{#request.audience()}: #{#request.title()}")
    public ResponseEntity<BroadcastResponse> send(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SendBroadcastRequest request
    ) throws ServiceException {
        String senderId = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        Broadcast broadcast = broadcastService.send(new SendBroadcastCommand(
                request.title(), request.content(), request.audience(), idempotencyKey, BaseUtil.uuid(senderId)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BroadcastResponse.from(broadcast));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GET_BROADCASTS')")
    public ResponseEntity<BaseListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResult<Broadcast> result = broadcastService.getBroadcasts(page, size);
        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(result.page());
        meta.setPageSize(result.pageSize());
        meta.setPages(result.totalPages());
        meta.setTotal(result.total());
        return ResponseEntity.ok(new BaseListResponse(meta, result.items().stream().map(BroadcastResponse::from).toList()));
    }

    /** Số người sẽ nhận (tài khoản đang hoạt động), hiện trong hộp xác nhận trước khi gửi. */
    @GetMapping("/audience-count")
    @PreAuthorize("hasAuthority('GET_BROADCASTS')")
    public ResponseEntity<Long> audienceCount(@RequestParam String audience) throws ServiceException {
        return ResponseEntity.ok(broadcastService.countAudience(audience));
    }
}
