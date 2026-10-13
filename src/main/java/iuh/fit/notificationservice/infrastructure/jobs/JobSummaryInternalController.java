package iuh.fit.notificationservice.infrastructure.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tình trạng job nền của service cho trang Giám sát (audit-service gọi; gateway chặn /internal/** từ ngoài).
 * Đếm thẳng bằng SQL: chỉ đọc, không cần qua use case.
 */
@RestController
@RequiredArgsConstructor
public class JobSummaryInternalController {
    private final JdbcTemplate jdbc;

    public record JobSummary(String job, String label, long pending, long running, long failedLast24h, Instant oldestPendingAt) {
    }

    @GetMapping("/internal/jobs/summary")
    public List<JobSummary> summary() {
        // SENDING chưa được worker nhận (claimed_at trống) là đang chờ; đã nhận là đang gửi theo lô.
        return List.of(new JobSummary("BROADCASTS", "Gửi thông báo hàng loạt",
                count("select count(*) from broadcasts where status = 'SENDING' and claimed_at is null"),
                count("select count(*) from broadcasts where status = 'SENDING' and claimed_at is not null"),
                count("select count(*) from broadcasts where status = 'FAILED' and updated_at >= ?", dayAgo()),
                oldest("select min(created_at) from broadcasts where status = 'SENDING'")));
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /** Cột "timestamp" (không múi giờ) lưu theo giờ ứng dụng; "timestamptz" là thời điểm tuyệt đối. */
    private Instant oldest(String sql) {
        Timestamp value = jdbc.queryForObject(sql, Timestamp.class);
        return value == null ? null : value.toInstant();
    }

    /** Mốc 24 giờ trước tính ở ứng dụng, không dùng now() của DB (Postgres trong Docker chạy UTC). */
    private static LocalDateTime dayAgo() {
        return LocalDateTime.now().minusHours(24);
    }
}
