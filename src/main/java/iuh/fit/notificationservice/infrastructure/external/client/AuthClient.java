package iuh.fit.notificationservice.infrastructure.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import iuh.fit.notificationservice.infrastructure.config.AuthClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = AuthClientConfig.class
)
public interface AuthClient {
    /** BaseResponse cua auth-service; ten hien thi nam o {@code data.fullName}. */
    @GetMapping("/{userId}")
    JsonNode getUserById(@PathVariable UUID userId);

    @GetMapping("/admin")
    String getAdmin();
}
