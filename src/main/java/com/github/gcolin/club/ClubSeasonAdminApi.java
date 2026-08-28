package com.github.gcolin.club;

import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.platform.JteHtml;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequirePermission(PermissionCode.ADMIN_PANEL)
@Path("admin/seasons")
public class ClubSeasonAdminApi {

    @Inject
    private ClubSeasonDao clubSeasonDao;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml list() {
        Map<String, Object> model = new HashMap<>();
        model.put("seasons", clubSeasonDao.allOrderByStartDateDesc());
        return new JteHtml(model, "club/seasonAdmin.jte");
    }

    @GET
    @Path("new")
    public JteHtml createPage() {
        Map<String, Object> model = new HashMap<>();
        model.put("season", new ClubSeason());
        return new JteHtml(model, "club/seasonEdit.jte");
    }

    @GET
    @Path("{id}/edit")
    public JteHtml editPage(@PathParam("id") Integer id) {
        ClubSeason season = clubSeasonDao.find(id);
        if (season == null) {
            return new JteHtml(Map.of(
                    "statusCode", 404,
                    "statusMessage", "Not Found",
                    "errors", List.of()), "platform/error.jte");
        }
        Map<String, Object> model = new HashMap<>();
        model.put("season", season);
        return new JteHtml(model, "club/seasonEdit.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(
            @FormParam("name") String name,
            @FormParam("startDate") String startDate,
            @FormParam("endDate") String endDate,
            @FormParam("current") String current) {
        ClubSeason season = new ClubSeason();
        season.setName(name);
        season.setStartDate(LocalDate.parse(startDate));
        season.setEndDate(LocalDate.parse(endDate));
        clubSeasonDao.save(season, isChecked(current));
        return redirectToList();
    }

    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(
            @PathParam("id") Integer id,
            @FormParam("name") String name,
            @FormParam("startDate") String startDate,
            @FormParam("endDate") String endDate,
            @FormParam("current") String current) {
        ClubSeason season = clubSeasonDao.find(id);
        if (season == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        season.setName(name);
        season.setStartDate(LocalDate.parse(startDate));
        season.setEndDate(LocalDate.parse(endDate));
        clubSeasonDao.save(season, isChecked(current));
        return redirectToList();
    }

    @POST
    @Path("{id}/delete")
    public Response delete(@PathParam("id") Integer id) {
        clubSeasonDao.remove(id);
        return redirectToList();
    }

    private Response redirectToList() {
        URI redirect = uriInfo.getBaseUriBuilder().path("admin/seasons").build();
        return Response.seeOther(redirect).build();
    }

    private static boolean isChecked(String value) {
        return "true".equals(value) || "on".equals(value);
    }
}
