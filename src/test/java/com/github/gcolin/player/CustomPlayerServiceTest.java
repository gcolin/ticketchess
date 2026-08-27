package com.github.gcolin.player;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.TestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomPlayerServiceTest {

    private EntityManager em;
    private CustomPlayerDao customPlayerService;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        testContext = TestContext.open(em);
        customPlayerService = TestContext.createDao(CustomPlayerDao.class, em);
    }

    @AfterEach
    void tearDown() {
        testContext.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByCreationUser() {
        TypedQuery<CustomPlayer> query = mock(TypedQuery.class);

        when(em.createQuery("SELECT e FROM CustomPlayer e where e.creationUser = :creationUser", CustomPlayer.class))
                .thenReturn(query);
        when(query.setParameter("creationUser", "bob@example.com")).thenReturn(query);

        CustomPlayer customPlayer = new CustomPlayer();
        when(query.getResultList()).thenReturn(List.of(customPlayer));

        List<CustomPlayer> result = customPlayerService.findByCreationUser("bob@example.com");

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(customPlayer, result.get(0));
        verify(query).setParameter("creationUser", "bob@example.com");
    }
}
