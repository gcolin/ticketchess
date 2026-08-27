package com.github.gcolin.platform;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;

public class JteConfig {

    private TemplateEngine pageEngine;
    private TemplateEngine mailEngine;

    public void init() {
        pageEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        mailEngine = pageEngine;
    }

    public TemplateEngine getPageEngine() {
        return pageEngine;
    }

    public TemplateEngine getMailEngine() {
        return mailEngine;
    }
}
