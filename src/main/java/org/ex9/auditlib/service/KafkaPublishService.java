package org.ex9.auditlib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.dto.LogDto;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Сервис для отправки логов в Kafka.
 * <p>
 * Сериализует {@link AuditDto} и {@link HttpLogDto} в JSON и отправляет в топик Kafka,
 * указанный в {@link AuditKafkaProperties}, с семантикой exactly-once.
 * </p>
 * @author Краковцев Артём
 */
@RequiredArgsConstructor
@Component
@Log4j2
public class KafkaPublishService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AuditKafkaProperties auditKafkaProperties;
    private final ObjectMapper objectMapper;

    /**
     * Отправляет данные в kafka в соответствующий топик.
     *
     * @param logDto данные
     */
    public void send(LogDto logDto) {
        try {
            String message = objectMapper.writeValueAsString(logDto);
            String topic = getTopicByDtoType(logDto);
            kafkaTemplate.executeInTransaction(ops -> {
                ops.send(topic, logDto.getMessageId(), message);
                return true;
            });
        } catch (JsonProcessingException e) {
            log.error("Serialize logDto error", e);
        } catch (IllegalStateException e) {
            log.error("Error searching for topic ", e);
        }
    }

    /**
     * Определяет топик по входным данным.
     * @param logDto входные данные
     * @return имя топика, соответствующего данным
     */
    private String getTopicByDtoType(LogDto logDto) {
        String topic = null;
        switch (logDto) {
            case HttpLogDto httpLogDto -> topic = auditKafkaProperties.getRequestsTopic();
            case AuditDto auditDto -> topic = auditKafkaProperties.getMethodsTopic();
            default -> throw new IllegalStateException("Unexpected value: " + logDto);
        }
        return topic;
    }

}
