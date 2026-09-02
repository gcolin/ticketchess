package com.github.gcolin.event;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.RequestContext;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
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
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPaymentsReportService {

    private static final Logger logger = LoggerFactory.getLogger(EventPaymentsReportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color HEADER_BG = new Color(220, 220, 220);

    private Properties properties;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public byte[] generateForEvent(Event event, List<PlayerSubscription> subscriptions) {
        String reportNumber = "REC-" + event.getId() + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String eventDates = formatEventDates(event);
        return generate(
                event.getName(),
                eventDates,
                reportNumber,
                subscriptions,
                false);
    }

    public byte[] generateForEventCollection(EventCollection collection, List<PlayerSubscription> subscriptions) {
        String reportNumber =
                "REC-C" + collection.getId() + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return generate(collection.getName(), null, reportNumber, subscriptions, true);
    }

    public byte[] generateForAccounting(List<Payment> payments, SeasonScope scope) {
        List<AccountingDetailRow> rows = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.PAID || payment.getAmountCents() == null) {
                continue;
            }
            LocalDateTime paymentAt =
                    payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt();
            rows.add(new AccountingDetailRow(
                    paymentAt,
                    payment.getId(),
                    resolvePaymentType(payment),
                    payment.getUserEmail(),
                    ServiceUtils.toEuros(payment.getAmountCents()),
                    "",
                    ""));
        }
        return generateForAccountingDetails(rows, scope);
    }

    public byte[] generateForAccountingDetails(List<AccountingDetailRow> rows, SeasonScope scope) {
        String reportNumber = "REC-"
                + (scope.isFiltered() ? "S" + scope.getSeasonId() + "-" : "")
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        List<PaymentRow> paymentRows = rows.stream()
                .map(row -> new PaymentRow(
                        row.paymentAt(),
                        row.id(),
                        row.paymentType(),
                        row.email(),
                        row.amountEuros(),
                        row.nature(),
                        row.label()))
                .sorted(Comparator.comparing(
                                PaymentRow::paymentAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PaymentRow::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return generateAccounting(reportNumber, paymentRows, scope);
    }

    public record AccountingDetailRow(
            LocalDateTime paymentAt,
            Long id,
            String paymentType,
            String email,
            double amountEuros,
            String nature,
            String label) {}

    private byte[] generateAccounting(String reportNumber, List<PaymentRow> rows, SeasonScope scope) {
        Map<String, Double> amountByType = new LinkedHashMap<>();
        double total = 0.0;
        for (PaymentRow row : rows) {
            total += row.amountEuros();
            amountByType.merge(row.paymentType(), row.amountEuros(), Double::sum);
        }

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
            document.add(new Paragraph(bundle.getString("statistics.payments.title"), titleFont));
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

            document.add(new Paragraph(bundle.getString("statistics.payments.summary"), sectionFont));
            document.add(new Paragraph(
                    MessageFormat.format(
                            bundle.getString("statistics.payments.totalCollected"), formatEuros(total)),
                    normalFont));
            document.add(new Paragraph(" "));
            document.add(buildSummaryTable(bundle, amountByType, total, headerFont, normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(bundle.getString("statistics.payments.detail"), sectionFont));
            document.add(new Paragraph(" "));
            document.add(buildAccountingDetailTable(bundle, rows, headerFont, normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.payments.totalCollected"), formatEuros(total)),
                    sectionFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating accounting payments report PDF", e);
            throw new RuntimeException("Failed to generate accounting payments report PDF", e);
        }
    }

    private byte[] generate(
            String title,
            String eventDates,
            String reportNumber,
            List<PlayerSubscription> subscriptions,
            boolean includeEventColumn) {
        List<DetailRow> rows = buildRows(subscriptions);
        Map<String, Double> amountByType = new LinkedHashMap<>();
        double total = 0.0;
        for (DetailRow row : rows) {
            total += row.amountEuros();
            amountByType.merge(row.paymentType(), row.amountEuros(), Double::sum);
        }

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

            document.add(new Paragraph(bundle.getString("statistics.payments.title"), titleFont));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.payments.reportNumber"), reportNumber),
                    normalFont));
            document.add(new Paragraph(
                    MessageFormat.format(
                            bundle.getString("statistics.payments.issuedAt"), LocalDate.now().format(DATE_FORMAT)),
                    normalFont));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.payments.event"), title), normalFont));
            if (eventDates != null && !eventDates.isBlank()) {
                document.add(new Paragraph(
                        MessageFormat.format(bundle.getString("statistics.payments.eventDates"), eventDates),
                        normalFont));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph(bundle.getString("statistics.payments.summary"), sectionFont));
            document.add(new Paragraph(
                    MessageFormat.format(
                            bundle.getString("statistics.payments.totalCollected"), formatEuros(total)),
                    normalFont));
            document.add(new Paragraph(" "));
            document.add(buildSummaryTable(bundle, amountByType, total, headerFont, normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(bundle.getString("statistics.payments.detail"), sectionFont));
            document.add(new Paragraph(" "));
            document.add(buildDetailTable(bundle, rows, includeEventColumn, headerFont, normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.payments.totalCollected"), formatEuros(total)),
                    sectionFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating payments report PDF for {}", title, e);
            throw new RuntimeException("Failed to generate payments report PDF", e);
        }
    }

    private List<DetailRow> buildRows(List<PlayerSubscription> subscriptions) {
        List<DetailRow> rows = new ArrayList<>();
        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() != PlayerSubscriptionStatus.PAID) {
                continue;
            }
            if (sub.getNrFfe() == null || sub.getNrFfe().isBlank()) {
                continue;
            }
            if (sub.getAmountCents() == null) {
                continue;
            }

            Payment payment = sub.getPayment();
            String paymentType = resolvePaymentType(payment);
            LocalDateTime paymentAt = null;
            String email = "";
            if (payment != null) {
                paymentAt = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt();
                email = payment.getUserEmail() != null ? payment.getUserEmail() : "";
            }

            IPlayer player = RequestContext.require().find().player(sub.getNrFfe(), null);
            String playerName = buildFullName(player);
            String eventName =
                    sub.getEvent() != null && sub.getEvent().getName() != null ? sub.getEvent().getName() : "";

            rows.add(new DetailRow(
                    paymentAt,
                    paymentType,
                    sub.getNrFfe(),
                    playerName,
                    email,
                    ServiceUtils.toEuros(sub.getAmountCents()),
                    eventName));
        }

        rows.sort(Comparator.comparing(DetailRow::paymentAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DetailRow::licence, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(DetailRow::playerName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return rows;
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

    private PdfPTable buildSummaryTable(
            ResourceBundle bundle,
            Map<String, Double> amountByType,
            double total,
            Font headerFont,
            Font normalFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[] {50f, 30f, 20f});

        addHeaderCell(table, bundle.getString("statistics.payments.paymentType"), headerFont);
        addHeaderCell(table, bundle.getString("payment.amount"), headerFont);
        addHeaderCell(table, "%", headerFont);

        for (Map.Entry<String, Double> entry : amountByType.entrySet()) {
            String typeLabel = paymentTypeLabel(bundle, entry.getKey());
            double amount = entry.getValue();
            double pct = total > 0 ? amount * 100.0 / total : 0.0;
            table.addCell(new Phrase(typeLabel, normalFont));
            PdfPCell amountCell = new PdfPCell(new Phrase(formatEuros(amount), normalFont));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
            PdfPCell pctCell = new PdfPCell(new Phrase(String.format(Locale.FRANCE, "%.1f", pct), normalFont));
            pctCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(pctCell);
        }
        return table;
    }

    private PdfPTable buildDetailTable(
            ResourceBundle bundle,
            List<DetailRow> rows,
            boolean includeEventColumn,
            Font headerFont,
            Font normalFont)
            throws DocumentException {
        int cols = includeEventColumn ? 7 : 6;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        if (includeEventColumn) {
            table.setWidths(new float[] {12f, 18f, 14f, 12f, 18f, 16f, 10f});
            addHeaderCell(table, bundle.getString("statistics.payments.date"), headerFont);
            addHeaderCell(table, bundle.getString("event.name"), headerFont);
            addHeaderCell(table, bundle.getString("statistics.payments.paymentType"), headerFont);
            addHeaderCell(table, bundle.getString("player.licence"), headerFont);
            addHeaderCell(table, bundle.getString("player.name"), headerFont);
            addHeaderCell(table, bundle.getString("payment.email"), headerFont);
            addHeaderCell(table, bundle.getString("payment.amount"), headerFont);
        } else {
            table.setWidths(new float[] {12f, 16f, 12f, 22f, 26f, 12f});
            addHeaderCell(table, bundle.getString("statistics.payments.date"), headerFont);
            addHeaderCell(table, bundle.getString("statistics.payments.paymentType"), headerFont);
            addHeaderCell(table, bundle.getString("player.licence"), headerFont);
            addHeaderCell(table, bundle.getString("player.name"), headerFont);
            addHeaderCell(table, bundle.getString("payment.email"), headerFont);
            addHeaderCell(table, bundle.getString("payment.amount"), headerFont);
        }

        for (DetailRow row : rows) {
            table.addCell(new Phrase(formatDate(row.paymentAt()), normalFont));
            if (includeEventColumn) {
                table.addCell(new Phrase(nullToEmpty(row.eventName()), normalFont));
            }
            table.addCell(new Phrase(paymentTypeLabel(bundle, row.paymentType()), normalFont));
            table.addCell(new Phrase(nullToEmpty(row.licence()), normalFont));
            table.addCell(new Phrase(nullToEmpty(row.playerName()), normalFont));
            table.addCell(new Phrase(nullToEmpty(row.email()), normalFont));
            PdfPCell amountCell = new PdfPCell(new Phrase(formatEuros(row.amountEuros()), normalFont));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
        }
        return table;
    }

    private PdfPTable buildAccountingDetailTable(
            ResourceBundle bundle, List<PaymentRow> rows, Font headerFont, Font normalFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {12f, 8f, 12f, 24f, 12f, 18f, 14f});
        addHeaderCell(table, bundle.getString("statistics.payments.date"), headerFont);
        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, bundle.getString("statistics.payments.paymentType"), headerFont);
        addHeaderCell(table, bundle.getString("payment.email"), headerFont);
        addHeaderCell(table, bundle.getString("statistics.payments.nature"), headerFont);
        addHeaderCell(table, bundle.getString("statistics.payments.label"), headerFont);
        addHeaderCell(table, bundle.getString("payment.amount"), headerFont);

        for (PaymentRow row : rows) {
            table.addCell(new Phrase(formatDate(row.paymentAt()), normalFont));
            table.addCell(new Phrase(row.id() == null ? "" : String.valueOf(row.id()), normalFont));
            table.addCell(new Phrase(paymentTypeLabel(bundle, row.paymentType()), normalFont));
            table.addCell(new Phrase(nullToEmpty(row.email()), normalFont));
            table.addCell(new Phrase(natureLabel(bundle, row.nature()), normalFont));
            table.addCell(new Phrase(nullToEmpty(row.label()), normalFont));
            PdfPCell amountCell = new PdfPCell(new Phrase(formatEuros(row.amountEuros()), normalFont));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
        }
        return table;
    }

    private static String natureLabel(ResourceBundle bundle, String nature) {
        if (nature == null || nature.isBlank()) {
            return "";
        }
        String key = "statistics.payments.nature." + nature;
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return nature;
    }

    private static void addHeaderCell(PdfPTable table, String text, Font headerFont) {
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(HEADER_BG);
        table.addCell(cell);
    }

    private static String formatEventDates(Event event) {
        if (event == null) {
            return "";
        }
        String start =
                event.getStartDate() != null ? event.getStartDate().toLocalDate().format(DATE_FORMAT) : "";
        String end = event.getEndDate() != null ? event.getEndDate().toLocalDate().format(DATE_FORMAT) : "";
        if (!start.isEmpty() && !end.isEmpty() && !start.equals(end)) {
            return start + " — " + end;
        }
        if (!start.isEmpty()) {
            return start;
        }
        return end;
    }

    /** Infer CARD for legacy Stripe payments whose type was never set. */
    public static String resolvePaymentType(Payment payment) {
        if (payment == null) {
            return "UNKNOWN";
        }
        if (payment.getType() != null) {
            return payment.getType().name();
        }
        if ((payment.getStripeSessionId() != null && !payment.getStripeSessionId().isBlank())
                || (payment.getStripeIntent() != null && !payment.getStripeIntent().isBlank())) {
            return PaymentType.CARD.name();
        }
        return "UNKNOWN";
    }

    private static String paymentTypeLabel(ResourceBundle bundle, String type) {
        String key = "payment.type." + type;
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return type;
    }

    private static String buildFullName(IPlayer player) {
        if (player == null) {
            return "";
        }
        String firstname = player.getFirstname() == null ? "" : player.getFirstname().trim();
        String name = player.getName() == null ? "" : player.getName().trim();
        return (firstname + " " + name).trim();
    }

    private static String formatEuros(double amount) {
        return String.format(Locale.FRANCE, "%.2f €", amount);
    }

    private static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.toLocalDate().format(DATE_FORMAT);
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

    private record DetailRow(
            LocalDateTime paymentAt,
            String paymentType,
            String licence,
            String playerName,
            String email,
            double amountEuros,
            String eventName) {}

    private record PaymentRow(
            LocalDateTime paymentAt,
            Long id,
            String paymentType,
            String email,
            double amountEuros,
            String nature,
            String label) {}

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

        private static String joinNotBlank(String separator, String... values) {
            StringBuilder sb = new StringBuilder();
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                if (!sb.isEmpty()) {
                    sb.append(separator);
                }
                sb.append(value.trim());
            }
            return sb.toString();
        }
    }
}
