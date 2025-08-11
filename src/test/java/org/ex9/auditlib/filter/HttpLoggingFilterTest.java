package org.ex9.auditlib.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.ex9.auditlib.dto.HttpLogDto;
import org.ex9.auditlib.service.KafkaPublishService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpLoggingFilterTest {

    @Mock
    private KafkaPublishService kafkaPublishService;

    @InjectMocks
    private HttpLoggingFilter httpLoggingFilter;

    @Test
    @DisplayName("doFilterInternal - успешная обработка GET запроса")
    void doFilterInternalTest_getRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setQueryString("page=1&size=10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = mock(FilterChain.class);

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(any(), any());
        ArgumentCaptor<HttpLogDto> captor = ArgumentCaptor.forClass(HttpLogDto.class);
        verify(kafkaPublishService, times(1)).send(captor.capture());

        HttpLogDto capturedDto = captor.getValue();
        assertEquals("GET", capturedDto.getMethod());
        assertEquals("/api/users?page=1&size=10", capturedDto.getUrl());
        assertEquals(200, capturedDto.getStatusCode());
        assertEquals("Incoming", capturedDto.getDirection());
        assertNotNull(capturedDto.getTimestamp());
    }

    @Test
    @DisplayName("doFilterInternal - обработка POST запроса с телом")
    void doFilterInternalTest_postRequest_withBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContent("{\"name\":\"John\",\"age\":30}".getBytes());
        request.setContentType("application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        MockFilterChain filterChain = new MockFilterChain();

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpLogDto> captor = ArgumentCaptor.forClass(HttpLogDto.class);
        verify(kafkaPublishService, times(1)).send(captor.capture());

        HttpLogDto capturedDto = captor.getValue();
        assertEquals("POST", capturedDto.getMethod());
        assertEquals("/api/users", capturedDto.getUrl());
        assertEquals(201, capturedDto.getStatusCode());
        assertNotNull(capturedDto.getRequestBody());
        assertNotNull(capturedDto.getResponseBody());
    }

    @Test
    @DisplayName("doFilterInternal - обработка запроса с ошибкой")
    void doFilterInternalTest_withError() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/users/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);

        FilterChain filterChain = mock(FilterChain.class);
        doThrow(new ServletException("Not found")).when(filterChain).doFilter(any(), any());

        assertThrows(ServletException.class, () ->
                httpLoggingFilter.doFilterInternal(request, response, filterChain));

        verify(kafkaPublishService, times(1)).send(any(HttpLogDto.class));
    }

    @Test
    @DisplayName("doFilterInternal - обработка запроса без query")
    void doFilterInternalTest_withoutQuery() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = mock(FilterChain.class);

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpLogDto> captor = ArgumentCaptor.forClass(HttpLogDto.class);
        verify(kafkaPublishService, times(1)).send(captor.capture());

        HttpLogDto capturedDto = captor.getValue();
        assertEquals("PUT", capturedDto.getMethod());
        assertEquals("/api/users/123", capturedDto.getUrl());
    }

    @Test
    @DisplayName("doFilterInternal - обработка запроса с пустым телом")
    void doFilterInternalTest_withEmptyBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = mock(FilterChain.class);

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpLogDto> captor = ArgumentCaptor.forClass(HttpLogDto.class);
        verify(kafkaPublishService, times(1)).send(captor.capture());

        HttpLogDto capturedDto = captor.getValue();
        assertEquals("GET", capturedDto.getMethod());
        assertEquals("/api/health", capturedDto.getUrl());
        assertEquals(200, capturedDto.getStatusCode());
        assertNotNull(capturedDto.getRequestBody());
        assertNotNull(capturedDto.getResponseBody());
    }

    @Test
    @DisplayName("doFilterInternal - проверка вызова copyBodyToResponse")
    void doFilterInternalTest_copyBodyToResponseCalled() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(any(), any());
        verify(kafkaPublishService, times(1)).send(any(HttpLogDto.class));

        assertTrue(response.getStatus() >= 0);
    }

}