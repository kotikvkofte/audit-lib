package org.ex9.auditlib.annotation;

import org.springframework.boot.logging.LogLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для логирования выполнения методов в Spring Boot приложениях.
 * <p>
 * Используется для логирования событий начала выполнения метода (START), успешного завершения (END)
 * или завершения с ошибкой (ERROR).
 * </p>
 * <p>
 * Пример использования:
 * <pre>
 *     &#64;AuditLog(logLevel = LogLevel.DEBUG)
 *     public String processData(String input) {
 *         return input.toUpperCase();
 *     }
 * </pre>
 * </p>
 * @author Краковцев Артём
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * Уровень логирования для событий, связанных с методом.
     * По умолчанию используется уровень INFO.
     *
     * @return уровень логирования
     */
    LogLevel logLevel() default LogLevel.INFO;
}
