package iuh.fit.notificationservice.infrastructure.persistence.entity;

import iuh.fit.notificationservice.domain.enums.BroadcastAudience;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "broadcasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "broadcast_id", columnDefinition = "UUID")
    private UUID broadcastId;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(nullable = false, length = 2000)
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BroadcastAudience audience;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BroadcastStatus status;
    @Column(name = "sent_by", nullable = false)
    private UUID sentBy;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    @Column(name = "delivered_count", nullable = false)
    private int deliveredCount;
    @Column(name = "last_recipient_id")
    private UUID lastRecipientId;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
