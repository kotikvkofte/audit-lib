package org.ex9.auditlib.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * DTO для передачи данных аудита HTTP-запросов в логах и Kafka.
 * @author Краковев Артём
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpLogDto implements LogDto {

    /** Время запроса. */
    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();

    /** Направление запроса (Incoming/Outgoing). */
    private String direction;

    /** HTTP-метод (GET, POST и т.д.). */
    private String method;

    /** Код статуса HTTP-запроса (200, 400 и т.д.). */
    private int statusCode;

    /** URL запроса, включая параметры. */
    private String url;

    /** Тело запроса. */
    private String requestBody;

    /** Тело ответа. */
    private String responseBody;

    /**
     * Возвращает строковое представление лога для вывода в консоль или файл.
     *
     * @return строка лога в формате:
     * direction method statusCode url RequestBody = {requestBody} ResponseBody = {responseBody}
     */
    @JsonIgnore
    public String getLog() {
        StringJoiner log = new StringJoiner(" ");
        log.add(direction);
        log.add(method);
        log.add(Integer.toString(statusCode));
        log.add(url);
        log.add("RequestBody = {");
        log.add(requestBody);
        log.add("}");
        log.add("ResponseBody = {");
        log.add(responseBody);
        log.add("}");

        return log.toString();
    }

}
