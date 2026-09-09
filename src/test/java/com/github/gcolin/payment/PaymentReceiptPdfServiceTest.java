package com.github.gcolin.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class PaymentReceiptPdfServiceTest {

    @Test
    void amountToFrenchWordsShouldFormatEurosAndCents() {
        assertEquals("cinquante euros", PaymentReceiptPdfService.amountToFrenchWords(50));
        assertEquals("un euro", PaymentReceiptPdfService.amountToFrenchWords(1));
        assertEquals("vingt-cinq euros et cinquante centimes", PaymentReceiptPdfService.amountToFrenchWords(25.50));
        assertEquals("cent euros", PaymentReceiptPdfService.amountToFrenchWords(100));
        assertEquals("soixante et onze euros", PaymentReceiptPdfService.amountToFrenchWords(71));
    }

    @Test
    void donationAttestationShouldContainFiscalMentions() throws Exception {
        PaymentReceiptPdfService service = new PaymentReceiptPdfService();
        Properties properties = new Properties();
        properties.setProperty("invoice.seller.name", "Club Echecs Test");
        properties.setProperty("invoice.seller.address1", "1 place du Club");
        properties.setProperty("invoice.seller.zip", "87000");
        properties.setProperty("invoice.seller.city", "Limoges");
        properties.setProperty("invoice.seller.siret", "12345678900012");
        properties.setProperty("invoice.seller.rna", "W123456789");
        properties.setProperty("invoice.donation.object", "Promotion du jeu d'echecs");
        properties.setProperty("invoice.donation.signatory", "Jean President");
        properties.setProperty("invoice.donation.signatoryTitle", "President");
        properties.setProperty("invoice.donation.prefix", "DON-");
        service.setProperties(properties);

        Payment payment = new Payment();
        payment.setId(42L);
        payment.setDonation(true);
        payment.setStatus(PaymentStatus.PAID);
        payment.setType(PaymentType.CHEQUE);
        payment.setUserEmail("donor@example.com");
        payment.setPayerName("Marie Curie");
        payment.setPayerAddress("12 rue de la Paix\n75002 Paris");
        payment.setAmount(50.0);
        payment.setUpdatedAt(LocalDateTime.of(2026, 3, 15, 10, 0));

        byte[] pdf = service.generateDonationAttestation(payment);
        assertTrue(new String(pdf, 0, Math.min(8, pdf.length)).startsWith("%PDF"));
        String text = extractPdfText(pdf);

        assertTrue(text.contains("Attestation de don"), text);
        assertTrue(text.contains("Marie Curie"), text);
        assertTrue(text.contains("12 rue de la Paix"), text);
        assertTrue(text.contains("50,00"), text);
        assertTrue(text.contains("cinquante euros"), text);
        assertTrue(text.contains("DON-42"), text);
        assertTrue(text.contains("Cheque") || text.contains("Chèque"), text);
        assertTrue(text.contains("200"), text);
        assertFalse(text.contains("Reglement de creance") || text.contains("Règlement de créance"), text);
    }

    @Test
    void paymentReceiptShouldKeepClassicWording() throws Exception {
        PaymentReceiptPdfService service = new PaymentReceiptPdfService();
        Properties properties = new Properties();
        properties.setProperty("invoice.seller.name", "Club Echecs Test");
        properties.setProperty("invoice.number.prefix", "FAC-");
        service.setProperties(properties);

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(PaymentStatus.PAID);
        payment.setType(PaymentType.CARD);
        payment.setPayerName("Ada Lovelace");
        payment.setAmount(25.0);
        payment.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));

        byte[] pdf = service.generatePaymentReceipt(payment, List.of(), List.of(), Map.of(), null, null);
        assertTrue(new String(pdf, 0, Math.min(8, pdf.length)).startsWith("%PDF"));
        String text = extractPdfText(pdf);

        assertTrue(text.contains("Recu de paiement") || text.contains("Reçu de paiement"), text);
        assertTrue(text.contains("Ada Lovelace"), text);
        assertTrue(text.contains("Reglement de creance") || text.contains("Règlement de créance"), text);
        assertFalse(text.contains("Attestation de don"), text);
    }

    private static String extractPdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                sb.append(extractor.getTextFromPage(i));
                sb.append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}
