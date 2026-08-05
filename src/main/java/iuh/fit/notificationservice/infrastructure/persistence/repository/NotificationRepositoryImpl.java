package iuh.fit.notificationservice.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.notificationservice.application.dto.PageFilter;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;
import iuh.fit.notificationservice.infrastructure.persistence.mapper.NotificationEntityMapper;
import iuh.fit.notificationservice.infrastructure.persistence.repository.jpa.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JpaNotificationRepository jpaRepository;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final FilterParser filterParser;
    private final NotificationEntityMapper notificationEntityMapper;

    @Override
    public Notification save(Notification notification) throws ServiceException {
        NotificationEntity entity;
        if (notification.getNotificationId() != null) {
            entity = jpaRepository.findById(notification.getNotificationId())
                    .map(existing -> {
                        notificationEntityMapper.updateEntity(notification, existing);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Thông báo không tồn tại."));
        } else {
            entity = notificationEntityMapper.createEntity(notification);
        }
        NotificationEntity saved = jpaRepository.save(entity);
        return notificationEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(notificationEntityMapper::toDomain);
    }

    @Override
    public PageResult<Notification> findByReceiverId(PageFilter filter, UUID receiverId) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<NotificationEntity> spec = (root, query, cb) ->
                cb.equal(root.get("receiverId"), receiverId);

        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            Specification<NotificationEntity> parsedSpec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
            spec = spec.and(parsedSpec);
        }

        Page<NotificationEntity> entityPage = jpaRepository.findAll(spec, pageable);
        List<Notification> items = entityPage.getContent().stream()
                .map(notificationEntityMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getTotalElements(),
                filter.page(),
                filter.size(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public long countUnread(UUID receiverId) {
        return jpaRepository.countByReceiverIdAndStatus(receiverId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public void markAllReadByReceiverId(UUID receiverId) {
        jpaRepository.markAllReadByReceiverId(
                receiverId,
                NotificationStatus.READ,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public void deleteById(UUID id) throws ServiceException {
        if (!jpaRepository.existsById(id)) {
            throw new ServiceException("Thông báo không tồn tại.");
        }
        jpaRepository.deleteById(id);
    }
}
