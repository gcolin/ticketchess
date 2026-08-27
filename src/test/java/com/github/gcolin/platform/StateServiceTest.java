package com.github.gcolin.platform;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateServiceTest {

    private StateService stateService;
    private HttpServletRequest request;
    private Config config;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        config = mock(Config.class);
        stateService = new StateService();
        stateService.setRequest(request);
        stateService.setConfig(config);
    }

    @Test
    void testGetValueWithoutQueryString() {
        when(request.getRequestURI()).thenReturn("/app/page");
        when(request.getQueryString()).thenReturn(null);

        String result = stateService.getValue();

        String expected = Base64.getUrlEncoder().encodeToString("app/page".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetValueWithQueryString() {
        when(request.getRequestURI()).thenReturn("/app/page");
        when(request.getQueryString()).thenReturn("id=123&name=test");

        String result = stateService.getValue();

        String expected =
                Base64.getUrlEncoder().encodeToString("app/page?id=123&name=test".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetValueStripsContextPath() {
        when(request.getRequestURI()).thenReturn("/ticket-chess-1.0.0/event/1");
        when(request.getContextPath()).thenReturn("/ticket-chess-1.0.0");
        when(request.getQueryString()).thenReturn(null);

        String result = stateService.getValue();

        String expected = Base64.getUrlEncoder().encodeToString("event/1".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetLogin() {
        when(request.getRequestURI()).thenReturn("/app/page");
        when(request.getQueryString()).thenReturn(null);
        when(config.getLoginUrl()).thenReturn("https://example.com/login?redirect=");

        String result = stateService.getLogin();

        String expectedValue = Base64.getUrlEncoder().encodeToString("app/page".getBytes(StandardCharsets.UTF_8));
        String expected = "https://example.com/login?redirect=" + expectedValue;
        Assertions.assertEquals(expected, result);
    }
}
