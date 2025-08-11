package org.ex9.auditlib.property;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.util.LogMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Класс для хранения настроек логирования из application.properties или application.yml.
 * <p>
 * Поддерживает настройку режимов логирования (консоль, файл, Kafka) и активацию логирования.
 * </p>
 * <p>
 * Пример конфигурации:
 * <pre>
 * audit:
 *   logging:
 *     enabled: true
 *     modes:
 *       - CONSOLE
 *       - FILE
 *       - KAFKA
 * </pre>
 * </p>
 * @author Краковцев Артём
 */
@ConfigurationProperties(prefix = "audit.logging")
@Log4j2
@Data
public class AuditLogProperties {

    /** Список режимов логирования (CONSOLE, FILE, KAFKA). */
    private List<LogMode> modes;

    /** Флаг активации логирования. */
    private boolean enabled;

    /**
     * Включено ли логирование в Kafka.
     *
     * @return true, если режим KAFKA включён
     */
    public boolean isKafkaIncluded() {
        return modes.contains(LogMode.KAFKA);
    }

    /**
     * Инициализирует настройки и логирует их значения.
     */
    @PostConstruct
    public void init() {
       log.info("AuditLogProperties init {}", this);
    }

}
