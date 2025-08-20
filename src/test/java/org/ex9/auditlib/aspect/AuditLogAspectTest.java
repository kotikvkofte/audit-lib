package org.ex9.auditlib.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.ex9.auditlib.annotation.AuditLog;
import org.ex9.auditlib.dto.AuditDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private AuditLog auditLog;

    private AuditLogAspect auditLogAspect;
    private TestAppender testAppender;

    private static class TestAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        protected TestAppender() {
            super("TestAppender", null, null, true, null);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        public void clear() {
            events.clear();
        }

        public List<AuditDto> getAuditDtos() {
            return events.stream()
                    .map(event -> {
                        Object[] params = event.getMessage().getParameters();
                        if (params != null && params.length > 0 && params[0] instanceof AuditDto) {
                            return (AuditDto) params[0];
                        }
                        return null;
                    })
                    .filter(dto -> dto != null)
                    .toList();
        }
    }

    @BeforeEach
    void setUp() {
        auditLogAspect = new AuditLogAspect();

        testAppender = new TestAppender();
        testAppender.start();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        config.addAppender(testAppender);
        context.getRootLogger().addAppender(testAppender);
        context.updateLoggers();
    }

    @AfterEach
    void tearDown() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getRootLogger().removeAppender(testAppender);
        context.updateLoggers();
        testAppender.stop();
        testAppender.clear();
    }

    @Test
    void logStartTest() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        when(auditLog.logLevel()).thenReturn(LogLevel.DEBUG);

        auditLogAspect.logStart(joinPoint, auditLog);

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(1, auditDtos.size());

        AuditDto dto = auditDtos.get(0);
        assertEquals("START", dto.getType());
        assertEquals("com.example.TestClass.testMethod", dto.getMethodName());
        assertEquals("DEBUG", dto.getLogLevel());
        assertNotNull(dto.getId());
        assertNotNull(dto.getMessageId());
        assertNotNull(dto.getTimestamp());
        assertArrayEquals(new Object[]{"arg1", "arg2"}, dto.getArgs());
    }

    @Test
    void logEndTest() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        Object result = "test result";

        auditLogAspect.logStart(joinPoint, auditLog);
        auditLogAspect.logEnd(joinPoint, auditLog, result);

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(2, auditDtos.size());

        AuditDto startDto = auditDtos.get(0);
        AuditDto endDto = auditDtos.get(1);

        assertEquals("START", startDto.getType());
        assertEquals("INFO", startDto.getLogLevel());

        assertEquals("END", endDto.getType());
        assertEquals("com.example.TestClass.testMethod", endDto.getMethodName());
        assertEquals("INFO", endDto.getLogLevel());
        assertEquals(result, endDto.getResult());
        assertEquals(startDto.getId(), endDto.getId());
        assertNotNull(endDto.getMessageId());
        assertNotNull(endDto.getTimestamp());
    }

    @Test
    void logErrorTest() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.ERROR);

        Exception exception = new RuntimeException("Test error");

        auditLogAspect.logStart(joinPoint, auditLog);
        auditLogAspect.logError(joinPoint, auditLog, exception);

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(2, auditDtos.size());

        AuditDto startDto = auditDtos.get(0);
        AuditDto errorDto = auditDtos.get(1);

        assertEquals("START", startDto.getType());
        assertEquals("ERROR", startDto.getLogLevel());

        assertEquals("ERROR", errorDto.getType());
        assertEquals("com.example.TestClass.testMethod", errorDto.getMethodName());
        assertEquals("ERROR", errorDto.getLogLevel());
        assertEquals("Test error", errorDto.getError());
        assertEquals(startDto.getId(), errorDto.getId());
        assertNotNull(errorDto.getMessageId());
        assertNotNull(errorDto.getTimestamp());
    }

    @Test
    void logStartTest_withEmptyArgs() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        auditLogAspect.logStart(joinPoint, auditLog);

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(1, auditDtos.size());

        AuditDto dto = auditDtos.get(0);
        assertEquals("START", dto.getType());
        assertNotNull(dto.getArgs());
        assertEquals(0, dto.getArgs().length);
    }

    @Test
    void logStartTest_withNullArgs() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(null);
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        auditLogAspect.logStart(joinPoint, auditLog);

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(1, auditDtos.size());

        AuditDto dto = auditDtos.get(0);
        assertEquals("START", dto.getType());
        assertNull(dto.getArgs());
    }

    @Test
    void testUniqueMessageId() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        auditLogAspect.logStart(joinPoint, auditLog);
        auditLogAspect.logEnd(joinPoint, auditLog, "result");

        List<AuditDto> auditDtos = testAppender.getAuditDtos();
        assertEquals(2, auditDtos.size());

        AuditDto startDto = auditDtos.get(0);
        AuditDto endDto = auditDtos.get(1);

        assertNotEquals(startDto.getMessageId(), endDto.getMessageId());
        assertEquals(startDto.getId(), endDto.getId());
    }

}
