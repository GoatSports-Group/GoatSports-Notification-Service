package iuh.fit.notificationservice.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendBroadcastRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
        String title,
        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
        String content,
        /** ALL | PLAYER | VENUE_OWNER */
        @NotBlank(message = "Chọn đối tượng nhận")
        String audience
) {}
