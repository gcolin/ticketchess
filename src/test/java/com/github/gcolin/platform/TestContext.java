package com.github.gcolin.platform;

import com.github.gcolin.player.Find;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;

public final class TestContext implements AutoCloseable {

    private boolean clearAppContext;

    private TestContext() {}

    public static TestContext open(EntityManager em) {
        RequestContext.openForTest(em);
        return new TestContext();
    }

    public static TestContext open(Find find) {
        RequestContext.openForTest(find);
        return new TestContext();
    }

    public static TestContext open(com.github.gcolin.auth.LoggedUser loggedUser) {
        RequestContext.openForTest(loggedUser);
        return new TestContext();
    }

    public static TestContext open(
            com.github.gcolin.auth.LoggedUser loggedUser,
            com.github.gcolin.auth.ActiveLoggedUsers activeLoggedUsers) {
        AppContext.initForTest(activeLoggedUsers);
        TestContext ctx = open(loggedUser);
        ctx.clearAppContext = true;
        return ctx;
    }

    public static TestContext open(EntityManager em, Find find) {
        RequestContext.openForTest(em, find);
        return new TestContext();
    }

    public static TestContext open(HttpServletRequest request, EntityManager em) {
        RequestContext.openForTest(request, em);
        return new TestContext();
    }

    public RequestContext ctx() {
        return RequestContext.require();
    }

    public static <T extends AbstractDao<?>> T createDao(Class<T> type, EntityManager em) {
        try {
            T dao = type.getDeclaredConstructor().newInstance();
            dao.setEm(em);
            return dao;
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot create dao " + type.getName(), e);
        }
    }

    @Override
    public void close() {
        RequestContext.close();
        if (clearAppContext) {
            AppContext.clearTestInstance();
        }
    }
}
