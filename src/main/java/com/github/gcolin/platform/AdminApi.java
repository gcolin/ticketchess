package com.github.gcolin.platform;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.payment.RibService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("admin")
@LoggedOnly
public class AdminApi {

    private static final Logger logger = LoggerFactory.getLogger(AdminApi.class);

    static final Map<String, Set<String>> TAB_KEYS = Map.of(
            "club",
            Set.of(
                    "title",
                    "contact.url",
                    "source.url",
                    "org.name",
                    "org.email",
                    "org.address",
                    "org.hosting.address",
                    "membership.notif.emails"),
            "invoice",
            Set.of(
                    "invoice.seller.name",
                    "invoice.seller.address1",
                    "invoice.seller.address2",
                    "invoice.seller.zip",
                    "invoice.seller.city",
                    "invoice.seller.country",
                    "invoice.seller.email",
                    "invoice.seller.phone",
                    "invoice.seller.website",
                    "invoice.seller.siret",
                    "invoice.seller.rna",
                    "invoice.seller.prefecture",
                    "invoice.number.prefix",
                    "invoice.footer",
                    "invoice.vat.notice"),
            "stripe",
            Set.of("stripe.public", "stripe.secret", "stripe.keyprefix", "stripe.simuled"),
            "oauth",
            Set.of(
                    "oauth.clientId",
                    "oauth.clientSecret",
                    "oauth.authorizationUrl",
                    "oauth.tokenUrl",
                    "oauth.scope",
                    "oauth.logoutUrl",
                    "oauth.accountUrl",
                    "oauth.userinfoUrl"),
            "system",
            Set.of(
                    "baseurl",
                    "jwt.key",
                    "mail.USER_NAME",
                    "mail.PASSWORD",
                    "db.host",
                    "db.name",
                    "db.user",
                    "db.pass",
                    "db.type",
                    "db.h2.loadPostgresDump",
                    "db.h2.postgresDumpFile"));

    static final Set<String> BOOLEAN_KEYS = Set.of("stripe.simuled", "db.h2.loadPostgresDump");

    @Inject
    private LoggedUser user;

    @Inject
    private Caches caches;

    @Inject
    private RibService ribService;

    @Inject
    private LogoService logoService;

    @Inject
    private BackgroundService backgroundService;

    @Inject
    private Config config;

