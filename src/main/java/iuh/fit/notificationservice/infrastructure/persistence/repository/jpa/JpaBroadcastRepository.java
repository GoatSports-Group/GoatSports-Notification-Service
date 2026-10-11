package iuh.fit.notificationservice.infrastructure.persistence.repository.jpa;

import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import iuh.fit.notificationservice.infrastructure.persistence.entity.BroadcastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBroadcastRepository extends JpaRepository<BroadcastEntity, UUID> {
    Optional<BroadcastEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true)
    @Query("""
            update BroadcastEntity b set b.claimedAt = :now, b.attemptCount = b.attemptCount + 1
            where b.broadcastId = :id and b.status = :sending
              and (b.claimedAt is null or b.claimedAt < :staleBefore)
            """)
    int claim(@Param("id") UUID id, @Param("sending") BroadcastStatus sending,
              @Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying(clearAutomatically = true)
    @Query("""
            update BroadcastEntity b
            set b.lastRecipientId = :lastRecipientId, b.deliveredCount = :deliveredCount, b.claimedAt = :now
            where b.broadcastId = :id
            """)
    void checkpoint(@Param("id") UUID id, @Param("lastRecipientId") UUID lastRecipientId,
                    @Param("deliveredCount") int deliveredCount, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
            update BroadcastEntity b
            set b.status = :status, b.completedAt = :completedAt, b.claimedAt = null, b.failureReason = :reason
            where b.broadcastId = :id
            """)
    void finish(@Param("id") UUID id, @Param("status") BroadcastStatus status,
                @Param("completedAt") LocalDateTime completedAt, @Param("reason") String reason);

    @Modifying(clearAutomatically = true)
    @Query("update BroadcastEntity b set b.claimedAt = null, b.failureReason = :reason where b.broadcastId = :id")
    void release(@Param("id") UUID id, @Param("reason") String reason);

    @Query("""
            select b.broadcastId from BroadcastEntity b
            where b.status = :sending and (b.claimedAt is null or b.claimedAt < :staleBefore)
            order by b.createdAt
            """)
    List<UUID> findStalled(@Param("sending") BroadcastStatus sending, @Param("staleBefore") LocalDateTime staleBefore);
}
