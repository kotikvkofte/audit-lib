package org.ex9.auditlib.util;

import org.ex9.auditlib.property.AuditLogProperties;

/**
 * Перечисление, определяющее режимы логирования.
 * <p>
 * Используется для указания, куда выводить логи: в консоль, файл или Kafka.
 * Применяется в настройках {@link AuditLogProperties} для конфигурации логирования через application.properties или application.yml.
 * </p>
 * <p>
 * Пример конфигурации:
 * <pre>
 * audit:
 *   logging:
 *     modes:
 *       - CONSOLE
 *       - FILE
 *       - KAFKA
 * </pre>
 * </p>
 * @author Краковцев Артём
 */
public enum LogMode {

    /**
     * Режим логирования в консоль.
     */
    CONSOLE ("Console"),

    /**
     * Режим логирования в файл с ротацией по 1 МБ.
     */
    FILE ("File"),

    /**
     * Режим логирования в Kafka с семантикой exactly-once.
     */
    KAFKA ("Kafka");

    private final String value;

    /**
     * Конструктор перечисления.
     *
     * @param value строковое представление режима логирования
     */
    LogMode(String value) {
        this.value = value;
    }

    /**
     * Возвращает строковое представление режима логирования.
     *
     * @return строковое значение режима
     */
    @Override
    public String toString() {
        return value;
    }

}
