package iuh.fit.notificationservice.infrastructure.external.client;

import iuh.fit.notificationservice.application.dto.response.base.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users"
)
public interface AuthClient {
    @GetMapping("/admin")
    BaseResponse<String> getAdmin();
}
