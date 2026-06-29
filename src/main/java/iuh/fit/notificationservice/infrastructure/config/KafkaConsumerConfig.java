package iuh.fit.notificationservice.infrastructure.config;

import iuh.fit.notificationservice.infrastructure.external.kafka.dto.OwnerApplicationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String trustedPackages;
    @Value("${spring.kafka.consumer.properties.spring.json.value.default.type}")
    private String defaultType;
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    @Bean
    public ConsumerFactory<String, OwnerApplicationEvent> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put("spring.json.trusted.packages", trustedPackages);
        configProps.put("spring.json.use.type.headers", false);
        configProps.put("spring.json.value.default.type", defaultType);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OwnerApplicationEvent> listenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OwnerApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
