package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PersistenceServiceTest {

    @Test
    void parseEnumNamesFromCheckConstraintShouldReadInClauseValues() {
        Set<String> values = PersistenceService.parseEnumNamesFromCheckConstraint(
                "CHECK (option_type IN ('FFE_ID', 'CHESS_EVENT_USER', 'CHESS_EVENT_PASSWORD'))");

        assertEquals(Set.of("CHESS_EVENT_PASSWORD", "CHESS_EVENT_USER", "FFE_ID"), values);
    }

    @Test
    void parseEnumNamesFromCheckConstraintShouldReadPostgresArrayValues() {
        String constraintDef =
                "CHECK (((option_type)::text = ANY ((ARRAY['FFE_ID'::character varying, 'CHESS_EVENT_USER'::character varying])::text[])))";

        Set<String> values = PersistenceService.parseEnumNamesFromCheckConstraint(constraintDef);

        assertEquals(Set.of("CHESS_EVENT_USER", "FFE_ID"), values);
    }

    @Test
    void parseEnumNamesFromCheckConstraintShouldReturnEmptySetWhenMissing() {
        assertTrue(PersistenceService.parseEnumNamesFromCheckConstraint(null).isEmpty());
        assertTrue(PersistenceService.parseEnumNamesFromCheckConstraint("").isEmpty());
    }
}
