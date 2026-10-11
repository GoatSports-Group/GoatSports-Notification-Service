package iuh.fit.notificationservice.application.port.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Danh bạ người nhận thông báo hệ thống (auth-service): tài khoản ACTIVE theo vai trò. */
public interface RecipientDirectoryPort {
    /** Keyset theo mã tài khoản: lấy tối đa {@code limit} mã sau {@code after} (null = từ đầu). */
    List<UUID> findRecipientIds(Set<String> roles, UUID after, int limit);

    long countRecipients(Set<String> roles);
}
