package org.ex9.auditlib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * DTO для передачи данных в логи и Kafka.
 * @author Краковев Артём
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDto implements LogDto {

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

    @Override
    public String getLog() {
        StringJoiner log = new StringJoiner(" ");
        log.add("\n");
        log.add(timestamp);
        log.add(logLevel);
        log.add(type);
        log.add(id);
        log.add(getThirdValue());
        log.add(methodName);
        return log.toString();
    }

    private String getThirdValue() {
        return switch (type) {
            case "ERROR" -> "error = " + error;
            case "END" -> "result = " + result.toString();
            case "START" -> "args = " + Arrays.toString(args);
            default -> "";
        };
    }

}
