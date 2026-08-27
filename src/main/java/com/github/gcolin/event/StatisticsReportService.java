package com.github.gcolin.event;

import com.github.gcolin.platform.RequestContext;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.event.Event;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsReportService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsReportService.class);

    public StatisticsReport computeForEvent(Event event, List<PlayerSubscription> subscriptions) {
        return compute(subscriptions, event, false);
    }

    public StatisticsReport computeForEventCollection(List<PlayerSubscription> subscriptions) {
        return compute(subscriptions, null, true);
    }

    public byte[] generatePdf(String title, StatisticsReport report) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 9);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph(bundle.getString("event.statistics"), sectionFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    MessageFormat.format(bundle.getString("statistics.event.total"), report.getTotal()), normalFont));
            document.add(new Paragraph(LocalDate.now().format(dateFormatter), normalFont));
            document.add(new Paragraph(" "));

            addCountTable(
                    document,
                    bundle.getString("statistics.event.byClub"),
                    bundle.getString("player.club"),
                    report.getByClub(),
                    report.getTotal(),
                    sectionFont,
                    headerFont,
                    normalFont);
            addCountTable(
                    document,
                    bundle.getString("statistics.event.byCategory"),
                    bundle.getString("player.category"),
                    report.getByCategory(),
                    report.getTotal(),
                    sectionFont,
                    headerFont,
                    normalFont);
            addCountTable(
                    document,
                    bundle.getString("statistics.event.byFederation"),
                    bundle.getString("player.federation"),
                    report.getByFederation(),
                    report.getTotal(),
                    sectionFont,
                    headerFont,
                    normalFont);
            addAgePyramidTable(document, bundle, report, sectionFont, headerFont, normalFont);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating statistics PDF for {}", title, e);
            throw new RuntimeException("Failed to generate statistics PDF", e);
        }
    }

    public String generateCsv(List<PlayerSubscription> subscriptions, boolean collectionMode) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        StringBuilder csv = new StringBuilder();
        csv.append(escapeCsv(bundle.getString("player.licence"))).append(";");
        csv.append(escapeCsv(bundle.getString("statistics.event.clubOrFederation"))).append(";");
        csv.append(escapeCsv(bundle.getString("player.category"))).append(";");
        csv.append(escapeCsv(bundle.getString("player.age"))).append("\n");

        List<PlayerCsvRow> rows = new ArrayList<>();
        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() != PlayerSubscriptionStatus.PAID) {
                continue;
            }
            if (sub.getNrFfe() == null || sub.getNrFfe().isEmpty()) {
                continue;
            }
            IPlayer player = RequestContext.require().find().player(sub.getNrFfe(), null);
            if (player == null) {
                continue;
            }
            String category = collectionMode ? normalizeCategory(player.getCategory()) : player.getCategory();
            if (category == null || category.isEmpty()) {
                category = "";
            }
            Integer age = extractAge(player.getBirthDate());
            String ageValue = age != null ? String.valueOf(age) : "";
            rows.add(new PlayerCsvRow(sub.getNrFfe(), resolveClubOrFederation(player), category, ageValue));
        }

        rows.sort(Comparator.comparing(PlayerCsvRow::licence));
        for (PlayerCsvRow row : rows) {
            csv.append(escapeCsv(row.licence())).append(";");
            csv.append(escapeCsv(row.clubOrFederation())).append(";");
            csv.append(escapeCsv(row.category())).append(";");
            csv.append(escapeCsv(row.age())).append("\n");
        }
        return csv.toString();
    }

    private String resolveClubOrFederation(IPlayer player) {
        String club = player.getClub();
        if (club == null || club.isEmpty()) {
            club = player.getClubRef();
        }
        if (club != null && !club.isEmpty()) {
            return club;
        }
        String federation = player.getFederation();
        return federation != null ? federation : "";
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private StatisticsReport compute(
            List<PlayerSubscription> subscriptions, Event event, boolean collectionMode) {
        Map<String, Integer> byClub = new TreeMap<>();
        Map<String, Integer> byCategory = new TreeMap<>();
        Map<String, Integer> byFederation = new TreeMap<>();
        Map<String, Double> amountByPaymentType = new TreeMap<>();
        Map<Integer, AgeBin> agePyramid = new TreeMap<>();
        List<Map<String, Object>> unknownPaymentPlayers = new ArrayList<>();

        int total = 0;
        double totalAmount = 0.0;
        double totalUnpaidAmount = 0.0;

        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() == PlayerSubscriptionStatus.CANCELLED) {
                continue;
            }
            if (sub.getNrFfe() == null || sub.getNrFfe().isEmpty()) {
                continue;
            }
            total++;

            boolean unknownPayment = false;
            if (sub.getStatus() == PlayerSubscriptionStatus.PAID && sub.getAmountCents() != null) {
                double amount = sub.getAmountCents() / 100d;
                totalAmount += amount;
                String paymentType = resolvePaymentType(sub.getPayment());
                amountByPaymentType.merge(paymentType, amount, Double::sum);
                unknownPayment = "UNKNOWN".equals(paymentType);
            }

            IPlayer player = RequestContext.require().find().player(sub.getNrFfe(), null);
            if (unknownPayment) {
                unknownPaymentPlayers.add(buildUnknownPaymentPlayer(sub, player, collectionMode ? null : event));
            }
            if (player == null) {
                continue;
            }

            String club = player.getClub();
            if (club == null || club.isEmpty()) {
                club = player.getClubRef();
            }
            if (club == null || club.isEmpty()) {
                club = "—";
            }
            byClub.merge(club, 1, Integer::sum);

            String category = collectionMode ? normalizeCategory(player.getCategory()) : player.getCategory();
            if (category == null || category.isEmpty()) {
                category = "—";
            }
            byCategory.merge(category, 1, Integer::sum);

            Integer age = extractAge(player.getBirthDate());
            if (age != null && age >= 0) {
                int binStart = (age / 2) * 2;
                AgeBin bin = agePyramid.computeIfAbsent(binStart, AgeBin::new);
                String rawCategory = player.getCategory();
                if (rawCategory != null && rawCategory.endsWith("F")) {
                    bin.female++;
                } else if (rawCategory != null && rawCategory.endsWith("M")) {
                    bin.male++;
                }
            }

            String federation = player.getFederation();
            if (federation == null || federation.isEmpty()) {
                federation = "—";
            }
            byFederation.merge(federation, 1, Integer::sum);
        }

        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() != PlayerSubscriptionStatus.NOT_PAID) {
                continue;
            }
            if (sub.getNrFfe() == null || sub.getNrFfe().isEmpty()) {
                continue;
            }
            IPlayer player = RequestContext.require().find().player(sub.getNrFfe(), null);
            if (player == null) {
                continue;
            }
            Event priceEvent = collectionMode ? sub.getEvent() : event;
            totalUnpaidAmount += ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, priceEvent));
        }

        AgePyramidData agePyramidData = buildAgePyramidRows(agePyramid);
        List<Map<String, Object>> multiEventPlayers =
                collectionMode ? buildMultiEventPlayers(subscriptions) : Collections.emptyList();
        unknownPaymentPlayers.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("name")))
                .thenComparing(row -> String.valueOf(row.get("firstname")))
                .thenComparing(row -> String.valueOf(row.get("licence"))));

        return new StatisticsReport(
                total,
                totalAmount,
                totalUnpaidAmount,
                sortByAmountDesc(amountByPaymentType),
                sortByValueDesc(byClub),
                sortByValueDesc(byCategory),
                sortByValueDesc(byFederation),
                agePyramidData.rows(),
                agePyramidData.max(),
                multiEventPlayers,
                unknownPaymentPlayers);
    }

    private Map<String, Object> buildUnknownPaymentPlayer(
            PlayerSubscription sub, IPlayer player, Event fallbackEvent) {
        Map<String, Object> row = new HashMap<>();
        row.put("licence", sub.getNrFfe());
        row.put("amount", sub.getAmountCents() / 100d);
        if (player != null) {
            row.put("name", player.getName() != null ? player.getName() : "");
            row.put("firstname", player.getFirstname() != null ? player.getFirstname() : "");
        } else {
            row.put("name", "");
            row.put("firstname", "");
        }
        Event subEvent = sub.getEvent() != null ? sub.getEvent() : fallbackEvent;
        if (subEvent != null && subEvent.getName() != null) {
            row.put("event", subEvent.getName());
        } else {
            row.put("event", "");
        }
        return row;
    }

    /** Infer CARD for legacy Stripe payments whose type was never set. */
    private static String resolvePaymentType(Payment payment) {
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

    private List<Map<String, Object>> buildMultiEventPlayers(List<PlayerSubscription> subscriptions) {
        Map<String, MultiEventAccumulator> byLicence = new HashMap<>();

        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() == PlayerSubscriptionStatus.CANCELLED) {
                continue;
            }
            if (sub.getNrFfe() == null || sub.getNrFfe().isEmpty()) {
                continue;
            }
            if (sub.getEvent() == null || sub.getEvent().getName() == null || sub.getEvent().getName().isEmpty()) {
                continue;
            }

            MultiEventAccumulator acc = byLicence.computeIfAbsent(sub.getNrFfe(), MultiEventAccumulator::new);
            acc.eventNames.add(sub.getEvent().getName());
            if (acc.player == null) {
                acc.player = RequestContext.require().find().player(sub.getNrFfe(), null);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (MultiEventAccumulator acc : byLicence.values()) {
            if (acc.eventNames.size() < 2) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("licence", acc.licence);
            if (acc.player != null) {
                row.put("name", acc.player.getName() != null ? acc.player.getName() : "");
                row.put("firstname", acc.player.getFirstname() != null ? acc.player.getFirstname() : "");
            } else {
                row.put("name", "");
                row.put("firstname", "");
            }
            row.put("events", String.join(", ", acc.eventNames));
            rows.add(row);
        }

        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("name")))
                .thenComparing(row -> String.valueOf(row.get("firstname")))
                .thenComparing(row -> String.valueOf(row.get("licence"))));
        return rows;
    }

    private AgePyramidData buildAgePyramidRows(Map<Integer, AgeBin> agePyramid) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int maxAgeBinCount = 0;
        for (Map.Entry<Integer, AgeBin> entry : agePyramid.entrySet()) {
            AgeBin bin = entry.getValue();
            int totalBin = bin.male + bin.female;
            if (totalBin <= 0) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            int binStart = entry.getKey();
            row.put("label", binStart + "-" + (binStart + 1));
            row.put("male", bin.male);
            row.put("female", bin.female);
            row.put("total", totalBin);
            rows.add(0, row);
            maxAgeBinCount = Math.max(maxAgeBinCount, Math.max(bin.male, bin.female));
        }
        return new AgePyramidData(rows, maxAgeBinCount);
    }

    private void addCountTable(
            Document document,
            String sectionTitle,
            String firstColumnLabel,
            Map<String, Integer> data,
            int total,
            Font sectionFont,
            Font headerFont,
            Font normalFont)
            throws Exception {
        document.add(new Paragraph(sectionTitle, sectionFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {50f, 25f, 25f});
        table.addCell(headerCell(firstColumnLabel, headerFont));
        table.addCell(headerCell(ResourceBundle.getBundle("messages", Locale.getDefault())
                .getString("statistics.event.count"), headerFont));
        table.addCell(headerCell("%", headerFont));

        if (data.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase(
                    ResourceBundle.getBundle("messages", Locale.getDefault()).getString("info.noData"),
                    normalFont));
            emptyCell.setColspan(3);
            table.addCell(emptyCell);
        } else {
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int count = entry.getValue();
                table.addCell(new PdfPCell(new Phrase(entry.getKey(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(count), normalFont)));
                String pct = total > 0 ? String.format(Locale.US, "%.1f", count * 100.0 / total) : "0.0";
                table.addCell(new PdfPCell(new Phrase(pct + "%", normalFont)));
            }
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addAgePyramidTable(
            Document document,
            ResourceBundle bundle,
            StatisticsReport report,
            Font sectionFont,
            Font headerFont,
            Font normalFont)
            throws Exception {
        document.add(new Paragraph(bundle.getString("statistics.event.agePyramid"), sectionFont));
        document.add(new Paragraph(bundle.getString("statistics.event.ageScale2"), normalFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {33f, 34f, 33f});
        table.addCell(headerCell(bundle.getString("statistics.event.male"), headerFont));
        table.addCell(headerCell(bundle.getString("statistics.event.ageRange"), headerFont));
        table.addCell(headerCell(bundle.getString("statistics.event.female"), headerFont));

        if (report.getAgePyramid().isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase(bundle.getString("info.noData"), normalFont));
            emptyCell.setColspan(3);
            table.addCell(emptyCell);
        } else {
            for (Map<String, Object> row : report.getAgePyramid()) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(row.get("male")), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(row.get("label")), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(row.get("female")), normalFont)));
            }
        }

        document.add(table);
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(220, 220, 220));
        return cell;
    }

    private Map<String, Integer> sortByValueDesc(Map<String, Integer> map) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    private Map<String, Double> sortByAmountDesc(Map<String, Double> map) {
        Map<String, Double> sorted = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "—";
        }
        return category;
    }

    private Integer extractAge(String birthDate) {
        if (birthDate == null || birthDate.length() < 4) {
            return null;
        }
        try {
            int birthYear = Integer.parseInt(birthDate.substring(0, 4));
            int age = LocalDate.now().getYear() - birthYear;
            return age < 0 ? null : age;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class AgeBin {
        private int male;
        private int female;

        private AgeBin(int binStart) {
            // binStart is provided by the map key for readability.
        }
    }

    private record AgePyramidData(List<Map<String, Object>> rows, int max) {}

    private record PlayerCsvRow(String licence, String clubOrFederation, String category, String age) {}

    private static class MultiEventAccumulator {
        private final String licence;
        private final Set<String> eventNames = new TreeSet<>();
        private IPlayer player;

        private MultiEventAccumulator(String licence) {
            this.licence = licence;
        }
    }
}
