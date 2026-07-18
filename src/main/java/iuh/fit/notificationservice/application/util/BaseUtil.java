package iuh.fit.notificationservice.application.util;

import iuh.fit.notificationservice.application.dto.event.type.OwnerApplicationEventType;
import iuh.fit.notificationservice.application.exception.ServiceException;

import java.util.UUID;

public class BaseUtil {

    public static UUID uuid(String id) throws ServiceException {
        UUID uuid;

        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id không hợp lệ: không phải UUID");
        }

        return uuid;
    }

    public static String getTitleForOwnerApplication(OwnerApplicationEventType type) {
        return switch (type) {
            case SUBMITTED -> "Có đơn đăng ký chủ sân mới";
            case APPROVED -> "Yêu cầu đăng ký chủ sân được phê duyệt";
            case REJECTED -> "Yêu cầu đăng ký chủ sân bị từ chối";
            default -> "Thông báo từ hệ thống";
        };
    }

    public static String getContentForOwnerApplication(OwnerApplicationEventType type) {
        return switch (type) {
            case SUBMITTED -> "Có đơn đăng ký chủ sân mới.";
            case APPROVED -> "Đơn đăng ký làm chủ sân của bạn đã được chấp nhận.";
            case REJECTED -> "Đơn đăng ký làm chủ sân của bạn đã bị từ chối.";
            default -> "Bạn có thông báo mới từ hệ thống.";
        };
    }
}
