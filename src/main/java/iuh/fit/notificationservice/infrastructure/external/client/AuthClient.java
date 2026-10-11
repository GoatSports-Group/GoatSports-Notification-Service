package iuh.fit.notificationservice.infrastructure.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import iuh.fit.notificationservice.infrastructure.config.AuthClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = AuthClientConfig.class
)
public interface AuthClient {
    @GetMapping("/{userId}")
    JsonNode getUserById(@PathVariable UUID userId);

    @GetMapping("/admin")
    String getAdmin();

    @GetMapping("/recipients/ids")
    JsonNode getRecipientIds(@RequestParam("roles") Set<String> roles,
                             @RequestParam(value = "after", required = false) UUID after,
                             @RequestParam("limit") int limit);

    @GetMapping("/recipients/count")
    JsonNode countRecipients(@RequestParam("roles") Set<String> roles);
}
