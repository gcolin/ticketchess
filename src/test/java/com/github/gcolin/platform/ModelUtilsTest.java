package com.github.gcolin.platform;

import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelUtilsTest {

    @Test
    public void testGetCategory() {
        LocalDate date = LocalDate.of(2024, 2, 1);
        int birthyear = 2017;
        Assertions.assertEquals("PpoF", ModelUtils.getCategory(date, birthyear, false));

        date = LocalDate.of(2025, 8, 31);
        Assertions.assertEquals("PpoF", ModelUtils.getCategory(date, birthyear, false));

        date = LocalDate.of(2025, 9, 1);
        Assertions.assertEquals("PouF", ModelUtils.getCategory(date, birthyear, false));

        date = LocalDate.of(2026, 9, 1);
        Assertions.assertEquals("PouF", ModelUtils.getCategory(date, birthyear, false));

        date = LocalDate.of(2027, 9, 1);
        Assertions.assertEquals("PupF", ModelUtils.getCategory(date, birthyear, false));

        date = LocalDate.of(2027, 9, 1);
        Assertions.assertEquals("PupM", ModelUtils.getCategory(date, birthyear, true));
    }
}
