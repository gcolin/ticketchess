package com.github.gcolin.auth;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.github.gcolin.platform.JteHtml;

@Path("logas")
@RequireRole(RoleCode.ADMIN)
public class LogAsApi {

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private PaymentDao paymentDao;

    @Inject
    private Config config;

    @GET
    @Transactional
    public JteHtml page() {
        Set<String> emails = new TreeSet<>();
        emails.addAll(playerSubscriptionDao.findDistinctCreationUsers());
        emails.addAll(paymentDao.findDistinctUserEmails());

        Map<String, Object> model = new HashMap<>();
        model.put("users", new ArrayList<>(emails));
        model.put("admins", config.getAdmins());
        return new JteHtml(model, "auth/logas.jte");
    }
}
