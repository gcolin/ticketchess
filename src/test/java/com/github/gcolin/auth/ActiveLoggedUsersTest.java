package com.github.gcolin.auth;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ActiveLoggedUsersTest {

    @Test
    void testTouchCreatesAndUpdatesSession() {
        ActiveLoggedUsers users = new ActiveLoggedUsers();

        users.touch("s1", "alice@example.com", "Alice", false);
        List<ActiveSession> first = users.listActive();

        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals("alice@example.com", first.get(0).getEmail());
        Assertions.assertEquals("Alice", first.get(0).getUsername());
        Assertions.assertFalse(first.get(0).isAdmin());

        users.touch("s1", "alice@example.com", "Alice Admin", true);
        List<ActiveSession> second = users.listActive();

        Assertions.assertEquals(1, second.size());
        Assertions.assertEquals("Alice Admin", second.get(0).getUsername());
        Assertions.assertTrue(second.get(0).isAdmin());
    }

    @Test
    void testRemoveSession() {
        ActiveLoggedUsers users = new ActiveLoggedUsers();

        users.touch("s1", "alice@example.com", "Alice", false);
        users.remove("s1");

        Assertions.assertTrue(users.listActive().isEmpty());
    }

    @Test
    void testListActiveSortsByEmailDescendingThenLastSeenDescending() {
        ActiveLoggedUsers users = new ActiveLoggedUsers();

        users.touch("s1", "alice@example.com", "Alice", false);
        users.touch("s2", "bob@example.com", "Bob", false);
        users.touch("s3", "bob@example.com", "Bob2", false);

        List<ActiveSession> list = users.listActive();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("bob@example.com", list.get(0).getEmail());
        Assertions.assertEquals("bob@example.com", list.get(1).getEmail());
        Assertions.assertEquals("alice@example.com", list.get(2).getEmail());
        Assertions.assertTrue(list.get(0).getLastSeenMillis() >= list.get(1).getLastSeenMillis());
    }

    @Test
    void testListActivePurgesIdleSessions() {
        ActiveLoggedUsers users = new ActiveLoggedUsers();

        users.touch("s1", "alice@example.com", "Alice", false);
        users.touch("s2", "bob@example.com", "Bob", false);

        List<ActiveSession> beforePurge = users.listActive();
        ActiveSession toExpire = beforePurge.stream()
                .filter(s -> "s1".equals(s.getSessionId()))
                .findFirst()
                .orElseThrow();
        toExpire.setLastSeenMillis(System.currentTimeMillis() - 3_000_000L);

        List<ActiveSession> afterPurge = users.listActive();
        Assertions.assertEquals(1, afterPurge.size());
        Assertions.assertEquals("s2", afterPurge.get(0).getSessionId());
    }

    @Test
    void testNullInputsAreIgnored() {
        ActiveLoggedUsers users = new ActiveLoggedUsers();

        users.touch(null, "alice@example.com", "Alice", false);
        users.touch("s1", null, "Alice", false);
        users.remove(null);

        Assertions.assertTrue(users.listActive().isEmpty());
    }
}
