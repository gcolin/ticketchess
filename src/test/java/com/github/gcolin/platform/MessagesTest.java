package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MessagesTest {

    @Test
    void formatDateShouldUseFrenchLocale() {
        Messages msg = new Messages(Locale.FRENCH);
        assertEquals("15 janv. 2026", msg.formatDate(LocalDate.of(2026, 1, 15)));
    }

    @Test
    void formatDateShouldUseEnglishLocale() {
        Messages msg = new Messages(Locale.UK);
        assertEquals("15 Jan 2026", msg.formatDate(LocalDate.of(2026, 1, 15)));
    }

    @Test
    void formatDateShouldReturnEmptyStringForNull() {
        Messages msg = new Messages(Locale.FRENCH);
        assertEquals("", msg.formatDate((LocalDate) null));
    }
}
