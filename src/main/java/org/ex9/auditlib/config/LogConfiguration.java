package org.ex9.auditlib.config;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.service.KafkaPublishService;
import org.ex9.auditlib.util.LogMode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Конфигурация логгеров.
 * <p>
 * Настраивает аппендеры Log4j2 для вывода логов в консоль и файл (с ротацией по 1 МБ)
 * на основе {@link AuditLogProperties}. Kafka логирование обрабатывается отдельно
 * через {@link KafkaPublishService}.
 * </p>
 * @author Краковев Артём
 */
@Component
@RequiredArgsConstructor
public class LogConfiguration implements InitializingBean {

    private final AuditLogProperties auditLogProperties;

    /**
     * Инициализирует и добавляет аппендеры на основе настроек {@link AuditLogProperties}
     * для режимов логирования (консоль, файл).
     * @throws Exception ошибка при конфигурации логгеров
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        auditLogProperties.getModes().forEach(mode -> {
            if (mode != LogMode.KAFKA) {
                Appender appender = config.getAppender(mode.toString());
                config.getRootLogger().addAppender(appender, null, null);
            }
        });

        context.updateLoggers();

    }
}
