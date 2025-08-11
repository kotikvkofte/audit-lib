package org.ex9.auditlib.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.ex9.auditlib.annotation.AuditLog;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.service.KafkaPublishService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogProperties auditLogProperties;

    @Mock
    private KafkaPublishService kafkaPublishService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private AuditLog auditLog;

    @InjectMocks
    private AuditLogAspect auditLogAspect;

    @Test
    @DisplayName("logStart - логирование начала выполнения метода без Kafka")
    void logStartTest_WithoutKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.DEBUG);
        when(auditLogProperties.isKafkaIncluded()).thenReturn(false);

        auditLogAspect.logStart(joinPoint, auditLog);

        verify(kafkaPublishService, never()).send(any(AuditDto.class));
    }

    @Test
    @DisplayName("logEnd - логирование успешного завершения метода с Kafka")
    void logEndTest_WithKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);
        when(auditLogProperties.isKafkaIncluded()).thenReturn(true);

        auditLogAspect.logStart(joinPoint, auditLog);

        Object result = "test result";

        auditLogAspect.logEnd(joinPoint, auditLog, result);

        ArgumentCaptor<AuditDto> captor = ArgumentCaptor.forClass(AuditDto.class);
        verify(kafkaPublishService, times(2)).send(captor.capture());

        AuditDto endDto = captor.getAllValues().get(1);
        assertEquals("END", endDto.getType());
        assertEquals("com.example.TestClass.testMethod", endDto.getMethodName());
        assertEquals("INFO", endDto.getLogLevel());
        assertEquals(result, endDto.getResult());
        assertNotNull(endDto.getId());
    }

    @Test
    @DisplayName("logError - логирование ошибки выполнения метода с Kafka")
    void logErrorTest_WithKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.ERROR);
        when(auditLogProperties.isKafkaIncluded()).thenReturn(true);

        auditLogAspect.logStart(joinPoint, auditLog);

        Exception exception = new RuntimeException("Test error");

        auditLogAspect.logError(joinPoint, auditLog, exception);

        ArgumentCaptor<AuditDto> captor = ArgumentCaptor.forClass(AuditDto.class);
        verify(kafkaPublishService, times(2)).send(captor.capture());

        AuditDto errorDto = captor.getAllValues().get(1);
        assertEquals("ERROR", errorDto.getType());
        assertEquals("com.example.TestClass.testMethod", errorDto.getMethodName());
        assertEquals("ERROR", errorDto.getLogLevel());
        assertEquals("Test error", errorDto.getError());
        assertNotNull(errorDto.getId());
    }

    @Test
    @DisplayName("logEnd - логирование успешного завершения метода без Kafka")
    void logEndTest_WithoutKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);
        when(auditLogProperties.isKafkaIncluded()).thenReturn(false);

        Object result = "test result";

        auditLogAspect.logEnd(joinPoint, auditLog, result);

        verify(kafkaPublishService, never()).send(any(AuditDto.class));
    }

    @Test
    @DisplayName("logError - логирование ошибки выполнения метода без Kafka")
    void logErrorTest_WithoutKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(auditLog.logLevel()).thenReturn(LogLevel.ERROR);
        when(auditLogProperties.isKafkaIncluded()).thenReturn(false);

        Exception exception = new RuntimeException("Test error");

        auditLogAspect.logError(joinPoint, auditLog, exception);

        verify(kafkaPublishService, never()).send(any(AuditDto.class));
    }

    @Test
    @DisplayName("Проверка различных уровней логирования")
    void logStartTest_differentLogLevels_withKafka() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLogProperties.isKafkaIncluded()).thenReturn(true);

        LogLevel[] levels = {LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR};

        for (LogLevel level : levels) {
            when(auditLog.logLevel()).thenReturn(level);

            auditLogAspect.logStart(joinPoint, auditLog);

            ArgumentCaptor<AuditDto> captor = ArgumentCaptor.forClass(AuditDto.class);
            verify(kafkaPublishService, atLeastOnce()).send(captor.capture());

            AuditDto dto = captor.getValue();
            assertEquals(level.toString(), dto.getLogLevel());
        }
    }

}