package org.ex9.auditlib.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.service.KafkaPublishService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Перехватчик для логирования исходящих HTTP-запросов.
 * @author Краковев Артём
 */
@RequiredArgsConstructor
@Log4j2
@Component
public class OutgoingInterceptor implements ClientHttpRequestInterceptor {

    private final KafkaPublishService kafkaPublishService;

    /**
     * Перехватывает исходящий HTTP-запрос и логирует его параметры.
     *
     * @param request HTTP-запрос
     * @param body тело запроса
     * @param execution выполнение запроса
     * @return HTTP-ответ
     * @throws IOException ошибка ввода-вывода
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        ClientHttpResponse response = execution.execute(request, body);

        String requestBody = new String(body, StandardCharsets.UTF_8);
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

        HttpLogDto dto = HttpLogDto.builder()
                .timestamp(LocalDateTime.now().toString())
                .direction("Outgoing")
                .method(request.getMethod().name())
                .url(request.getURI().toString())
                .statusCode(response.getStatusCode().value())
                .responseBody(responseBody)
                .requestBody(requestBody)
                .build();

        log.debug(dto);
        kafkaPublishService.send(dto);

        return response;
    }
}
