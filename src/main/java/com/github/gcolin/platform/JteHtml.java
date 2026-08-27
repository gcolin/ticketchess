package com.github.gcolin.platform;

import java.util.Map;

public class JteHtml {

    private final Map<String, Object> model;
    private final String template;

    public JteHtml(Map<String, Object> model, String template) {
        this.model = model;
        this.template = template;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public String getTemplate() {
        return template;
    }
}
