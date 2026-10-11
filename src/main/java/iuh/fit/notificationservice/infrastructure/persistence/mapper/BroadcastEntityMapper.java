package iuh.fit.notificationservice.infrastructure.persistence.mapper;

import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.infrastructure.persistence.entity.BroadcastEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BroadcastEntityMapper {
    BroadcastEntity toEntity(Broadcast domain);

    Broadcast toDomain(BroadcastEntity entity);
}
