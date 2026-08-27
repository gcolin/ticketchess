package com.github.gcolin.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HttpSessionDestroyedListenerTest {

    @Test
    void testSessionDestroyedDoesNotThrowWhenEventIsNull() {
        HttpSessionDestroyedListener listener = new HttpSessionDestroyedListener();

        Assertions.assertDoesNotThrow(() -> listener.sessionDestroyed(null));
    }

    @Test
    void testSessionDestroyedDoesNotThrowWhenCdiIsUnavailable() {
        HttpSessionDestroyedListener listener = new HttpSessionDestroyedListener();
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn("s1");
        HttpSessionEvent event = new HttpSessionEvent(session);

        Assertions.assertDoesNotThrow(() -> listener.sessionDestroyed(event));
    }
}
