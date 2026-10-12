package iuh.fit.notificationservice.infrastructure.external.adapter;

import iuh.fit.notificationservice.application.port.client.RecipientDirectoryPort;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthRecipientDirectoryAdapter implements RecipientDirectoryPort {
    private final AuthClient authClient;

    @Override
    public List<UUID> findRecipientIds(Set<String> roles, UUID after, int limit) {
        List<UUID> ids = authClient.getRecipientIds(roles, after, limit).data();
        return ids == null ? List.of() : ids;
    }

    @Override
    public long countRecipients(Set<String> roles) {
        Long count = authClient.countRecipients(roles).data();
        return count == null ? 0 : count;
    }
}
