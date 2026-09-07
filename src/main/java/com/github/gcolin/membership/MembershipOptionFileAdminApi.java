package com.github.gcolin.membership;

import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.platform.BroadcastMail;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.MailTemplate;
import com.github.gcolin.platform.SendMail;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequireRole(value = RoleCode.TRESORIER, or = RoleCode.ENTRAINEUR)
@Path("membership-option-files")
public class MembershipOptionFileAdminApi {

    private static final Logger logger = LoggerFactory.getLogger(MembershipOptionFileAdminApi.class);
    private static final String SHARED_FILES_ANCHOR_PREFIX = "shared-file-";

    @Inject
    private MembershipOptionDao membershipOptionDao;

    @Inject
    private MembershipOptionFileDao membershipOptionFileDao;

    @Inject
    private MembershipOptionFileService membershipOptionFileService;

    @Inject
    private MembershipOptionSubscriptionDao membershipOptionSubscriptionDao;

    @Inject
    private LoggedUser loggedUser;

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    @Inject
    private Config config;

    @Inject
    private SendMail sendMail;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml page(
            @QueryParam("optionId") Integer optionId,
            @QueryParam("seasonId") Integer seasonId,
            @QueryParam("error") String error,
            @QueryParam("sent") Integer sent) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<MembershipOption> options = membershipOptionDao.all(scope);
        MembershipOption selected = resolveSelectedOption(options, optionId);

