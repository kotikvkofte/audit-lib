package org.ex9.auditlib.appender;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.service.KafkaPublishService;

import java.io.Serializable;
import java.util.UUID;

/**
 * Кастомный аппендер для работы с кафкой.
 *
 * @author Краковцев Артём
 */
@Plugin(name = "KafkaAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class KafkaAppender extends AbstractAppender {

    private static final String KAFKA_LOGGING_KEY = "kafkaLogging";
    private static final String KAFKA_LOGGING_VALUE = "true";

    private final KafkaPublishService kafkaPublishService;

    public KafkaAppender(String name,
                         Filter filter,
                         Layout<? extends Serializable> layout,
                         boolean ignoreExceptions,
                         Property[] properties,
                         KafkaPublishService kafkaPublishService) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.kafkaPublishService = kafkaPublishService;
    }

    /**
     * Фабричный метод для создания экземпляра {@link KafkaAppender}.
     *
     * @param name                имя аппендера
     * @param ignoreExceptions    флаг игнорирования исключений
     * @param layout              макет для форматирования логов
     * @param filter              фильтр для обработки логов
     * @param properties          дополнительные свойства конфигурации
     * @param kafkaPublishService сервис для отправки сообщений в Kafka
     * @return новый экземпляр {@link KafkaAppender}
     */
    @PluginFactory
    public static KafkaAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) boolean ignoreExceptions,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Properties") Property[] properties,
            @PluginElement("KafkaPublishService") KafkaPublishService kafkaPublishService) {
        if (name == null) {
            LOGGER.error("No name provided for KafkaAppender");
            return null;
        }
        if (kafkaPublishService == null) {
            LOGGER.error("No KafkaPublishService provided for KafkaAppender");
            return null;
        }
        if (layout == null) {
            LOGGER.error("No layout provided for KafkaAppender");
            return null;
        }

        KeyValuePair[] keyValuePairs = new KeyValuePair[]{
                new KeyValuePair(KAFKA_LOGGING_KEY, KAFKA_LOGGING_VALUE)
        };
        Filter recursionFilter = ThreadContextMapFilter.createFilter(
                keyValuePairs,
                "and",
                Filter.Result.DENY,
                Filter.Result.ACCEPT
        );

        Filter finalFilter = filter != null ? CompositeFilter.createFilters(new Filter[]{filter, recursionFilter}) : recursionFilter;

        return new KafkaAppender(name, finalFilter, layout, ignoreExceptions, properties, kafkaPublishService);
    }

    @Override
    public void append(LogEvent event) {
        try {
            Object[] params = event.getMessage().getParameters();
            if (params != null && params.length > 0) {
                Object obj = params[0];
                if (obj instanceof AuditDto auditDto) {
                    kafkaPublishService.send(auditDto);
                    return;
                }
                if (obj instanceof HttpLogDto httpLogDto) {
                    httpLogDto.setMessageId(UUID.randomUUID().toString());
                    kafkaPublishService.send(httpLogDto);
                    return;
                }
                throw new AppenderLoggingException("Unhandled parameter: " + obj);
            }
        } catch (Exception e) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException("Error sending log to Kafka", e);
            }
        }
    }

}
