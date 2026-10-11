package iuh.fit.notificationservice.infrastructure.external.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import iuh.fit.notificationservice.application.port.client.RecipientDirectoryPort;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthRecipientDirectoryAdapter implements RecipientDirectoryPort {
    private final AuthClient authClient;

    @Override
    public List<UUID> findRecipientIds(Set<String> roles, UUID after, int limit) {
        JsonNode data = authClient.getRecipientIds(roles, after, limit).path("data");
        List<UUID> ids = new ArrayList<>(data.size());
        data.forEach(item -> ids.add(UUID.fromString(item.asText())));
        return ids;
    }

    @Override
    public long countRecipients(Set<String> roles) {
        return authClient.countRecipients(roles).path("data").asLong();
    }
}
