package com.github.gcolin.platform;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.platform.Config;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.AbstractMail;
import com.github.gcolin.registration.CancelMail;
import com.github.gcolin.auth.LoginMail;
import com.github.gcolin.platform.MailTemplate;
import com.github.gcolin.payment.PaymentMail;
import com.github.gcolin.registration.RegistrationMail;
import com.github.gcolin.platform.SendMail;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("admin/mail")
@RequirePermission(PermissionCode.MAIL_SEND)
public class AdminMailApi {

    @Inject
    private SendMail sendMail;

    @Inject
    private Config config;

    /**
     * Show a simple form to send a templated email.
     */
    @GET
    public JteHtml showForm() {
        Map<String, Object> model = new HashMap<>();
        // List of available templates
        model.put("templates", new String[] {
            "registrationConfirmation", "paymentConfirmation", "cancelConfirmation", "loginLink",
        });
        return new JteHtml(model, "platform/adminMail.jte");
    }

    /**
     * Send an email using the chosen template.
     */
    @POST
    @Path("preview")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response previewMailForm(
            @FormParam("to") String to,
            @FormParam("template") String template,
            @FormParam("name") String name,
            @FormParam("eventName") String eventName,
            @FormParam("evenDate") String evenDate,
            @FormParam("amount") String amountStr,
            @FormParam("reference") String reference,
            @FormParam("baseUrl") String baseUrl,
            @FormParam("redirectUri") String redirectUri) {
        try {
            AbstractMail mail = createMail(to, template, name, eventName, evenDate, amountStr, reference, baseUrl, redirectUri);
            if (mail == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Unknown template")
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }

            config.applyOrg(mail);
            String html = new MailTemplate().render(mail.getTemplate(), mail);
            return Response.ok(html, MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error generating preview: " + e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendMailForm(
            @FormParam("to") String to,
            @FormParam("subject") String subject,
            @FormParam("template") String template,
            @FormParam("name") String name,
            @FormParam("eventName") String eventName,
            @FormParam("evenDate") String evenDate,
            @FormParam("amount") String amountStr,
            @FormParam("reference") String reference,
            @FormParam("baseUrl") String baseUrl,
            @FormParam("redirectUri") String redirectUri) {
        if (to == null || to.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Recipient email is required")
                    .build();
        }
        try {
            AbstractMail mail = createMail(to, template, name, eventName, evenDate, amountStr, reference, baseUrl, redirectUri);
            if (mail == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Unknown template")
                        .build();
            }
            sendMail.send(mail, to, subject);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error sending email: " + e.getMessage())
                    .build();
        }
        return Response.ok("Email sent").build();
    }

    private AbstractMail createMail(
            String to,
            String template,
            String name,
            String eventName,
            String evenDate,
            String amountStr,
            String reference,
            String baseUrl,
            String redirectUri) {
        String safeName = safeText(name);
        String safeEventName = safeText(eventName);
        String safeEvenDate = safeText(evenDate);
        String safeReference = safeText(reference);

        switch (template) {
            case "registrationConfirmation": {
                String selectedBaseUrl =
                        baseUrl == null || baseUrl.isEmpty() ? config.getProperties().getProperty("baseurl") : baseUrl;
                
                String jwt = Jwts.builder()
                        .subject(to.trim())
                        .issuer(safeName.isBlank() ? to.trim() : safeName)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                        .signWith(config.getKeys())
                        .compact();
                String loginUrl = selectedBaseUrl + "/login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode("/event/my", StandardCharsets.UTF_8);
                
                RegistrationMail rm = new RegistrationMail();
                rm.setName(safeName);
                rm.setEventName(safeEventName);
                rm.setEvenDate(safeEvenDate);
                rm.setBaseUrl(selectedBaseUrl);
                rm.setLoginUrl(loginUrl);
                if (amountStr != null && !amountStr.isEmpty()) {
                    rm.setAmount(amountStr);
                }
                return rm;
            }
            case "paymentConfirmation": {
                String selectedBaseUrl =
                        baseUrl == null || baseUrl.isEmpty() ? config.getProperties().getProperty("baseurl") : baseUrl;
                
                String jwt = Jwts.builder()
                        .subject(to.trim())
                        .issuer(safeName.isBlank() ? to.trim() : safeName)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                        .signWith(config.getKeys())
                        .compact();
                String loginUrl = selectedBaseUrl + "/login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode("/event/my", StandardCharsets.UTF_8);
                
                PaymentMail pm = new PaymentMail();
                pm.setName(safeName);
                pm.setEventName(safeEventName);
                pm.setEvenDate(safeEvenDate);
                pm.setLoginUrl(loginUrl);
                if (amountStr != null && !amountStr.isEmpty()) {
                    pm.setAmount(amountStr);
                }
                pm.setReference(safeReference);
                return pm;
            }
            case "cancelConfirmation": {
                String selectedBaseUrl =
                        baseUrl == null || baseUrl.isEmpty() ? config.getProperties().getProperty("baseurl") : baseUrl;
                
                String jwt = Jwts.builder()
                        .subject(to.trim())
                        .issuer(safeName.isBlank() ? to.trim() : safeName)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                        .signWith(config.getKeys())
                        .compact();
                String loginUrl = selectedBaseUrl + "/login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode("/event/my", StandardCharsets.UTF_8);
                
                CancelMail cm = new CancelMail();
                cm.setName(safeName);
                cm.setEventName(safeEventName);
                cm.setEvenDate(safeEvenDate);
                cm.setLoginUrl(loginUrl);
                if (amountStr != null && !amountStr.isEmpty()) {
                    cm.setAmount(amountStr);
                }
                cm.setReference(safeReference);
                return cm;
            }
            case "loginLink": {
                if (to == null || to.isBlank()) {
                    return null;
                }
                String selectedBaseUrl =
                        baseUrl == null || baseUrl.isEmpty() ? config.getProperties().getProperty("baseurl") : baseUrl;
                String jwt = Jwts.builder()
                        .subject(to.trim())
                        .issuer(safeName.isBlank() ? to.trim() : safeName)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                        .signWith(config.getKeys())
                        .compact();

                String loginUrl = selectedBaseUrl + "/login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
                if (redirectUri != null && !redirectUri.isBlank()) {
                    loginUrl += "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                }

                LoginMail lm = new LoginMail();
                lm.setName(safeName.isBlank() ? to.trim() : safeName);
                lm.setLoginUrl(loginUrl);
                return lm;
            }
            default:
                return null;
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
