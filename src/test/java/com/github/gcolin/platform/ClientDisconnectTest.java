package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ClientDisconnectTest {

    @Test
    void detectsEofAndBrokenPipe() {
        assertTrue(ClientDisconnect.isGone(new EOFException()));
        assertTrue(ClientDisconnect.isGone(new IOException("Broken pipe")));
        assertTrue(ClientDisconnect.isGone(new IOException("Connection reset")));
        assertTrue(ClientDisconnect.isGone(new RuntimeException(new IOException("Broken pipe"))));
        assertFalse(ClientDisconnect.isGone(new IOException("disk full")));
        assertFalse(ClientDisconnect.isGone(null));
    }
}
