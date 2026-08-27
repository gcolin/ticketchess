package com.github.gcolin.auth;

import com.github.gcolin.platform.AbstractMail;
public class LoginMail extends AbstractMail {

    private String name;
    private String loginUrl;

    public LoginMail() {
        setTemplate("auth/loginLink");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
}
