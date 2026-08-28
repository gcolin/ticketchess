package com.github.gcolin.membership;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.ServiceUtils;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipReportService {

    private static final Logger logger = LoggerFactory.getLogger(MembershipReportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color HEADER_BG = new Color(220, 220, 220);

    private Properties properties;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public byte[] generate(
            List<Membership> memberships, Map<Integer, List<String>> optionsByMembership, SeasonScope scope) {
        List<Membership> sorted = memberships.stream()
                .sorted(Comparator.comparing(Membership::getId, Comparator.nullsLast(Integer::compareTo))
                        .reversed())
                .toList();
        int totalCents = sorted.stream().mapToInt(Membership::getAmountCents).sum();
        String reportNumber = "ADH-"
                + (scope.isFiltered() ? "S" + scope.getSeasonId() + "-" : "")
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        Document document = new Document(PageSize.A4.rotate(), 36, 36, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
            String vatNotice = properties.getProperty(
                    "invoice.vat.notice", bundle.getString("statistics.payments.vatNotice"));
            String footerContact = properties.getProperty(
                    "invoice.footer", bundle.getString("statistics.payments.footer"));
            writer.setPageEvent(new FooterEvent(vatNotice, footerContact));
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 9);
            Font smallFont = new Font(Font.HELVETICA, 8);

            addSellerHeader(document, normalFont, smallFont);
            document.add(new Paragraph(bundle.getString("membership.report.title"), titleFont));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.payments.reportNumber"), reportNumber),
                    normalFont));
            document.add(new Paragraph(
                    MessageFormat.format(
                            bundle.getString("statistics.payments.issuedAt"), LocalDate.now().format(DATE_FORMAT)),
                    normalFont));
            if (scope.isFiltered()) {
                String period = scope.getStart().toLocalDate().format(DATE_FORMAT)
                        + " — "
                        + scope.getEnd().toLocalDate().format(DATE_FORMAT);
                document.add(new Paragraph(
                        MessageFormat.format(bundle.getString("statistics.payments.period"), period), normalFont));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("membership.report.count"), sorted.size()), sectionFont));
            document.add(new Paragraph(
                    MessageFormat.format(
                            bundle.getString("membership.report.totalAmount"),
                            formatEuros(ServiceUtils.toEuros((long) totalCents))),
                    normalFont));
            document.add(new Paragraph(" "));
            document.add(buildTable(bundle, sorted, optionsByMembership, headerFont, normalFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating memberships report PDF", e);
            throw new RuntimeException("Failed to generate memberships report PDF", e);
        }
    }

    private PdfPTable buildTable(
            ResourceBundle bundle,
            List<Membership> memberships,
            Map<Integer, List<String>> optionsByMembership,
            Font headerFont,
            Font normalFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {5f, 14f, 10f, 12f, 12f, 10f, 12f, 10f, 15f});

        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, bundle.getString("admin.membership.user"), headerFont);
        addHeaderCell(table, bundle.getString("admin.membership.nrFfe"), headerFont);
        addHeaderCell(table, bundle.getString("label.lastname"), headerFont);
        addHeaderCell(table, bundle.getString("label.firstname"), headerFont);
        addHeaderCell(table, bundle.getString("player.birthdate"), headerFont);
        addHeaderCell(table, bundle.getString("label.status"), headerFont);
        addHeaderCell(table, bundle.getString("admin.membership.amount"), headerFont);
        addHeaderCell(table, bundle.getString("clubRegister.myMemberships.options"), headerFont);

        for (Membership membership : memberships) {
            table.addCell(new Phrase(membership.getId() == null ? "" : String.valueOf(membership.getId()), normalFont));
            table.addCell(new Phrase(nullToEmpty(membership.getUser()), normalFont));
            table.addCell(new Phrase(nullToEmpty(membership.getNrFfe()), normalFont));
            table.addCell(new Phrase(nullToEmpty(membership.getLastname()), normalFont));
            table.addCell(new Phrase(nullToEmpty(membership.getFirstname()), normalFont));
            table.addCell(new Phrase(nullToEmpty(membership.getBirthDate()), normalFont));
            table.addCell(new Phrase(statusLabel(bundle, membership.getStatus()), normalFont));
            PdfPCell amountCell =
                    new PdfPCell(new Phrase(formatEuros(ServiceUtils.toEuros((long) membership.getAmountCents())), normalFont));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
            List<String> options = membership.getId() == null
                    ? List.of()
                    : optionsByMembership.getOrDefault(membership.getId(), List.of());
            table.addCell(new Phrase(String.join(", ", options), normalFont));
        }
        return table;
    }

    private static String statusLabel(ResourceBundle bundle, MembershipStatus status) {
        if (status == null) {
            return "";
        }
        String key = "clubRegister.membershipStatus." + status.name();
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return status.name();
    }

    private void addSellerHeader(Document document, Font normalFont, Font smallFont) throws DocumentException {
        String sellerName = Config.configured(properties, "invoice.seller.name", "org.name");
        String sellerAddress1 = Config.configured(properties, "invoice.seller.address1", "org.address");
        String sellerAddress2 = Config.configured(properties, "invoice.seller.address2", null);
        String sellerZip = Config.configured(properties, "invoice.seller.zip", null);
        String sellerCity = Config.configured(properties, "invoice.seller.city", null);
        String sellerCountry = Config.configured(properties, "invoice.seller.country", null);
        String sellerEmail = Config.configured(properties, "invoice.seller.email", "org.email");
        String sellerPhone = Config.configured(properties, "invoice.seller.phone", null);
        String sellerWebsite = Config.configured(properties, "invoice.seller.website", "contact.url");
        String sellerSiret = Config.configured(properties, "invoice.seller.siret", null);
        String sellerRna = Config.configured(properties, "invoice.seller.rna", null);
        String sellerPrefecture = Config.configured(properties, "invoice.seller.prefecture", null);

        addLineIfNotBlank(document, normalFont, sellerName);
        addLineIfNotBlank(document, normalFont, sellerAddress1);
        addLineIfNotBlank(document, normalFont, sellerAddress2);
        addLineIfNotBlank(document, normalFont, joinNotBlank(" ", sellerZip, sellerCity));
        addLineIfNotBlank(document, normalFont, sellerCountry);
        addLineIfNotBlank(
                document,
                smallFont,
                joinNotBlank(
                        " | ",
                        isBlank(sellerEmail) ? "" : "Email: " + sellerEmail,
                        isBlank(sellerPhone) ? "" : "Tel: " + sellerPhone,
                        sellerWebsite));
        addLineIfNotBlank(
                document,
                smallFont,
                joinNotBlank(
                        " | ",
                        isBlank(sellerSiret) ? "" : "SIRET: " + sellerSiret,
                        isBlank(sellerRna) ? "" : "RNA: " + sellerRna));
        addLineIfNotBlank(document, smallFont, sellerPrefecture);
        document.add(new Paragraph(" "));
    }

    private static void addHeaderCell(PdfPTable table, String text, Font headerFont) {
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(HEADER_BG);
        table.addCell(cell);
    }

    private static String formatEuros(double amount) {
        return String.format(Locale.FRANCE, "%.2f €", amount);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void addLineIfNotBlank(Document document, Font font, String value) throws DocumentException {
        if (!isBlank(value)) {
            document.add(new Paragraph(value, font));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String joinNotBlank(String separator, String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(separator);
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private static final class FooterEvent extends PdfPageEventHelper {
        private final String vatNotice;
        private final String footerContact;
        private final Font footerFont = new Font(Font.HELVETICA, 8);

        private FooterEvent(String vatNotice, String footerContact) {
            this.vatNotice = vatNotice;
            this.footerContact = footerContact;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 20;
            String left = joinNotBlank(" — ", vatNotice, footerContact);
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase(left, footerFont),
                    document.left(),
                    y,
                    0);
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    new Phrase(String.valueOf(writer.getPageNumber()), footerFont),
                    document.right(),
                    y,
                    0);
        }
    }
}
