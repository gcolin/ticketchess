package com.github.gcolin.membership;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.gcolin.club.SeasonScope;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MembershipReportServiceTest {

    @Test
    void generateShouldProducePdfBytes() {
        MembershipReportService service = new MembershipReportService();
        service.setProperties(new Properties());

        Membership membership = new Membership();
        membership.setId(1);
        membership.setUser("user@test.com");
        membership.setNrFfe("A12345");
        membership.setLastname("Dupont");
        membership.setFirstname("Jean");
        membership.setBirthDate("2000-01-01");
        membership.setStatus(MembershipStatus.APPROVED);
        membership.setAmountCents(4000);

        byte[] pdf = service.generate(
                List.of(membership),
                Map.of(1, List.of("Licence B")),
                Map.of("Licence B", new MembershipSummaryLine(1, 1, 4000, 4000)),
                SeasonScope.all());

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }

    @Test
    void generateShouldIncludeOptionSummaryCounts() {
        MembershipReportService service = new MembershipReportService();
        service.setProperties(new Properties());

        Membership withTwoOptions = new Membership();
        withTwoOptions.setId(1);
        withTwoOptions.setLastname("Dupont");
        withTwoOptions.setFirstname("Jean");
        withTwoOptions.setStatus(MembershipStatus.APPROVED);
        withTwoOptions.setAmountCents(4000);

        Membership withOneOption = new Membership();
        withOneOption.setId(2);
        withOneOption.setLastname("Martin");
        withOneOption.setFirstname("Alice");
        withOneOption.setStatus(MembershipStatus.APPROVED);
        withOneOption.setAmountCents(2000);

        Membership withoutOption = new Membership();
        withoutOption.setId(3);
        withoutOption.setLastname("Durand");
        withoutOption.setFirstname("Paul");
        withoutOption.setStatus(MembershipStatus.APPROVED);
        withoutOption.setAmountCents(1000);

        byte[] pdf = service.generate(
                List.of(withTwoOptions, withOneOption, withoutOption),
                Map.of(
                        1, List.of("Licence B", "Cours du mercredi"),
                        2, List.of("Licence B")),
                Map.of(
                        "Licence B", new MembershipSummaryLine(2, 2, 6000, 6000),
                        "Cours du mercredi", new MembershipSummaryLine(1, 1, 1500, 1500)),
                SeasonScope.all());

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void generateShouldIncludeOptionSectionsForEachOption() {
        MembershipReportService service = new MembershipReportService();
        service.setProperties(new Properties());

        Membership withOption = new Membership();
        withOption.setId(1);
        withOption.setLastname("Dupont");
        withOption.setFirstname("Jean");
        withOption.setNrFfe("A12345");
        withOption.setStatus(MembershipStatus.APPROVED);
        withOption.setAmountCents(4000);

        Membership withoutOption = new Membership();
        withoutOption.setId(2);
        withoutOption.setLastname("Martin");
        withoutOption.setFirstname("Alice");
        withoutOption.setStatus(MembershipStatus.PENDING_APPROVAL);
        withoutOption.setAmountCents(2000);

        byte[] pdf = service.generate(
                List.of(withOption, withoutOption),
                Map.of(1, List.of("Licence B", "Cours du mercredi")),
                Map.of(),
                SeasonScope.all());

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }
}
