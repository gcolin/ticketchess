package com.github.gcolin.platform;

import com.github.gcolin.event.Event;
import com.github.gcolin.player.IPlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ServiceUtilsTest {

    @Test
    public void testCalculatePriceFreeTitleReturnsZero() {
        IPlayer player = Mockito.mock(IPlayer.class);
        Event event = Mockito.mock(Event.class);

        Mockito.when(player.getFideTitre()).thenReturn("g");
        Mockito.when(player.isYoung()).thenReturn(false);
        Mockito.when(event.getPriceCents()).thenReturn(4200L);

        Assertions.assertEquals(0L, ServiceUtils.calculatePrice(player, event));
    }

    @Test
    public void testCalculatePriceYoungUsesYoungPrice() {
        IPlayer player = Mockito.mock(IPlayer.class);
        Event event = Mockito.mock(Event.class);

        Mockito.when(player.getFideTitre()).thenReturn(null);
        Mockito.when(player.isYoung()).thenReturn(true);
        Mockito.when(event.getYoungPriceCents()).thenReturn(1250L);

        Assertions.assertEquals(1250L, ServiceUtils.calculatePrice(player, event));
    }

    @Test
    public void testCalculatePriceNullPricesReturnZero() {
        IPlayer player = Mockito.mock(IPlayer.class);
        Event event = Mockito.mock(Event.class);

        Mockito.when(player.getFideTitre()).thenReturn(null);
        Mockito.when(player.isYoung()).thenReturn(false);
        Mockito.when(event.getPriceCents()).thenReturn(0L);

        Assertions.assertEquals(0L, ServiceUtils.calculatePrice(player, event));
    }

    @Test
    public void testToEuros() {
        Assertions.assertEquals(12.5, ServiceUtils.toEuros(1250L));
    }

    @Test
    public void testParseInt() {
        Assertions.assertNull(ServiceUtils.parseInt(null));
        Assertions.assertNull(ServiceUtils.parseInt(""));
        Assertions.assertEquals(123, ServiceUtils.parseInt("123"));
    }
}
