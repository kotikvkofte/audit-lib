package org.ex9.auditlib.config;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.ex9.auditlib.appender.AppenderFabric;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.service.KafkaPublishService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private final KafkaPublishService kafkaPublishService;

    /**
     * Инициализирует и добавляет аппендеры на основе настроек {@link AuditLogProperties}
     * для режимов логирования (консоль, файл, кафка).
     * @throws Exception ошибка при конфигурации логгеров
     */
    @Override
    public void afterPropertiesSet() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        auditLogProperties.getModes().forEach(mode -> {
            Appender appender = AppenderFabric.getAppender(mode, config, kafkaPublishService);
            config.getRootLogger().addAppender(appender, null, null);
        });

        context.updateLoggers();
    }

}