    @Inject
    private SendMail sendMail;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response page(@QueryParam("success") @DefaultValue("") String success) {
        if (!user.canSeeAdminMenu()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Map<String, Object> model = new HashMap<>();
        model.put("success", success);
        return Response.ok(new JteHtml(model, "platform/admin.jte"))
                .build();
    }

    @GET
    @Path("clear-cache")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    public Response clearCache() {
        caches.getDebtCache().invalidateAll();
        caches.getPermissionCache().invalidateAll();
        caches.getAllEvents().invalidateAll();
        caches.getEvent().invalidateAll();
        caches.getNotifications().invalidateAll();
        caches.getEventGroups().invalidateAll();

        URI redirect = Redirects.toSameOriginRelative(uriInfo.getBaseUriBuilder()
                .path("admin")
                .queryParam("success", "cacheCleared")
                .build());
        return Response.seeOther(redirect).build();
    }

    @GET
    @Path("rib")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    public Response ribRedirect() {
        URI requestUri = uriInfo.getRequestUri();
        String query = requestUri == null ? null : requestUri.getRawQuery();
        URI redirect = Redirects.toSameOriginRelative(uriInfo.getBaseUriBuilder()
                .path("admin")
                .path("org")
                .replaceQuery(query)
                .build());
        return Response.seeOther(redirect).build();
    }

    @GET
    @Path("org")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    public JteHtml orgPage(
            @QueryParam("success") @DefaultValue("") String success,
            @QueryParam("error") @DefaultValue("") String error,
            @QueryParam("tab") @DefaultValue("club") String tab) {
        Map<String, Object> model = new HashMap<>();
        boolean ribAvailable = ribService.exists();
        model.put("success", success);
        model.put("error", error);
        model.put("tab", tab);
        model.put("ribAvailable", ribAvailable);
        model.put("logoAvailable", logoService.exists());
        model.put("backgroundAvailable", backgroundService.exists());
        Map<String, String> cfg = new LinkedHashMap<>(config.getOrgFormValues());
        for (Set<String> keys : TAB_KEYS.values()) {
            for (String key : keys) {
                cfg.putIfAbsent(key, "");
            }
        }
        model.put("cfg", cfg);
        model.put("clubRegisterEnabled", config.getPage().isClubRegisterEnabled());
        Map<String, Boolean> secrets = new LinkedHashMap<>();
        for (String key : Config.SECRET_KEYS) {
            secrets.put(key, config.isSecretConfigured(key));
        }
        model.put("secretsConfigured", secrets);
        if (ribAvailable) {
            var ribFile = ribService.getRibFile();
            try {
                model.put("ribSize", ServiceUtils.readable(Files.size(ribFile)));
            } catch (IOException e) {
                logger.warn("cannot read RIB file size", e);
            }
        }
        return new JteHtml(model, "platform/adminOrg.jte");
    }

    @POST
    @Path("org/config")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveConfig(MultivaluedMap<String, String> form) {
        String tab = formValue(form, "tab");
        if (tab == null || !TAB_KEYS.containsKey(tab)) {
            return redirectToOrg("error", "configInvalid", "club");
        }
        Map<String, String> updates = new LinkedHashMap<>();
        for (String key : TAB_KEYS.get(tab)) {
            String value = formValue(form, key);
            if (BOOLEAN_KEYS.contains(key)) {
                updates.put(key, value != null && !value.isBlank() && !"false".equalsIgnoreCase(value) ? "true" : "false");
            } else {
                updates.put(key, value == null ? "" : value.trim());
            }
        }
        try {
            config.updateProperties(updates);
            if ("system".equals(tab) && sendMail != null) {
                sendMail.reloadCredentials();
            }
            return redirectToOrg("success", "configSaved", tab);
        } catch (IOException e) {
            logger.error("cannot save organisation config", e);
            return redirectToOrg("error", "configSaveFailed", tab);
        }
    }

    @POST
    @Path("org/rib")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Consumes({"application/pdf", "application/x-pdf", "application/octet-stream", "*/*"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRib(InputStream file) {
        try {
            ribService.save(file);
            return jsonRedirectToOrg("success", "ribUploaded", "files");
        } catch (WebApplicationException e) {
            String error = "ribUploadFailed";
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("PDF")) {
                error = "invalidRib";
            } else if (message.contains("too large")) {
                error = "ribTooLarge";
            }
            return jsonRedirectToOrg("error", error, "files");
        } catch (IOException e) {
            logger.error("cannot save RIB file", e);
            return jsonRedirectToOrg("error", "ribUploadFailed", "files");
        }
    }

    @DELETE
    @Path("org/rib")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRib() {
        try {
            ribService.delete();
            return jsonRedirectToOrg("success", "ribDeleted", "files");
        } catch (IOException e) {
            logger.error("cannot delete RIB file", e);
            return jsonRedirectToOrg("error", "ribDeleteFailed", "files");
        }
    }

    @POST
    @Path("org/logo")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Consumes({"image/png", "image/jpeg", "image/webp", "application/octet-stream"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadLogo(InputStream file) {
        try {
            logoService.save(file);
            config.applyRuntime();
            return jsonRedirectToOrg("success", "logoUploaded", "files");
        } catch (WebApplicationException e) {
            String error = "logoUploadFailed";
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("PNG") || message.contains("JPEG") || message.contains("WebP")) {
                error = "invalidLogo";
            } else if (message.contains("too large")) {
                error = "logoTooLarge";
            }
            return jsonRedirectToOrg("error", error, "files");
        } catch (IOException e) {
            logger.error("cannot save logo file", e);
            return jsonRedirectToOrg("error", "logoUploadFailed", "files");
        }
    }

    @DELETE
    @Path("org/logo")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLogo() {
        try {
            logoService.delete();
            config.applyRuntime();
            return jsonRedirectToOrg("success", "logoDeleted", "files");
        } catch (IOException e) {
            logger.error("cannot delete logo file", e);
            return jsonRedirectToOrg("error", "logoDeleteFailed", "files");
        }
    }

    @POST
    @Path("org/background")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Consumes({"image/png", "image/jpeg", "image/webp", "application/octet-stream"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadBackground(InputStream file) {
        try {
            backgroundService.save(file);
            config.applyRuntime();
            return jsonRedirectToOrg("success", "backgroundUploaded", "files");
        } catch (WebApplicationException e) {
            String error = "backgroundUploadFailed";
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("PNG") || message.contains("JPEG") || message.contains("WebP")) {
                error = "invalidBackground";
            } else if (message.contains("too large")) {
                error = "backgroundTooLarge";
            }
            return jsonRedirectToOrg("error", error, "files");
        } catch (IOException e) {
            logger.error("cannot save background file", e);
            return jsonRedirectToOrg("error", "backgroundUploadFailed", "files");
        }
    }

    @DELETE
    @Path("org/background")
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBackground() {
        try {
            backgroundService.delete();
            config.applyRuntime();
            return jsonRedirectToOrg("success", "backgroundDeleted", "files");
        } catch (IOException e) {
            logger.error("cannot delete background file", e);
            return jsonRedirectToOrg("error", "backgroundDeleteFailed", "files");
        }
    }

    private Response redirectToOrg(String queryName, String queryValue, String tab) {
        return Response.seeOther(orgRedirectUri(queryName, queryValue, tab)).build();
    }

    private Response jsonRedirectToOrg(String queryName, String queryValue, String tab) {
        return Response.ok(Map.of("redirect", orgRedirectUri(queryName, queryValue, tab).toString()))
                .build();
    }

    private URI orgRedirectUri(String queryName, String queryValue, String tab) {
        return Redirects.toSameOriginRelative(uriInfo.getBaseUriBuilder()
                .path("admin")
                .path("org")
                .queryParam(queryName, queryValue)
                .queryParam("tab", tab)
                .fragment(tab)
                .build());
    }

    private static String formValue(MultivaluedMap<String, String> form, String key) {
        return form == null ? null : form.getFirst(key);
    }
}
