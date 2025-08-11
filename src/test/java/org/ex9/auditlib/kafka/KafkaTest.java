package org.ex9.auditlib.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.ex9.auditlib.service.KafkaPublishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
public class KafkaTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTemplate<String, String> mockKafkaTemplate;

    @Mock
    private ObjectMapper mockObjectMapper;

    private KafkaPublishService kafkaPublishService;
    private KafkaPublishService kafkaPublishServiceWithMocks;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AuditKafkaProperties auditKafkaProperties = new AuditKafkaProperties();
        auditKafkaProperties.setTopic("test-topic");

        kafkaPublishService = new KafkaPublishService(
                kafkaTemplate,
                auditKafkaProperties,
                new ObjectMapper()
        );

        kafkaPublishServiceWithMocks = new KafkaPublishService(
                mockKafkaTemplate,
                auditKafkaProperties,
                mockObjectMapper
        );
    }

    @Test
    @DisplayName("Отправка HttpLogDto в Kafka")
    void sendTest_withHttpLogDto() throws Exception {
        HttpLogDto httpLogDto = HttpLogDto.builder()
                .url("https://api.example.com")
                .method("GET")
                .statusCode(200)
                .requestBody("{\"param\":\"value\"}")
                .responseBody("{\"result\":\"success\"}")
                .timestamp(LocalDateTime.now().toString())
                .build();

        kafkaPublishService.send(httpLogDto);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup_" + UUID.randomUUID(), "true", embeddedKafka);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);

        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "test-topic");

        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        consumerRecords.forEach(records::add);

        ConsumerRecord<String, String> record = records.stream()
                .filter(r -> r.value().contains("https://api.example.com"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Record not found"));

        assertTrue(record.value().contains("https://api.example.com"));
        assertTrue(record.value().contains("GET"));
        assertNotNull(record.key());
    }

    @Test
    @DisplayName("Отправка нескольких сообщений подряд")
    void sendTest_sendMultipleMessages() throws Exception {
        AuditDto dto1 = AuditDto.builder()
                .id(UUID.randomUUID().toString())
                .type("START")
                .methodName("method1")
                .build();

        AuditDto dto2 = AuditDto.builder()
                .id(UUID.randomUUID().toString())
                .type("END")
                .methodName("method2")
                .build();

        kafkaPublishService.send(dto1);
        kafkaPublishService.send(dto2);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup_" + UUID.randomUUID(), "true", embeddedKafka);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);

        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer, java.time.Duration.ofSeconds(5));
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        consumerRecords.forEach(records::add);
        assertEquals(2, records.size());

        assertTrue(records.get(0).value().contains("method1"));
        assertTrue(records.get(1).value().contains("method2"));
    }

    @Test
    @DisplayName("Проверка обработки ошибок сериализации для AuditDto")
    void testSend_throwSerializationErrorHandling_auditDto() throws Exception {
        when(mockObjectMapper.writeValueAsString(any(AuditDto.class)))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        AuditDto dto = AuditDto.builder()
                .id(UUID.randomUUID().toString())
                .type("START")
                .methodName("testMethod")
                .build();

        assertDoesNotThrow(() -> kafkaPublishServiceWithMocks.send(dto));
        verify(mockKafkaTemplate, never()).executeInTransaction(any());
    }

    @Test
    @DisplayName("Проверка обработки ошибок сериализации для HttpLogDto")
    void testSend_throwSerializationErrorHandling_httpLogDto() throws Exception {
        when(mockObjectMapper.writeValueAsString(any(HttpLogDto.class)))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        HttpLogDto httpLogDto = HttpLogDto.builder()
                .url("https://test.com")
                .method("POST")
                .statusCode(500)
                .build();

        assertDoesNotThrow(() -> kafkaPublishServiceWithMocks.send(httpLogDto));
        verify(mockKafkaTemplate, never()).executeInTransaction(any());
    }

    @Test
    @DisplayName("Проверка транзакционности отправки AuditDto")
    void sendTest_transactionalSend_auditDto() throws Exception {
        when(mockObjectMapper.writeValueAsString(any(AuditDto.class)))
                .thenReturn("{\"id\":\"test\",\"type\":\"START\"}");

        AuditDto dto = AuditDto.builder()
                .id("test-id")
                .type("START")
                .methodName("testMethod")
                .build();

        kafkaPublishServiceWithMocks.send(dto);

        verify(mockKafkaTemplate, times(1)).executeInTransaction(any());
        verify(mockObjectMapper, times(1)).writeValueAsString(dto);
    }

    @Test
    @DisplayName("Проверка транзакционности отправки HttpLogDto")
    void sendTest_transactionalSend_httpLogDto() throws Exception {
        when(mockObjectMapper.writeValueAsString(any(HttpLogDto.class)))
                .thenReturn("{\"url\":\"test\",\"method\":\"GET\"}");

        HttpLogDto httpLogDto = HttpLogDto.builder()
                .url("https://test.com")
                .method("GET")
                .statusCode(200)
                .build();

        kafkaPublishServiceWithMocks.send(httpLogDto);

        verify(mockKafkaTemplate, times(1)).executeInTransaction(any());
        verify(mockObjectMapper, times(1)).writeValueAsString(httpLogDto);
    }

    @Test
    @DisplayName("Проверка отправки с пустыми полями")
    void sendTest_emptyFields() {
        AuditDto dto = AuditDto.builder()
                .id("")
                .type("")
                .methodName("")
                .build();

        assertDoesNotThrow(() -> kafkaPublishService.send(dto));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup_" + System.currentTimeMillis(), "true", embeddedKafka);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);

        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "test-topic");

        org.apache.kafka.clients.consumer.ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer, java.time.Duration.ofSeconds(10));
        List<ConsumerRecord<String, String>> records = new java.util.ArrayList<>();
        consumerRecords.forEach(records::add);

        ConsumerRecord<String, String> record = records.stream()
                .filter(r -> r.key() != null && r.key().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Record with empty key not found"));

        assertNotNull(record.value());
        assertTrue(record.key().isEmpty());
    }

    @Test
    @DisplayName("Проверка отправки с null значениями")
    void sendTest_nullValues() {
        AuditDto dto = AuditDto.builder()
                .id(UUID.randomUUID().toString())
                .type(null)
                .methodName(null)
                .build();

        assertDoesNotThrow(() -> kafkaPublishService.send(dto));
    }

    @Test
    @DisplayName("Проверка отправки больших сообщений")
    void sendTest_largeMessage() {
        String largeString = "a".repeat(10000);
        HttpLogDto httpLogDto = HttpLogDto.builder()
                .url("https://example.com")
                .method("POST")
                .statusCode(200)
                .requestBody(largeString)
                .responseBody(largeString)
                .build();

        assertDoesNotThrow(() -> kafkaPublishService.send(httpLogDto));
    }

    @Test
    @DisplayName("Проверка производительности отправки")
    void sendTest_sendManyTimes() throws InterruptedException {
        int messageCount = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            AuditDto dto = AuditDto.builder()
                    .id(UUID.randomUUID().toString())
                    .type("test")
                    .methodName("method " + i)
                    .build();
            kafkaPublishService.send(dto);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < TimeUnit.SECONDS.toMillis(30));
    }


}
