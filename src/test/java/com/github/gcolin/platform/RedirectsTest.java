package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.Test;

class RedirectsTest {

    @Test
    void shouldAcceptRelativePaths() {
        assertTrue(Redirects.isSafeRelativeRedirect("/event/my"));
        assertTrue(Redirects.isSafeRelativeRedirect("/club-register?success=mail"));
    }

    @Test
    void shouldRejectAbsoluteAndProtocolRelativeUrls() {
        assertFalse(Redirects.isSafeRelativeRedirect("https://evil.example/phish"));
        assertFalse(Redirects.isSafeRelativeRedirect("//evil.example/phish"));
        assertFalse(Redirects.isSafeRelativeRedirect("javascript:alert(1)"));
    }

    @Test
    void shouldBuildLocalRedirectFromRelativePath() {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder builder = UriBuilder.fromUri("http://localhost:8080/app/");
        when(uriInfo.getBaseUriBuilder()).thenReturn(builder);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/app/"));

        URI redirect = Redirects.safeRedirect(
                "/event/my?success=payment", URI.create("http://localhost:8080/app/"), uriInfo);

        assertEquals("http://localhost:8080/app/event/my?success=payment", redirect.toString());
    }

    @Test
    void shouldStripHostFromAbsoluteRedirect() {
        URI relative = Redirects.toSameOriginRelative(
                URI.create("http://tournoistest.example/admin/org?success=ribUploaded&tab=files#files"));

        assertEquals("/admin/org?success=ribUploaded&tab=files#files", relative.toString());
    }

    @Test
    void shouldFallbackToDefaultForUnsafeRedirect() {
        UriInfo uriInfo = mock(UriInfo.class);
        URI defaultUri = URI.create("http://localhost:8080/app/");
        when(uriInfo.getBaseUri()).thenReturn(defaultUri);

        URI redirect = Redirects.safeRedirect("https://evil.example", defaultUri, uriInfo);

        assertEquals(defaultUri, redirect);
    }
}
