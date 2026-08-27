package com.github.gcolin.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ActiveSessionTest {

    @Test
    void testGettersAndSetters() {
        ActiveSession session = new ActiveSession("s1", "alice@example.com", "Alice", false, 123L);

        Assertions.assertEquals("s1", session.getSessionId());
        Assertions.assertEquals("alice@example.com", session.getEmail());
        Assertions.assertEquals("Alice", session.getUsername());
        Assertions.assertFalse(session.isAdmin());
        Assertions.assertEquals(123L, session.getLastSeenMillis());

        session.setEmail("bob@example.com");
        session.setUsername("Bob");
        session.setAdmin(true);
        session.setLastSeenMillis(456L);

        Assertions.assertEquals("bob@example.com", session.getEmail());
        Assertions.assertEquals("Bob", session.getUsername());
        Assertions.assertTrue(session.isAdmin());
        Assertions.assertEquals(456L, session.getLastSeenMillis());
    }
}
