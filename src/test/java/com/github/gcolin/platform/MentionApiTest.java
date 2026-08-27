package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MentionApiTest {

    @Test
    void getShouldReturnMentionTemplate() {
        MentionApi api = new MentionApi();

        JteHtml html = api.get();

        assertEquals("platform/mentions.jte", html.getTemplate());
        assertTrue(html.getModel().isEmpty());
    }
}
