package iuh.fit.notificationservice.infrastructure.persistence.mapper;

import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationEntityMapper {

    NotificationEntity createEntity(Notification domain);

    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(Notification domain, @MappingTarget NotificationEntity entity);

    Notification toDomain(NotificationEntity entity);
}
