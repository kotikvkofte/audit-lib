package org.ex9.auditlib.property;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Класс для хранения настроек Kafka из application.properties или application.yml.
 * <p>
 * Пример конфигурации:
 * <pre>
 * audit:
 *   kafka:
 *     topic: audit-topic
 * </pre>
 * </p>
 * @author Краковцев Артём
 */
@ConfigurationProperties(prefix = "audit.kafka")
@Log4j2
@Data
public class AuditKafkaProperties {

    /** Название Kafka топика для отправки логов. */
    private String topic = "audit-log";

    /**
     * Инициализирует настройки и логирует их значения.
     */
    @PostConstruct
    public void init() {
        log.info("KafkaProperties init {}", this);
    }

}
