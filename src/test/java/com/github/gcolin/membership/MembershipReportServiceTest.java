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

        byte[] pdf = service.generate(List.of(membership), Map.of(1, List.of("Licence B")), SeasonScope.all());

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }
}
