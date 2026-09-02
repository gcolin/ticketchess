package com.github.gcolin.player;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FideHandlerTest {

    @Test
    void formatFideRatingUses1399ForUnrated() {
        Assertions.assertEquals("1399F", FideHandler.formatFideRating("0"));
        Assertions.assertEquals("1399F", FideHandler.formatFideRating(""));
        Assertions.assertEquals("1399F", FideHandler.formatFideRating(null));
        Assertions.assertEquals("1399F", FideHandler.formatFideRating("   "));
    }

    @Test
    void formatFideRatingKeepsRatedValue() {
        Assertions.assertEquals("1950F", FideHandler.formatFideRating("1950"));
        Assertions.assertEquals("2100F", FideHandler.formatFideRating(" 2100 "));
    }
}
