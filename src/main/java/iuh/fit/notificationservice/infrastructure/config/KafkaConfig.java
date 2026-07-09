package iuh.fit.notificationservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topic.owner-application}")
    private String ownerApplicationTopic;

    @Bean
    public NewTopic ownerApplicationTopic() {
        return TopicBuilder.name(ownerApplicationTopic)
                .partitions(3)
                .replicas(2)
                .build();
    }
}
