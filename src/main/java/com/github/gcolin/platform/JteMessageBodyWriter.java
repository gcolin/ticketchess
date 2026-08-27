package com.github.gcolin.platform;

import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.notification.Notifications;
import com.github.gcolin.payment.RibService;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
public class JteMessageBodyWriter implements MessageBodyWriter<JteHtml> {

    private static final Logger logger = LoggerFactory.getLogger(JteMessageBodyWriter.class);

    @Context
    HttpServletRequest request;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == JteHtml.class;
    }

    @Override
    public void writeTo(
            JteHtml t,
            Class<?> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
            throws IOException, WebApplicationException {
        RequestContext ctx = RequestContext.require();
        LoggedUser user = ctx.loggedUser();
        StateService state = ctx.stateService();
        Notifications notifications = ctx.notifications();
        Config config = AppContext.get().config();
        RibService ribService = AppContext.get().ribService();
        JteConfig jteConfig = AppContext.get().jteConfig();

        Map<String, Object> model = new HashMap<>();
        model.putAll(t.getModel());
        model.put("msg", new Messages(request.getLocale()));
        model.put("contextPath", request.getContextPath());
        model.put("user", user);
        model.put("state", state);
        model.put("notifications", notifications);
        model.put("pageConfig", config.getPage());
        model.put("ribAvailable", ribService != null && ribService.exists());

        String templateName = t.getTemplate().endsWith(".jte") ? t.getTemplate() : t.getTemplate() + ".jte";
        TemplateEngine engine = jteConfig.getPageEngine();
        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        WriterOutput output = new WriterOutput(writer);
        try {
            engine.render(templateName, model, output);
            writer.flush();
        } catch (RuntimeException e) {
            if (ClientDisconnect.isGone(e)) {
                return;
            }
            logger.error("Failed to render template {}", templateName, e);
            throw new WebApplicationException(e);
        } catch (IOException e) {
            if (ClientDisconnect.isGone(e)) {
                return;
            }
            throw e;
        }
    }
}
