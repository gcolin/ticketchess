package com.github.gcolin.membership;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionAccessRule;
import com.github.gcolin.membership.MembershipOptionType;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.membership.LicenseDao;
import com.github.gcolin.membership.MembershipOptionDao;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@RequirePermission(PermissionCode.ADMIN_PANEL)
@Path("membership-option-admin")
public class MembershipOptionAdminApi {

    @Inject
    private MembershipOptionDao membershipOptionDao;

    @Inject
    private LicenseDao licenseDao;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml list() {
        Map<String, Object> model = new HashMap<>();
        model.put("options", membershipOptionDao.all());
        return new JteHtml(model, "membership/membershipOption.jte");
    }

    @GET
    @Path("new")
    public JteHtml createPage() {
        Map<String, Object> model = new HashMap<>();
        model.put("option", new MembershipOption());
        model.put("optionTypes", MembershipOptionType.values());
        model.put("accessRules", MembershipOptionAccessRule.values());
        model.put("licenses", licenseDao.all());
        return new JteHtml(model, "membership/membershipOptionEdit.jte");
    }

    @GET
    @Path("{id}/edit")
    public JteHtml editPage(@PathParam("id") Integer id) {
        MembershipOption option = membershipOptionDao.find(id);
        if (option == null) {
            return new JteHtml(Map.of(
                    "statusCode", 404,
                    "statusMessage", "Not Found",
                    "errors", List.of()), "platform/error.jte");
        }
        Map<String, Object> model = new HashMap<>();
        model.put("option", option);
        model.put("optionTypes", MembershipOptionType.values());
        model.put("accessRules", MembershipOptionAccessRule.values());
        model.put("licenses", licenseDao.all());
        return new JteHtml(model, "membership/membershipOptionEdit.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(
            @FormParam("optionType") String optionType,
            @FormParam("optionValue") String optionValue,
            @FormParam("amountCents") Integer amountCents,
            @FormParam("accessRule") String accessRule,
            @FormParam("licenseId") Integer licenseId) {
        MembershipOption option = new MembershipOption();
        option.setOptionType(MembershipOptionType.valueOf(optionType));
        option.setOptionValue(optionValue);
        option.setAmountCents(amountCents);
        option.setAccessRule(parseAccessRule(accessRule));
        if (licenseId != null && licenseId > 0) {
            option.setLicense(licenseDao.find(licenseId));
        }
        membershipOptionDao.persist(option);
        URI redirect = uriInfo.getBaseUriBuilder().path("membership-option-admin").build();
        return Response.seeOther(redirect).build();
    }

    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(
            @PathParam("id") Integer id,
            @FormParam("optionType") String optionType,
            @FormParam("optionValue") String optionValue,
            @FormParam("amountCents") Integer amountCents,
            @FormParam("accessRule") String accessRule,
            @FormParam("licenseId") Integer licenseId) {
        MembershipOption option = membershipOptionDao.find(id);
        if (option == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        option.setOptionType(MembershipOptionType.valueOf(optionType));
        option.setOptionValue(optionValue);
        option.setAmountCents(amountCents);
        option.setAccessRule(parseAccessRule(accessRule));
        if (licenseId != null && licenseId > 0) {
            option.setLicense(licenseDao.find(licenseId));
        } else {
            option.setLicense(null);
        }
        membershipOptionDao.merge(option);
        URI redirect = uriInfo.getBaseUriBuilder().path("membership-option-admin").build();
        return Response.seeOther(redirect).build();
    }

    @POST
    @Path("{id}/delete")
    public Response delete(@PathParam("id") Integer id) {
        membershipOptionDao.remove(id);
        URI redirect = uriInfo.getBaseUriBuilder().path("membership-option-admin").build();
        return Response.seeOther(redirect).build();
    }

    private MembershipOptionAccessRule parseAccessRule(String accessRule) {
        if (accessRule == null || accessRule.isBlank()) {
            return MembershipOptionAccessRule.ALL;
        }
        return MembershipOptionAccessRule.valueOf(accessRule);
    }
}
