package iuh.fit.notificationservice.presentation.controller;

import iuh.fit.notificationservice.application.dto.PageFilter;
import iuh.fit.notificationservice.application.dto.request.CountUnreadNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.GetNotificationsCommand;
import iuh.fit.notificationservice.application.dto.request.MarkNotificationReadCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.dto.response.base.BaseListResponse;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.security.CurrentUserProvider;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.presentation.converter.NotificationConverter;
import iuh.fit.notificationservice.presentation.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

        private final NotificationService notificationService;
        private final CurrentUserProvider currentUserProvider;
        private final NotificationMapper notificationMapper;
        private final NotificationConverter notificationConverter;

        @GetMapping
        public ResponseEntity<BaseListResponse> getAllNotifications(
                        @RequestParam(value = "filter", required = false) String filter,
                        Pageable pageable) throws ServiceException {
                String userIdStr = currentUserProvider.getCurrentUserId()
                                .orElseThrow(() -> new ServiceException(
                                                 "Bạn phải đăng nhập để thực hiện chức năng này."));
                UUID receiverId = BaseUtil.uuid(userIdStr);

                PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
                GetNotificationsCommand command = new GetNotificationsCommand(query, receiverId);

                PageResult<NotificationResponseCommand> result = notificationMapper.toNotificationResponseCommand(
                                 notificationService.getAllNotifications(command));

                BaseListResponse.Meta meta = new BaseListResponse.Meta();
                meta.setPage(result.page());
                meta.setPageSize(result.pageSize());
                meta.setPages(result.totalPages());
                meta.setTotal(result.total());

                List<NotificationResponse> responses = result.items().stream()
                                .map(notificationConverter::toResponse)
                                .toList();

                return ResponseEntity.ok(new BaseListResponse(meta, responses));
        }

        @GetMapping("/unread-count")
        public ResponseEntity<Long> countUnreadNotifications() throws ServiceException {
                String userIdStr = currentUserProvider.getCurrentUserId()
                                .orElseThrow(() -> new ServiceException(
                                                 "Bạn phải đăng nhập để thực hiện chức năng này."));
                UUID receiverId = BaseUtil.uuid(userIdStr);

                CountUnreadNotificationCommand command = new CountUnreadNotificationCommand(receiverId);
                long count = notificationService.countUnreadNotification(command);

                return ResponseEntity.ok(count);
        }

        @PutMapping("/{id}/read")
        public ResponseEntity<NotificationResponse> markNotificationRead(
                        @PathVariable("id") String id) throws ServiceException {
                UUID notificationId = BaseUtil.uuid(id);
                MarkNotificationReadCommand command = new MarkNotificationReadCommand(notificationId);

                NotificationResponseCommand result = notificationMapper.toNotificationResponseCommand(
                                 notificationService.markNotificationRead(command));

                return ResponseEntity.ok(notificationConverter.toResponse(result));
        }
}
