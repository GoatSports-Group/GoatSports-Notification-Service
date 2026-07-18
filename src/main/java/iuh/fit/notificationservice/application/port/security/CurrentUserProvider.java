package iuh.fit.notificationservice.application.port.security;

import iuh.fit.notificationservice.application.exception.ServiceException;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {
    Optional<String> getCurrentUserId();

    UUID getAdminId() throws ServiceException;
}