        Map<String, Object> model = new HashMap<>();
        model.put("options", options);
        model.put("selectedOption", selected);
        model.put(
                "files",
                selected != null && selected.getId() != null
                        ? membershipOptionFileDao.findByOptionId(selected.getId())
                        : Collections.emptyList());
        model.put("error", error);
        model.put("sent", sent);
        model.put("seasonId", clubSeasonFilter.effectiveSeasonId(seasonId));
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "membership/membershipOptionFiles.jte");
    }

    @POST
    @Path("upload")
    @Consumes({
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet",
        "text/plain",
        "text/csv",
        "image/png",
        "image/jpeg",
        "image/webp",
        "image/gif",
        "application/octet-stream",
        "*/*"
    })
    public Response upload(
            @QueryParam("optionId") Integer optionId,
            @QueryParam("filename") String filename,
            @QueryParam("seasonId") Integer seasonId,
            InputStream file) {
        MembershipOption option = requireOption(optionId);
        try {
            MembershipOptionFileService.StoredFile stored =
                    membershipOptionFileService.save(option.getId(), filename, null, file);
            MembershipOptionFile entity = new MembershipOptionFile();
            entity.setMembershipOption(option);
            entity.setOriginalName(stored.originalName());
            entity.setStoredName(stored.storedName());
            entity.setContentType(stored.contentType());
            entity.setSizeBytes(stored.sizeBytes());
            entity.setUploadedBy(loggedUser != null ? loggedUser.getEmail() : null);
            membershipOptionFileDao.persist(entity);
            return Response.seeOther(pageUri(option.getId(), seasonId, null)).build();
        } catch (WebApplicationException e) {
            String error = "uploadFailed";
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("not allowed") || message.contains("invalid") || message.contains("required")) {
                error = "invalidFile";
            } else if (message.contains("too large")) {
                error = "fileTooLarge";
            } else if (message.contains("empty")) {
                error = "emptyFile";
            }
            return Response.seeOther(pageUri(optionId, seasonId, error)).build();
        } catch (IOException e) {
            logger.error("cannot save membership option file", e);
            return Response.seeOther(pageUri(optionId, seasonId, "uploadFailed")).build();
        }
    }

    @POST
    @Path("{fileId}/delete")
    public Response delete(
            @PathParam("fileId") Integer fileId,
            @FormParam("optionId") Integer optionId,
            @FormParam("seasonId") Integer seasonId) {
        MembershipOptionFile file = membershipOptionFileDao.findWithOption(fileId);
        if (file == null || file.getMembershipOption() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Integer resolvedOptionId = file.getMembershipOption().getId();
        if (optionId != null && !optionId.equals(resolvedOptionId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            membershipOptionFileService.delete(file);
        } catch (IOException e) {
            logger.warn("cannot delete membership option file from disk: {}", e.getMessage());
        }
        membershipOptionFileDao.remove(file);
        return Response.seeOther(pageUri(resolvedOptionId, seasonId, null)).build();
    }

    @GET
    @Path("{fileId}/download")
    public Response download(@PathParam("fileId") Integer fileId) {
        MembershipOptionFile file = membershipOptionFileDao.findWithOption(fileId);
        if (file == null) {
            throw new jakarta.ws.rs.NotFoundException("File not found");
        }
        try {
            var path = membershipOptionFileService.resolveStoredFile(file);
            if (!Files.isRegularFile(path)) {
                throw new jakarta.ws.rs.NotFoundException("Stored file missing");
            }
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
            StreamingOutput stream = output -> Files.copy(path, output);
            return Response.ok(stream)
                    .type(contentType)
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName)
                    .build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("cannot download membership option file {}", fileId, e);
            throw new WebApplicationException("Cannot download file", 500);
        }
    }

    @GET
    @Path("{fileId}/mail")
    public JteHtml mailPage(
            @PathParam("fileId") Integer fileId,
            @QueryParam("seasonId") Integer seasonId,
            @QueryParam("sent") Integer sent) {
        MembershipOptionFile file = requireFile(fileId);
        MembershipOption option = file.getMembershipOption();
        List<MailRecipient> recipients = buildRecipients(option.getId());

        Map<String, Object> model = new HashMap<>();
        model.put("file", file);
        model.put("option", option);
        model.put("recipients", recipients);
        model.put("defaultBody", defaultMarkdownBody(file, option));
        model.put("defaultSubject", defaultSubject(file, option));
        model.put("seasonId", clubSeasonFilter.effectiveSeasonId(seasonId));
        if (sent != null) {
            model.put("sent", sent);
        }
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "membership/membershipOptionFileMail.jte");
    }

    @POST
    @Path("{fileId}/mail/preview")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response mailPreview(@PathParam("fileId") Integer fileId, @FormParam("body") String body) {
        MembershipOptionFile file = requireFile(fileId);
        MembershipOption option = file.getMembershipOption();
        try {
            BroadcastMail mail = buildBroadcastMail(file, option, body, "NOM Prénom", null);
            config.applyOrg(mail);
            String html = new MailTemplate().render(mail.getTemplate(), mail);
            return Response.ok(html, MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            logger.error("Failed to render membership option file mail preview for {}: {}", fileId, e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("{fileId}/mail")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendMail(
            @PathParam("fileId") Integer fileId,
            @FormParam("subject") String subject,
            @FormParam("body") String body,
            @FormParam("seasonId") Integer seasonId) {
        MembershipOptionFile file = requireFile(fileId);
        MembershipOption option = file.getMembershipOption();
        String resolvedSubject = subject == null || subject.isBlank() ? defaultSubject(file, option) : subject.trim();
        int sent = 0;
        for (MailRecipient recipient : buildRecipients(option.getId())) {
            try {
                BroadcastMail mail =
                        buildBroadcastMail(file, option, body, recipient.displayName(), recipient.email());
                sendMail.send(mail, recipient.email(), resolvedSubject);
                sent++;
            } catch (Exception e) {
                logger.error(
                        "Failed to send membership option file mail {} to {}: {}",
                        fileId,
                        recipient.email(),
                        e.getMessage());
            }
        }
        UriBuilder builder = uriInfo.getBaseUriBuilder()
                .path("membership-option-files")
                .path(fileId.toString())
                .path("mail")
                .queryParam("sent", sent);
        if (seasonId != null) {
            builder.queryParam("seasonId", seasonId);
        }
        return Response.seeOther(builder.build()).build();
    }

    private BroadcastMail buildBroadcastMail(
            MembershipOptionFile file,
            MembershipOption option,
            String markdownBody,
            String playerName,
            String emailAddress) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdownBody == null ? "" : markdownBody);
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

        BroadcastMail mail = new BroadcastMail();
        mail.setEventName(option.getOptionValue() != null ? option.getOptionValue() : "Fichier partagé");
        mail.setName(playerName);
        mail.setBody(renderer.render(document));
        mail.setHeaderBackgroundColor("#0f766e");
        mail.setHeaderTextColor("#ffffff");
        mail.setHeaderIcon("📁");
        mail.setButtonText("Accéder aux fichiers");
        if (emailAddress != null && !emailAddress.isBlank()) {
            mail.setLoginUrl(loginUrlFor(emailAddress, file.getId(), playerName));
        } else {
            mail.setLoginUrl(publicFilesUrl(file.getId()));
        }
        return mail;
    }

    private String loginUrlFor(String email, Integer fileId, String playerName) {
        String issuer = playerName != null && !playerName.isBlank() ? playerName : email;
        String jwt = Jwts.builder()
                .subject(email.trim())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();
        String redirect = "/club-register#" + SHARED_FILES_ANCHOR_PREFIX + fileId;
        return baseUrl()
                + "/login?jwt="
                + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&redirect_uri="
                + URLEncoder.encode(redirect, StandardCharsets.UTF_8);
    }

    private String publicFilesUrl(Integer fileId) {
        return baseUrl() + "/club-register#" + SHARED_FILES_ANCHOR_PREFIX + fileId;
    }

    private String baseUrl() {
        String base = config.getProperties().getProperty("baseurl", "http://localhost:8080");
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String defaultSubject(MembershipOptionFile file, MembershipOption option) {
        String optionLabel = option.getOptionValue() != null ? option.getOptionValue() : "option";
        String fileName = file.getOriginalName() != null ? file.getOriginalName() : "fichier";
        return "Nouveau fichier — " + optionLabel + " — " + fileName;
    }

    private String defaultMarkdownBody(MembershipOptionFile file, MembershipOption option) {
        String optionLabel = option.getOptionValue() != null ? option.getOptionValue() : "votre option";
        String fileName = file.getOriginalName() != null ? file.getOriginalName() : "fichier";
        String link = publicFilesUrl(file.getId());
        return "Bonjour,\n\n"
                + "Un nouveau fichier est disponible pour **"
                + optionLabel
                + "** : `"
                + fileName
                + "`.\n\n"
                + "Cordialement";
    }

    private List<MailRecipient> buildRecipients(Integer optionId) {
        Map<String, MailRecipient> byEmail = new LinkedHashMap<>();
        for (MembershipOptionSubscription sub : membershipOptionSubscriptionDao.findByOptionId(optionId)) {
            Membership membership = sub.getMembership();
            if (membership == null || membership.getUser() == null || membership.getUser().isBlank()) {
                continue;
            }
            if (!isPaidOrApproved(membership.getStatus())) {
                continue;
            }
            String email = membership.getUser().trim().toLowerCase();
            String firstname = membership.getFirstname() != null ? membership.getFirstname().trim() : "";
            String lastname = membership.getLastname() != null ? membership.getLastname().trim() : "";
            String displayName = (firstname + " " + lastname).trim();
            byEmail.putIfAbsent(
                    email, new MailRecipient(membership.getUser().trim(), displayName, firstname, lastname));
        }
        return new ArrayList<>(byEmail.values());
    }

    private static boolean isPaidOrApproved(MembershipStatus status) {
        return status == MembershipStatus.APPROVED || status == MembershipStatus.PAID;
    }

    private MembershipOptionFile requireFile(Integer fileId) {
        MembershipOptionFile file = membershipOptionFileDao.findWithOption(fileId);
        if (file == null || file.getMembershipOption() == null || file.getMembershipOption().getId() == null) {
            throw new jakarta.ws.rs.NotFoundException("File not found");
        }
        return file;
    }

    private MembershipOption resolveSelectedOption(List<MembershipOption> options, Integer optionId) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        if (optionId != null) {
            for (MembershipOption option : options) {
                if (optionId.equals(option.getId())) {
                    return option;
                }
            }
        }
        return options.get(0);
    }

    private MembershipOption requireOption(Integer optionId) {
        if (optionId == null) {
            throw new WebApplicationException("Option is required", 400);
        }
        MembershipOption option = membershipOptionDao.find(optionId);
        if (option == null) {
            throw new jakarta.ws.rs.NotFoundException("Option not found");
        }
        return option;
    }

    private URI pageUri(Integer optionId, Integer seasonId, String error) {
        UriBuilder builder = uriInfo.getBaseUriBuilder().path("membership-option-files");
        if (optionId != null) {
            builder.queryParam("optionId", optionId);
        }
        if (seasonId != null) {
            builder.queryParam("seasonId", seasonId);
        }
        if (error != null && !error.isBlank()) {
            builder.queryParam("error", error);
        }
        return builder.build();
    }

    public record MailRecipient(String email, String displayName, String firstname, String lastname) {}
}
