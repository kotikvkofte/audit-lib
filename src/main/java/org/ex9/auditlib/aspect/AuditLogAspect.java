package org.ex9.auditlib.aspect;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.ex9.auditlib.annotation.AuditLog;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.dto.AuditDto;
import org.ex9.auditlib.service.KafkaPublishService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.UUID;

/**
 * Аспект для обработки методов, аннотированных {@link AuditLog}.
 * <p>
 * Логирует события начала выполнения метода (START), успешного завершения (END) и завершения с ошибкой (ERROR).
 * Поддерживает вывод логов в консоль, файл (с ротацией по 1 МБ)
 * и Kafka (в формате JSON с семантикой exactly-once).
 * </p>
 * @author Краковцев Артём
 */
@Aspect
@Log4j2
public class AuditLogAspect {

    @Autowired
    private AuditLogProperties auditLogProperties;

    @Autowired
    private KafkaPublishService kafkaPublishService;

    private static UUID ID;

    /**
     * Логирует начало выполнения метода.
     * <p>
     * Создаёт уникальный идентификатор и логирует информацию о методе перед его выполнением.
     * Если включено логирование в Kafka, отправляет данные в формате {@link AuditDto}.
     * </p>
     *
     * @param joinPoint точка соединения для метода
     * @param auditLog аннотация с параметрами логирования
     */
    @Before("@annotation(auditLog)")
    public void logStart(JoinPoint joinPoint, AuditLog auditLog) {
        ID = UUID.randomUUID();

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String args = Arrays.toString(joinPoint.getArgs());
        Level logLevel = Level.toLevel(auditLog.logLevel().toString());

        String message = String.format("START %s %s.%s \n args = %s", ID, className, methodName, args);

        log.log(logLevel, message);

        if (auditLogProperties.isKafkaIncluded()) {

            var dto = AuditDto.builder()
                    .type("START")
                    .id(ID.toString())
                    .args(joinPoint.getArgs())
                    .logLevel(logLevel.toString())
                    .methodName(String.format("%s.%s", className, methodName))
                    .build();

            kafkaPublishService.send(dto);
        }

    }

    /**
     * Логирует успешное завершение метода.
     * <p>
     * Логирует завершение выполнения метода с возвращаемым значением.
     * Если включено логирование в Kafka, отправляет данные в формате {@link AuditDto}.
     * </p>
     *
     * @param joinPoint точка соединения для метода
     * @param auditLog аннотация с параметрами логирования
     * @param result возвращаемое значение метода
     */
    @AfterReturning(value = "@annotation(auditLog)",
            returning = "result")
    public void logEnd(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        Level logLevel = Level.toLevel(auditLog.logLevel().toString());

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String message = String.format("END %s %s.%s result = %s", ID, className, methodName, result);

        log.log(logLevel, message);
        if (auditLogProperties.isKafkaIncluded()) {

            var dto = AuditDto.builder()
                    .type("END")
                    .id(ID.toString())
                    .result(result)
                    .logLevel(logLevel.toString())
                    .methodName(String.format("%s.%s", className, methodName))
                    .build();

            kafkaPublishService.send(dto);
        }
    }

    /**
     * Логирует завершение метода с ошибкой.
     * <p>
     * Логирует ошибку выполнения метода.
     * Если включено логирование в Kafka, отправляет данные в формате {@link AuditDto}.
     * </p>
     *
     * @param joinPoint точка соединения для метода
     * @param auditLog аннотация с параметрами логирования
     * @param ex исключение, вызвавшее ошибку
     */
    @AfterThrowing(value = "@annotation(auditLog)",
            throwing = "ex")
    public void logError(JoinPoint joinPoint, AuditLog auditLog, Throwable ex) {

        Level logLevel = Level.toLevel(auditLog.logLevel().toString());

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String message = String.format("ERROR %s %s.%s error = %s", ID, className, methodName, ex);

        log.log(logLevel, message);

        if (auditLogProperties.isKafkaIncluded()) {

            var dto = AuditDto.builder()
                    .type("ERROR")
                    .id(ID.toString())
                    .error(ex.getMessage())
                    .logLevel(logLevel.toString())
                    .methodName(String.format("%s.%s", className, methodName))
                    .build();

            kafkaPublishService.send(dto);
        }
    }

}
