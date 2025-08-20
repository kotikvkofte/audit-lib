package org.ex9.auditlib.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private final HttpLoggingFilter httpLoggingFilter = new HttpLoggingFilter();

    @Test
    void doFilterInternalTest_getRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.setQueryString("page=1&size=10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = new MockFilterChain();

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternalTest_postRequest_withBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setContent("{\"name\":\"John\",\"age\":30}".getBytes());
        request.setContentType("application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        response.getWriter().write("{\"id\":123,\"name\":\"John\"}");

        MockFilterChain filterChain = new MockFilterChain();

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getContentAsString());
    }

    @Test
    void doFilterInternalTest_withError() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);

        FilterChain filterChain = mock(FilterChain.class);
        doThrow(new ServletException("Not found")).when(filterChain).doFilter(any(), any());

        assertThrows(ServletException.class, () ->
                httpLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void doFilterInternalTest_withoutQuery() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = new MockFilterChain();

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternalTest_withEmptyBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain filterChain = new MockFilterChain();

        httpLoggingFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternalTest_differentHttpMethods() throws ServletException, IOException {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};

        for (String method : methods) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = new MockFilterChain();

            httpLoggingFilter.doFilterInternal(request, response, filterChain);

            assertTrue(response.getStatus() >= 0);
        }
    }

}