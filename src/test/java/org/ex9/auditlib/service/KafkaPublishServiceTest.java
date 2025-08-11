package org.ex9.auditlib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.apache.logging.log4j.Logger;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPublishServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AuditKafkaProperties auditKafkaProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaPublishService kafkaPublishService;

    private StringWriter logCapture;
    private Appender appender;

    @BeforeEach
    void setUp() {
        logCapture = new StringWriter();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%m%n").build();
        appender = WriterAppender.createAppender(layout, null, logCapture, "TestAppender", false, true);
        appender.start();
        config.addAppender(appender);
        context.getRootLogger().addAppender(appender);
        context.updateLoggers();
    }

    @AfterEach
    void tearDown() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getRootLogger().removeAppender(appender);
        context.updateLoggers();
        appender.stop();
    }

    private static class ArgsObj {
        private String name;
        private int value;

        public ArgsObj(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    void sendAuditDto_shouldSendToKafka() throws JsonProcessingException {
        AuditDto auditDto = AuditDto.builder()
                .id("test-id")
                .type("START")
                .logLevel("INFO")
                .args(new Object[]{"test"})
                .methodName("TestClass.testMethod")
                .build();
        when(auditKafkaProperties.getTopic()).thenReturn("audit-topic");
        when(objectMapper.writeValueAsString(auditDto)).thenReturn("serialized-dto");
        when(kafkaTemplate.executeInTransaction(any())).thenReturn(true);

        kafkaPublishService.send(auditDto);

        verify(objectMapper).writeValueAsString(auditDto);
        verify(kafkaTemplate).executeInTransaction(any());
    }

    @Test
    void sendHttpLogDto_shouldSendToKafka() throws JsonProcessingException {
        HttpLogDto httpLogDto = HttpLogDto.builder()
                .direction("Incoming")
                .method("GET")
                .url("/test")
                .statusCode(200)
                .build();
        when(auditKafkaProperties.getTopic()).thenReturn("audit-topic");
        when(objectMapper.writeValueAsString(httpLogDto)).thenReturn("serialized-dto");
        when(kafkaTemplate.executeInTransaction(any())).thenReturn(true);

        kafkaPublishService.send(httpLogDto);

        verify(objectMapper).writeValueAsString(httpLogDto);
        verify(kafkaTemplate).executeInTransaction(any());
    }

    @Test
    void sendAuditDto_withJsonProcessingException_shouldLogError() throws JsonProcessingException {
        AuditDto auditDto = AuditDto.builder()
                .id("test-id")
                .type("START")
                .methodName("TestClass.testMethod")
                .logLevel("INFO")
                .args(null)
                .timestamp("2025-08-11T16:10:38.915613700")
                .build();
        when(auditKafkaProperties.getTopic()).thenReturn("audit-topic");
        doThrow(new JsonProcessingException("Serialization error") {}).when(objectMapper).writeValueAsString(auditDto);

        kafkaPublishService.send(auditDto);

        verify(objectMapper).writeValueAsString(auditDto);
        String logOutput = logCapture.toString();
        assertTrue(logOutput.contains("Serialize auditDto error"));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendHttpLogDto_withJsonProcessingException_shouldLogError() throws JsonProcessingException {
        HttpLogDto httpLogDto = HttpLogDto.builder()
                .direction("Incoming")
                .method("GET")
                .url("/test")
                .statusCode(200)
                .build();
        when(auditKafkaProperties.getTopic()).thenReturn("audit-topic");
        doThrow(new JsonProcessingException("Serialization error") {}).when(objectMapper).writeValueAsString(httpLogDto);

        kafkaPublishService.send(httpLogDto);

        verify(objectMapper).writeValueAsString(httpLogDto);
        String logOutput = logCapture.toString();
        assertTrue(logOutput.contains("Serialize httpLogDto error"));
        verifyNoInteractions(kafkaTemplate);
    }

}