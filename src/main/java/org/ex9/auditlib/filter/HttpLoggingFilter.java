package org.ex9.auditlib.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.dto.HttpLogDto;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

/**
 * Фильтр для логирования входящих HTTP-запросов.
 * @author Краковев Артём
 */
@Log4j2
public class HttpLoggingFilter extends OncePerRequestFilter {

    /**
     * Обрабатывает входящий HTTP-запрос и вызывает
     * {@link #logRequestAndResponse(ContentCachingRequestWrapper, ContentCachingResponseWrapper)}}.
     *
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException ошибка сервлета
     * @throws IOException ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequestAndResponse(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Логирует параметры запроса и ответа.
     *
     * @param request кэшированный HTTP-запрос
     * @param response кэшированный HTTP-ответ
     * @throws UnsupportedEncodingException кодировка не поддерживается
     */
    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response) throws UnsupportedEncodingException {
        String requestBody = new String(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = new String(response.getContentAsByteArray(), response.getCharacterEncoding());

        HttpLogDto dto = HttpLogDto.builder()
                .timestamp(LocalDateTime.now().toString())
                .direction("Incoming")
                .method(request.getMethod())
                .url(request.getRequestURI() +
                        (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                .statusCode(response.getStatus())
                .requestBody(requestBody)
                .responseBody(responseBody)
                .build();

        log.info(dto);
    }

}
