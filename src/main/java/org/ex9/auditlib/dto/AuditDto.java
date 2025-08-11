package org.ex9.auditlib.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для передачи данных в логи и Kafka.
 * @author Краковев Артём
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDto {

    /** Уникальный идентификатор события. */
    private String id;

    /** Тип события (START/END/ERROR). */
    private String type;

    /** Название метода (className.methodName). */
    private String methodName;

    /** Аргументы метода (для события START). */
    private Object[] args;

    /** Результат выполнения метода (для события END). */
    private Object result;

    /** Текст ошибки (для события ERROR). */
    private String error;

    /** Уровень логирования (INFO, DEBUG и т.д.). */
    private String logLevel;

    /** Время события. По умолчанию текущая дата и время. */
    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();

}
