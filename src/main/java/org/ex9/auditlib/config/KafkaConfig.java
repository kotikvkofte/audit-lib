package org.ex9.auditlib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.service.KafkaPublishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Kafka.
 * <p>
 * Настраивает продюсера Kafka с семантикой exactly-once.
 * Используется для отправки логов в формате JSON в топик Kafka из {@link AuditKafkaProperties}.
 * </p>
 * @author Краковецв Артём
 */
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:}")
    private String bootstrapServer;

    @Value("${audit.kafka.transactional-id-prefix:audit-lib-tx-}")
    private String transactionalIdPrefix;

    private final AuditLogProperties auditLogProperties;

    /**
     * Создаёт фабрику продюсера с настройками для семантики exactly-once.
     *
     * @return фабрика продюсера
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(configProps);
        factory.setTransactionIdPrefix(transactionalIdPrefix);

        return factory;
    }

    /**
     * Создаёт шаблон Kafka для отправки сообщений.
     *
     * @return экземпляр {@link KafkaTemplate}
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Создаёт бин сервиса для отправки сообщений в Kafka.
     *
     * @param kafkaTemplate шаблон Kafka для отправки сообщений
     * @param props настройки Kafka из {@link AuditKafkaProperties}
     * @return экземпляр {@link KafkaPublishService}
     */
    @Bean
    public KafkaPublishService kafkaPublishService(KafkaTemplate<String, String> kafkaTemplate, AuditKafkaProperties props) {
        return new KafkaPublishService(kafkaTemplate, props, new ObjectMapper());
    }

}
