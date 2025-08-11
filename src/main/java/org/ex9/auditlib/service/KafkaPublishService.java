package org.ex9.auditlib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
     * Отправляет данные в Kafka.
     *
     * @param auditDto данные
     */
    public void send(AuditDto auditDto) {
        try {
            String topic = auditKafkaProperties.getTopic();
            String message = objectMapper.writeValueAsString(auditDto);
            kafkaTemplate.executeInTransaction(ops -> {
                ops.send(topic, auditDto.getId(), message);
                return true;
            });
        } catch (JsonProcessingException e) {
            log.error("Serialize auditDto error", e);
        }
    }

    /**
     * Отправляет данные HTTP-запроса в Kafka.
     *
     * @param httpLogDto данные HTTP-запроса
     */
    public void send(HttpLogDto httpLogDto) {
        try {
            String topic = auditKafkaProperties.getTopic();
            String message = objectMapper.writeValueAsString(httpLogDto);
            kafkaTemplate.executeInTransaction(ops -> {
                ops.send(topic, UUID.randomUUID().toString(), message);
                return true;
            });
        } catch (JsonProcessingException e) {
            log.error("Serialize httpLogDto error", e);
        }
    }

}
