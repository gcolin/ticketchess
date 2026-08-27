package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.github.gcolin.auth.LoginMail;
import com.github.gcolin.payment.PaymentMail;
import com.github.gcolin.registration.CancelMail;
import com.github.gcolin.registration.RegistrationMail;

class MailTemplateTest {

    private final MailTemplate template = new MailTemplate();

    @Test
    void shouldRenderRegistrationConfirmationTemplate() throws Exception {
        RegistrationMail mail = new RegistrationMail();
        mail.setName("Alice Doe");
        mail.setEventName("Open Test");
        mail.setEvenDate("2026-06-01");
        mail.setAmount("25 EUR");
        mail.setLoginUrl("https://example.org/event/my");
        mail.setOrgName("Test Club");

        String html = template.render(mail.getTemplate(), mail);

        assertFalse(html.contains("${"));
        assertTrue(html.contains("Alice Doe"));
        assertTrue(html.contains("Open Test"));
        assertTrue(html.contains("25 EUR"));
        assertTrue(html.contains("https://example.org/event/my"));
    }

    @Test
    void shouldRenderPaymentConfirmationTemplate() throws Exception {
        PaymentMail mail = new PaymentMail();
        mail.setName("Bob Doe");
        mail.setEventName("Rapid Test");
        mail.setEvenDate("2026-06-02");
        mail.setAmount("18 EUR");
        mail.setReference("PAY-123");
        mail.setOrgName("Test Club");

        String html = template.render(mail.getTemplate(), mail);

        assertFalse(html.contains("${"));
        assertTrue(html.contains("Bob Doe"));
        assertTrue(html.contains("Rapid Test"));
        assertTrue(html.contains("18 EUR"));
        assertTrue(html.contains("PAY-123"));
    }

    @Test
    void shouldRenderRegistrationConfirmationTemplateWithoutAmount() throws Exception {
        RegistrationMail mail = new RegistrationMail();
        mail.setName("Alice Doe");
        mail.setEventName("Open Test");
        mail.setEvenDate("2026-06-01");
        mail.setLoginUrl("https://example.org/login?jwt=abc&redirect_uri=%2Fevent%2Fmy");
        mail.setOrgName("Test Club");

        String html = template.render(mail.getTemplate(), mail);

        assertFalse(html.contains("${"));
        assertTrue(html.contains("Voir mon événement"));
        assertTrue(html.contains("https://example.org/login?jwt=abc&redirect_uri=%2Fevent%2Fmy"));
    }

    @Test
    void shouldRenderCancelConfirmationTemplate() throws Exception {
        CancelMail mail = new CancelMail();
        mail.setName("Chris Doe");
        mail.setEventName("Blitz Test");
        mail.setAmount("10 EUR");
        mail.setReference("CANCEL-123");
        mail.setEvenDate("2026-06-03");
        mail.setOrgName("Test Club");

        String html = template.render(mail.getTemplate(), mail);

        assertFalse(html.contains("${"));
        assertTrue(html.contains("Chris Doe"));
        assertTrue(html.contains("Blitz Test"));
        assertTrue(html.contains("10 EUR"));
        assertTrue(html.contains("CANCEL-123"));
    }

    @Test
    void shouldRenderLoginLinkTemplate() throws Exception {
        LoginMail mail = new LoginMail();
        mail.setName("Eve Doe");
        mail.setLoginUrl("https://example.org/login?jwt=abc");
        mail.setOrgName("Test Club");

        String html = template.render(mail.getTemplate(), mail);

        assertFalse(html.contains("${"));
        assertTrue(html.contains("Eve Doe"));
        assertTrue(html.contains("https://example.org/login?jwt=abc"));
    }

    @Test
    void shouldRenderBroadcastTemplateWithAndWithoutName() throws Exception {
        BroadcastMail withName = new BroadcastMail();
        withName.setEventName("Cup Test");
        withName.setName("Dana Doe");
        withName.setBody("<p>Hello</p>");
        withName.setOrgName("Test Club");

        String withNameHtml = template.render(withName.getTemplate(), withName);
        assertTrue(withNameHtml.contains("Cup Test - Dana Doe"));

        BroadcastMail withoutName = new BroadcastMail();
        withoutName.setEventName("Cup Test");
        withoutName.setName("");
        withoutName.setBody("<p>Hello</p>");
        withoutName.setOrgName("Test Club");

        String withoutNameHtml = template.render(withoutName.getTemplate(), withoutName);
        assertFalse(withoutNameHtml.contains("Cup Test -"));
        assertTrue(withoutNameHtml.contains("Cup Test</h1>"));
    }
}
