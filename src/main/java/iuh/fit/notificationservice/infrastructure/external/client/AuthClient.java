package iuh.fit.notificationservice.infrastructure.external.client;

import iuh.fit.notificationservice.infrastructure.config.AuthClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = AuthClientConfig.class
)
public interface AuthClient {
    /**
     * Vỏ BaseResponse của auth-service. Dùng record có kiểu thay vì JsonNode: Spring Boot 4 giải mã bằng Jackson 3,
     */
    record Envelope<T>(T data) {}

    record UserView(String fullName, String username) {}

    @GetMapping("/{userId}")
    Envelope<UserView> getUserById(@PathVariable UUID userId);

    @GetMapping("/admin")
    String getAdmin();

    @GetMapping("/recipients/ids")
    Envelope<List<UUID>> getRecipientIds(@RequestParam("roles") Set<String> roles,
                                         @RequestParam(value = "after", required = false) UUID after,
                                         @RequestParam("limit") int limit);

    @GetMapping("/recipients/count")
    Envelope<Long> countRecipients(@RequestParam("roles") Set<String> roles);
}
