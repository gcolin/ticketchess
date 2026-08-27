package com.github.gcolin.platform;



import gg.jte.ContentType;

import gg.jte.TemplateEngine;

import gg.jte.output.StringOutput;

import java.io.IOException;



public class MailTemplate {



    private final TemplateEngine engine;



    public MailTemplate() {

        engine = TemplateEngine.createPrecompiled(ContentType.Html);

    }



    public String render(String name, Object data) throws IOException {

        String templateName = name.endsWith(".jte") ? name : name + ".jte";

        templateName = "tmpl/" + templateName;

        StringOutput output = new StringOutput();

        engine.render(templateName, data, output);

        return output.toString();

    }

}


