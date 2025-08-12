package org.ex9.auditlib.appender;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Property;
import org.ex9.auditlib.layout.AuditJsonLayout;
import org.ex9.auditlib.layout.AuditStringLayout;
import org.ex9.auditlib.service.KafkaPublishService;
import org.ex9.auditlib.util.LogMode;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * Фабрика аппендеров для логирования.
 *
 * @author Краковцев Артём
 */
@Log4j2
public class AppenderFabric {

    public static Appender getAppender(LogMode logMode, Configuration config, KafkaPublishService kafkaPublishService) {
        return switch (logMode) {
            case CONSOLE -> createConsoleAppender(config);
            case FILE -> createFileAppender(config);
            case KAFKA -> createKafkaAppender(config, kafkaPublishService);
        };
    }

    private static Appender createFileAppender(Configuration config) {
        AuditStringLayout layout = AuditStringLayout.createLayout();

        String logFilePath = "logs/audit.log"; // можно взять из application.yml
        SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy("1MB");

        RollingFileAppender fileAppender = RollingFileAppender.newBuilder()
                .withFileName(logFilePath)
                .withFilePattern("logs/audit-%d{yyyy-MM-dd-HH-mm}-%i.log.gz")
                .setName("File")
                .setLayout(layout)
                .withPolicy(policy)
                .setConfiguration(config)
                .build();
        fileAppender.start();

        return fileAppender;
    }

    private static Appender createConsoleAppender(Configuration config) {
        AuditStringLayout layout = AuditStringLayout.createLayout();

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("Console")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setConfiguration(config)
                .build();
        consoleAppender.start();

        return consoleAppender;
    }

    private static Appender createKafkaAppender(Configuration config, KafkaPublishService kafkaPublishService) {
        if (kafkaPublishService == null) {
            log.error("KafkaPublishService is not available.");
            return null;
        }

        try {
            AuditJsonLayout layout = AuditJsonLayout.createLayout();
            KafkaAppender kafkaAppender = KafkaAppender.createAppender(
                    "Kafka",
                    true,
                    layout,
                    null,
                    Property.EMPTY_ARRAY, kafkaPublishService);
            kafkaAppender.start();
            return kafkaAppender;
        } catch (Exception e) {
            log.error("Error creating Kafka appender: " + e.getMessage());
            return null;
        }
    }

}
