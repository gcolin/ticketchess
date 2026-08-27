package com.github.gcolin.platform;

public class Page {

    private String title;
    private String contactUrl;
    private boolean clubRegisterEnabled;
    private String orgName;
    private String orgEmail;
    private String orgAddress;
    private String orgHostingAddress;
    private String logoUrl;
    private String backgroundUrl;
    private String accountUrl;
    private String sourceUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContactUrl() {
        return contactUrl;
    }

    public void setContactUrl(String contactUrl) {
        this.contactUrl = contactUrl;
    }

    public boolean isClubRegisterEnabled() {
        return clubRegisterEnabled;
    }

    public void setClubRegisterEnabled(boolean clubRegisterEnabled) {
        this.clubRegisterEnabled = clubRegisterEnabled;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgEmail() {
        return orgEmail;
    }

    public void setOrgEmail(String orgEmail) {
        this.orgEmail = orgEmail;
    }

    public String getOrgAddress() {
        return orgAddress;
    }

    public void setOrgAddress(String orgAddress) {
        this.orgAddress = orgAddress;
    }

    public String getOrgHostingAddress() {
        return orgHostingAddress;
    }

    public void setOrgHostingAddress(String orgHostingAddress) {
        this.orgHostingAddress = orgHostingAddress;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    public String getAccountUrl() {
        return accountUrl;
    }

    public void setAccountUrl(String accountUrl) {
        this.accountUrl = accountUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @Override
    public String toString() {
        return "Page [title=" + title + ", contactUrl=" + contactUrl + ", clubRegisterEnabled="
                + clubRegisterEnabled + ", orgName=" + orgName + "]";
    }
}
