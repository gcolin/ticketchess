package com.github.gcolin.player;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindTest {

    private Find find;
    private CustomPlayerDao customPlayerService;
    private LuceneDb luceneDb;

    @BeforeEach
    void setUp() {
        customPlayerService = mock(CustomPlayerDao.class);
        luceneDb = mock(LuceneDb.class);
        find = new Find();
        find.setCustomPlayerDao(customPlayerService);
        find.setLuceneDb(luceneDb);
    }

    @Test
    void testPlayerWithCustomPlayerId() {
        CustomPlayer customPlayer = new CustomPlayer();
        customPlayer.setId(42);

        when(customPlayerService.find(42)).thenReturn(customPlayer);

        IPlayer result = find.player("@42", null);

        Assertions.assertSame(customPlayer, result);
        verify(customPlayerService).find(42);
        verify(customPlayerService).detach(customPlayer);
    }

    @Test
    void testPlayerWithCustomPlayerIdNotFound() {
        when(customPlayerService.find(99)).thenReturn(null);

        IPlayer result = find.player("@99", null);

        Assertions.assertNull(result);
        verify(customPlayerService).find(99);
    }

    @Test
    void testPlayerWithPlayerId() throws ParseException, IOException {
        Player player = new Player();
        when(luceneDb.searchJoueur("123")).thenReturn(player);

        IPlayer result = find.player("123", null);

        Assertions.assertSame(player, result);
        verify(luceneDb).searchJoueur("123");
    }

    @Test
    void testPlayerWithPlayerIdNotFound() throws ParseException, IOException {
        when(luceneDb.searchJoueur("123")).thenThrow(IOException.class);

        Assertions.assertNull(find.player("123", null));
        verify(luceneDb).searchJoueur("123");
    }
}
