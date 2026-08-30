package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;
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

    @Test
    void roleMessageKeysShouldExistInBothLocales() {
        Messages english = new Messages(Locale.UK);
        Messages french = new Messages(Locale.FRENCH);
        for (String role : List.of("ADMIN", "TRESORIER", "ARBITRE", "EVENT_ADMIN")) {
            String key = "role." + role;
            assertFalse(english.get(key).isBlank());
            assertFalse(french.get(key).isBlank());
            assertFalse(english.get(key).equals(key));
            assertFalse(french.get(key).equals(key));
        }
    }

    @Test
    void getShouldEscapeApostropheInMessageFormat() {
        Messages msg = new Messages(Locale.FRENCH);
        assertEquals("S'inscrire au club 2025-2026", msg.get("clubRegister.title", "2025-2026"));
    }
}
