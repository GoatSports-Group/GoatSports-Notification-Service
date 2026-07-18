package iuh.fit.notificationservice.infrastructure.security;

import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.port.security.CurrentUserProvider;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserProviderAdapter implements CurrentUserProvider {
    private final SecurityContextHelper securityContextHelper;
    private final AuthClient authClient;

    @Override
    public Optional<String> getCurrentUserId() {
        return securityContextHelper.getCurrentUserId();
    }

    @Override
    public UUID getAdminId() throws ServiceException {
        return BaseUtil.uuid(authClient.getAdmin());
    }
}
